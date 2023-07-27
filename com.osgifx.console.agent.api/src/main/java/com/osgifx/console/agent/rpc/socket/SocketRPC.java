/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.rpc.socket;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.rpc.RemoteRPC;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.json.JSONCodec;

public class SocketRPC<L, R> extends Thread implements Closeable, RemoteRPC<L, R> {

    private final DataInputStream         in;
    private final DataOutputStream        out;
    private final AtomicInteger           id       = new AtomicInteger(10_000);
    private final Map<Integer, RpcResult> promises = new ConcurrentHashMap<>();
    private final AtomicBoolean           stopped  = new AtomicBoolean();
    private final ThreadLocal<Integer>    msgId    = new ThreadLocal<>();
    private final FluentLogger            logger   = LoggerFactory.getFluentLogger(getClass());

    private L              local;
    private R              remote;
    private final Class<R> remoteClass;

    private ExecutorService executor;

    private static class RpcResult {
        boolean resolved;
        byte[]  value;
        boolean exception;
    }

    public SocketRPC(final Class<R> remoteClass,
                     final L local,
                     final Socket socket,
                     final ExecutorService executor) throws IOException {
        this(remoteClass, local, new DataInputStream(socket.getInputStream()),
             new DataOutputStream(socket.getOutputStream()), executor);
    }

    public SocketRPC(final Class<R> remoteClass,
                     final L local,
                     final InputStream in,
                     final OutputStream out,
                     final ExecutorService executor) {
        this(remoteClass, local, new DataInputStream(in), new DataOutputStream(out), executor);
    }

    @SuppressWarnings("unchecked")
    public SocketRPC(final Class<R> remoteClass,
                     final L local,
                     final DataInputStream in,
                     final DataOutputStream out,
                     final ExecutorService executor) {
        super("fx-agent-rpc::" + remoteClass.getName());
        setDaemon(true);
        this.remoteClass = remoteClass;
        this.local       = local == null ? (L) this : local;
        this.in          = new DataInputStream(in);
        this.out         = new DataOutputStream(out);
        this.executor    = executor;
    }

    @Override
    public void open() {
        if (isAlive()) {
            throw new IllegalStateException("Socket RPC is already running");
        }
        if (in != null) {
            start();
        }
    }

