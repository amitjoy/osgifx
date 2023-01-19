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
package com.osgifx.console.agent.link;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.osgifx.console.agent.Agent;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.json.JSONCodec;

public class RemoteRPC<L, R> extends Thread implements Closeable {

    private static final JSONCodec codec = new JSONCodec();

    private final DataInputStream                in;
    private final DataOutputStream               out;
    private final Class<R>                       remoteClass;
    private final AtomicInteger                  id       = new AtomicInteger(10_000);
    private final ConcurrentMap<Integer, Result> promises = new ConcurrentHashMap<>();
    private final AtomicBoolean                  quit     = new AtomicBoolean();
    private volatile boolean                     transfer;
    private final ThreadLocal<Integer>           msgId    = new ThreadLocal<>();

    private L                     local;
    private R                     endpoint;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private static class Result {
        boolean resolved;
        byte[]  value;
        boolean exception;
    }

    public RemoteRPC(final Class<R> remoteType, final L local, final InputStream in, final OutputStream out) {
        this(remoteType, local, new DataInputStream(in), new DataOutputStream(out));
    }

    @SuppressWarnings("unchecked")
    public RemoteRPC(final Class<R> remoteType, final L local, final DataInputStream in, final DataOutputStream out) {
        super("fx-agent-link::" + remoteType.getName());
        setDaemon(true);
        this.remoteClass = remoteType;
        this.local       = local == null ? (L) this : local;
        this.in          = new DataInputStream(in);
        this.out         = new DataOutputStream(out);
    }

    public RemoteRPC(final Class<R> type, final L local, final Socket socket) throws IOException {
        this(type, local, socket.getInputStream(), socket.getOutputStream());
    }

    public void open() {
        if (isAlive()) {
            throw new IllegalStateException("Already running");
        }
        if (in != null) {
            start();
        }
    }

    @Override
    public void close() throws IOException {
        if (quit.getAndSet(true)) {
            return; // already closed
        }
        if (local instanceof Closeable) {
            try {
                ((Closeable) local).close();
            } catch (final Exception e) {
                // nothing to do
            }
        }
        if (!transfer) {
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
        }
        executor.shutdownNow();
    }

    @SuppressWarnings("unchecked")
    public synchronized R getRemote() {
        if (quit.get()) {
            return null;
        }
        if (endpoint == null) {
            endpoint = (R) Proxy.newProxyInstance(remoteClass.getClassLoader(), new Class<?>[] { remoteClass },
                    (target, method, args) -> {
                        final Object hash = new Object();
                        try {
                            if (method.getDeclaringClass() == Object.class) {
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
        return endpoint;
    }

    @Override
    public void run() {
        while (!isInterrupted() && !transfer && !quit.get()) {
            try {
                final String cmd = in.readUTF();
                trace("rx " + cmd);
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

    /*
     * Signaling function
     */
    protected void terminate() {
        try {
            close();
        } catch (final IOException e) {
            // nothing to do
        }
    }

    Method getMethod(final String cmd, final int count) {
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

    int send(final int msgId, final Method m, Object[] args) throws Exception {
        if (m != null) {
            promises.put(msgId, new Result());
        }
        trace("send");
        synchronized (out) {
            out.writeUTF(m != null ? m.getName() : "");
            out.writeInt(msgId);
            if (args == null) {
                args = new String[] {};
            }
            out.writeShort(args.length);
            for (final Object arg : args) {
                if (arg instanceof byte[]) {
                    final byte[] data = (byte[]) arg;
                    out.writeInt(data.length);
                    out.write(data);
                } else {
                    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    codec.enc().to(bout).put(arg);
                    final byte[] data = bout.toByteArray();
                    out.writeInt(data.length);
                    out.write(data);
                }
            }
            out.flush();
            trace("sent");
        }
        return msgId;
    }

    void response(int msgId, final byte[] data) {
        boolean exception = false;
        if (msgId < 0) {
            msgId     = -msgId;
            exception = true;
        }
        final Result o = promises.get(msgId);
        if (o != null) {
            synchronized (o) {
                trace("resolved");
                o.value     = data;
                o.exception = exception;
                o.resolved  = true;
                o.notifyAll();
            }
        }
    }

    @SuppressWarnings("unchecked")
    <T> T waitForResult(final int id, final Type type) throws Exception {
        final long   deadline   = 300_000L;
        final long   startNanos = System.nanoTime();
        final Result result     = promises.get(id);
        try {
            do {
                synchronized (result) {
                    if (result.resolved) {
                        if (result.value == null) {
                            return null;
                        }
                        if (result.exception) {
                            final String msg = codec.dec().from(result.value).get(String.class);
                            trace("Exception during agent communication: " + msg);
                            throw new RuntimeException(msg);
                        }
                        if (type == byte[].class) {
                            return (T) result.value;
                        }
                        return (T) codec.dec().from(result.value).get(type);
                    }

                    long elapsed = System.nanoTime() - startNanos;
                    long delay   = deadline - TimeUnit.NANOSECONDS.toMillis(elapsed);
                    if (delay <= 0L) {
                        return null;
                    }
                    trace("start delay " + delay);
                    result.wait(delay);
                    elapsed = System.nanoTime() - startNanos;
                    delay   = deadline - TimeUnit.NANOSECONDS.toMillis(elapsed);
                    trace("end delay " + delay);
                }
            } while (true);
        } finally {
            promises.remove(id);
        }
    }

    private void trace(final String message) {
        final boolean isTracingEnabled = Boolean.getBoolean(Agent.TRACE_LOG_KEY);
        if (isTracingEnabled) {
            System.out.println("[OSGi.fx] " + message);
        }
    }

    void executeCommand(final String cmd, final int id, final List<byte[]> args) throws Exception {
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
                    parameters[i] = codec.dec().from(args.get(i)).get(m.getGenericParameterTypes()[i]);
                }
            }
            try {
                final Object result = m.invoke(local, parameters);
                if (transfer || m.getReturnType() == void.class) {
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

    public boolean isOpen() {
        return !quit.get();
    }

}