    @Override
    public void close() throws IOException {
        if (stopped.getAndSet(true)) {
            return; // already closed
        }
        if (local instanceof Closeable) {
            try {
                ((Closeable) local).close();
            } catch (final Exception e) {
                // nothing to do
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (final Exception e) {
                // nothing to do
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (final Exception e) {
                // nothing to do
            }
        }
        executor.shutdownNow();
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized R getRemote() {
        if (stopped.get()) {
            return null;
        }
        if (remote == null) {
            remote = (R) Proxy.newProxyInstance(remoteClass.getClassLoader(), new Class<?>[] { remoteClass },
                    (target, method, args) -> {
                        try {
                            if (method.getDeclaringClass() == Object.class) {
                                final Object hash = new Object();
                                return method.invoke(hash, args);
                            }
                            int msgId;
                            try {
                                msgId = send(id.getAndIncrement(), method, args);
                                if (method.getReturnType() == void.class) {
                                    promises.remove(msgId);
                                    return null;
                                }
                            } catch (final Exception e1) {
                                terminate();
                                return null;
                            }
                            return waitForResult(msgId, method.getGenericReturnType());
                        } catch (final InvocationTargetException e2) {
                            throw Exceptions.unrollCause(e2, InvocationTargetException.class);
                        } catch (final InterruptedException e3) {
                            interrupt();
                            throw e3;
                        } catch (final Exception e4) {
                            throw e4;
                        }
                    });
        }
        return remote;
    }

    @Override
    public boolean isOpen() {
        return !stopped.get();
    }

    @Override
    public void run() {
        while (!isInterrupted() && !stopped.get()) {
            try {
                final String       cmd   = in.readUTF();
                final int          id    = in.readInt();
                final int          count = in.readShort();
                final List<byte[]> args  = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    final int    length = in.readInt();
                    final byte[] data   = new byte[length];
                    in.readFully(data);
                    args.add(data);
                }
                final Runnable r = () -> {
                    try {
                        msgId.set(id);
                        executeCommand(cmd, id, args);
                    } catch (final Exception e) {
                        // nothing to do
                    }
                    msgId.remove();
                };
                executor.execute(r);
            } catch (final SocketTimeoutException ee) {
                // Ignore, just to allow polling the actors again
            } catch (final Exception ee) {
                terminate();
                return;
            }
        }
    }

    protected void terminate() {
        try {
            close();
        } catch (final IOException e) {
            // nothing to do
        }
    }

    private Method getMethod(final String cmd, final int count) {
        for (final Method m : local.getClass().getMethods()) {
            if (m.getDeclaringClass() == RemoteRPC.class) {
                continue;
            }
            if (m.getName().equals(cmd) && m.getParameterTypes().length == count) {
                return m;
            }
        }
        return null;
    }

    private int send(final int msgId, final Method m, Object[] values) throws Exception {
        if (m != null) {
            promises.put(msgId, new RpcResult());
        }
        trace("Sending Socket RPC");
        synchronized (out) {
            out.writeUTF(m != null ? m.getName() : "");
            out.writeInt(msgId);
            if (values == null) {
                values = new String[] {};
            }
            out.writeShort(values.length);
            for (final Object value : values) {
                if (value instanceof byte[]) {
                    final byte[] data = (byte[]) value;
                    out.writeInt(data.length);
                    out.write(data);
                } else {
                    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    new JSONCodec().enc().deflate().to(bout).put(value);
                    final byte[] data = bout.toByteArray();
                    out.writeInt(data.length);
                    out.write(data);
                }
            }
            out.flush();
            trace("Sent Socket RPC");
        }
        return msgId;
    }

    private void response(int msgId, final byte[] data) {
        boolean exception = false;
        if (msgId < 0) {
            msgId     = -msgId;
            exception = true;
        }
        final RpcResult result = promises.get(msgId);
        if (result != null) {
            synchronized (result) {
                trace("Resolved Socket RPC");
                result.value     = data;
                result.exception = exception;
                result.resolved  = true;
                result.notifyAll();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T waitForResult(final int id, final Type type) throws Exception {
        final long      deadlineInMillis = 300_000L;
        final long      startInNanos     = System.nanoTime();
        final RpcResult result           = promises.get(id);
        try {
            do {
                synchronized (result) {
                    if (result.resolved) {
                        if (result.value == null) {
                            return null;
                        }
                        if (result.exception) {
                            final String msg = new JSONCodec().dec().inflate().from(result.value).get(String.class);
                            trace("Exception during agent communication: " + msg);
                            throw new RuntimeException(msg);
                        }
                        if (type == byte[].class) {
                            return (T) result.value;
                        }
                        return (T) new JSONCodec().dec().inflate().from(result.value).get(type);
                    }
                    long elapsedInNanos = System.nanoTime() - startInNanos;
                    long delayInMillis  = deadlineInMillis - TimeUnit.NANOSECONDS.toMillis(elapsedInNanos);
                    if (delayInMillis <= 0L) {
                        return null;
                    }
                    trace("Start Delay (Socket RPC)" + delayInMillis);
                    result.wait(delayInMillis);
                    elapsedInNanos = System.nanoTime() - startInNanos;
                    delayInMillis  = deadlineInMillis - TimeUnit.NANOSECONDS.toMillis(elapsedInNanos);
                    trace("End Delay (Socket RPC)" + delayInMillis);
                }
            } while (true);
        } finally {
            promises.remove(id);
        }
    }

    private void trace(final String message) {
        final boolean isTracingEnabled = Boolean.getBoolean(Agent.AGENT_RPC_TRACE_LOG_KEY);
        if (isTracingEnabled) {
            logger.atDebug().msg("[OSGi.fx] {}").arg(message).log();
        }
    }

    private void executeCommand(final String cmd, final int id, final List<byte[]> args) throws Exception {
        if (cmd.isEmpty()) {
            response(id, args.get(0));
        } else {
            final Method m = getMethod(cmd, args.size());
            if (m == null) {
                return;
            }
            final Object[] parameters = new Object[args.size()];
            for (int i = 0; i < args.size(); i++) {
                final Class<?> type = m.getParameterTypes()[i];
                if (type == byte[].class) {
                    parameters[i] = args.get(i);
                } else {
                    parameters[i] = new JSONCodec().dec().inflate().from(args.get(i))
                            .get(m.getGenericParameterTypes()[i]);
                }
            }
            try {
                final Object result = m.invoke(local, parameters);
                if (m.getReturnType() == void.class) {
                    return;
                }
                try {
                    send(id, null, new Object[] { result });
                } catch (final Exception e) {
                    terminate();
                }
            } catch (Throwable t) {
                t = Exceptions.unrollCause(t, InvocationTargetException.class);
                try {
                    send(-id, null, new Object[] { t + "" });
                } catch (final Exception e) {
                    terminate();
                }
            }
        }
    }

}
