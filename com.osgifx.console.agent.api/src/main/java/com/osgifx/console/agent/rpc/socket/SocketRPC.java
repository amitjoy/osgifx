/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.rpc.socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.rpc.BinaryCodec;
import com.osgifx.console.agent.rpc.RemoteRPC;

import aQute.bnd.exceptions.Exceptions;

/**
 * Socket Implementation of RemoteRPC.
 * Optimized for constrained networks using TCP_NODELAY, Buffering, and GZIP compression.
 */
public class SocketRPC<L, R> extends Thread implements Closeable, RemoteRPC<L, R> {

    private final DataInputStream         in;
    private final DataOutputStream        out;
    private final AtomicInteger           id       = new AtomicInteger(10_000);
    private final Map<Integer, RpcResult> promises = new ConcurrentHashMap<>();
    private final AtomicBoolean           stopped  = new AtomicBoolean();
    private final ThreadLocal<Integer>    msgId    = new ThreadLocal<>();
    private final FluentLogger            logger   = LoggerFactory.getFluentLogger(getClass());

    // Optimization: Shared Codec & Reuse Buffers
    private static final BinaryCodec                 codec       = new BinaryCodec();
    private final ThreadLocal<ByteArrayOutputStream> buffer      = ThreadLocal
            .withInitial(() -> new ByteArrayOutputStream(4096));
    private final Map<String, Method>                methodCache = new HashMap<>();

    private L               local;
    private R               remote;
    private final Class<R>  remoteClass;
    private ExecutorService executor;

    private static class RpcResult {
        boolean resolved;
        byte[]  value;
        boolean exception;
    }

    private static Socket configureSocket(Socket socket) throws IOException {
        // Critical for RPC performance to avoid 200ms latency
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        return socket;
    }

    public SocketRPC(final Class<R> remoteClass,
                     final L local,
                     final Socket socket,
                     final ExecutorService executor) throws IOException {
        this(remoteClass, local, new BufferedInputStream(configureSocket(socket).getInputStream()),
             new BufferedOutputStream(socket.getOutputStream()), executor);
    }

    public SocketRPC(final Class<R> remoteClass,
                     final L local,
                     final InputStream in,
                     final OutputStream out,
                     final ExecutorService executor) {
        this(remoteClass, local, (in instanceof DataInputStream) ? (DataInputStream) in : new DataInputStream(in),
             (out instanceof DataOutputStream) ? (DataOutputStream) out : new DataOutputStream(out), executor);
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
        this.in          = in;
        this.out         = out;
        this.executor    = executor;

        // Cache local methods
        for (Method m : this.local.getClass().getMethods()) {
            if (m.getDeclaringClass() != RemoteRPC.class && m.getDeclaringClass() != Object.class) {
                methodCache.put(m.getName() + "#" + m.getParameterTypes().length, m);
            }
        }
    }

    @Override
    public void open() {
        if (isAlive())
            throw new IllegalStateException("Socket RPC is already running");
        if (in != null)
            start();
    }

    @Override
    public void close() throws IOException {
        if (stopped.getAndSet(true))
            return;
        if (local instanceof Closeable)
            try {
                ((Closeable) local).close();
            } catch (Exception e) {
            }
        try {
            if (in != null)
                in.close();
        } catch (Exception e) {
        }
        try {
            if (out != null)
                out.close();
        } catch (Exception e) {
        }
        executor.shutdownNow();
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized R getRemote() {
        if (stopped.get())
            return null;
        if (remote == null) {
            remote = (R) Proxy.newProxyInstance(remoteClass.getClassLoader(), new Class<?>[] { remoteClass },
                    (target, method, args) -> {
                        if (method.getDeclaringClass() == Object.class)
                            return method.invoke(new Object(), args);
                        int msgId = -1;
                        try {
                            msgId = send(id.getAndIncrement(), method, args);
                            if (method.getReturnType() == void.class) {
                                promises.remove(msgId);
                                return null;
                            }
                            return waitForResult(msgId, method.getGenericReturnType());
                        } catch (final Exception e) {
                            if (msgId != -1)
                                promises.remove(msgId);
                            throw Exceptions.unrollCause(e, RuntimeException.class);
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
                    } catch (Exception e) {
                        logger.atError().msg("Error executing command").throwable(e).log();
                    } finally {
                        msgId.remove();
                    }
                };
                executor.execute(r);
            } catch (SocketTimeoutException ee) {
                // Ignore
            } catch (Exception ee) {
                terminate();
                return;
            }
        }
    }

    protected void terminate() {
        try {
            close();
        } catch (IOException e) {
        }
    }

    private Method getMethod(final String cmd, final int count) {
        return methodCache.get(cmd + "#" + count);
    }

    private int send(final int msgId, final Method m, Object[] values) throws Exception {
        if (m != null)
            promises.put(msgId, new RpcResult());
        synchronized (out) {
            out.writeUTF(m != null ? m.getName() : "");
            out.writeInt(msgId);
            if (values == null)
                values = new String[] {};
            out.writeShort(values.length);
            for (final Object value : values) {
                if (value instanceof byte[]) {
                    final byte[] data = (byte[]) value;
                    out.writeInt(data.length);
                    out.write(data);
                } else {
                    final ByteArrayOutputStream bout = buffer.get();
                    bout.reset();
                    try (GZIPOutputStream gzip = new GZIPOutputStream(bout);
                            DataOutputStream dataOut = new DataOutputStream(gzip)) {
                        codec.encode(value, dataOut);
                        dataOut.flush();
                        gzip.finish();
                    }
                    final byte[] data = bout.toByteArray();
                    out.writeInt(data.length);
                    out.write(data);
                    if (data.length > 1024 * 1024)
                        buffer.set(new ByteArrayOutputStream(4096));
                }
            }
            out.flush();
        }
        return msgId;
    }

    @SuppressWarnings("unchecked")
    private <T> T waitForResult(final int id, final Type type) throws Exception {
        final long      deadlineInMillis = 300_000L;
        final long      startInNanos     = System.nanoTime();
        final RpcResult result           = promises.get(id);
        try {
            synchronized (result) {
                while (!result.resolved) {
                    long elapsedInNanos = System.nanoTime() - startInNanos;
                    long delayInMillis  = deadlineInMillis - TimeUnit.NANOSECONDS.toMillis(elapsedInNanos);
                    if (delayInMillis <= 0L)
                        return null;
                    result.wait(delayInMillis);
                }
            }
            if (result.value == null)
                return null;
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(result.value));
                    DataInputStream dataIn = new DataInputStream(gzip)) {
                if (result.exception) {
                    final String msg = (String) codec.decode(dataIn, String.class);
                    throw new RuntimeException(msg);
                }
                if (type == byte[].class)
                    return (T) result.value;
                return (T) codec.decode(dataIn, type);
            }
        } finally {
            promises.remove(id);
        }
    }

    private void executeCommand(final String cmd, final int id, final List<byte[]> args) throws Exception {
        if (cmd.isEmpty()) {
            response(id, args.get(0));
        } else {
            final Method m = getMethod(cmd, args.size());
            if (m == null)
                return;
            final Object[] parameters = new Object[args.size()];
            for (int i = 0; i < args.size(); i++) {
                final Class<?> type = m.getParameterTypes()[i];
                if (type == byte[].class) {
                    parameters[i] = args.get(i);
                } else {
                    try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(args.get(i)));
                            DataInputStream dataIn = new DataInputStream(gzip)) {
                        parameters[i] = codec.decode(dataIn, m.getGenericParameterTypes()[i]);
                    }
                }
            }
            try {
                final Object result = m.invoke(local, parameters);
                if (m.getReturnType() == void.class)
                    return;
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

    private void response(int msgId, final byte[] data) {
        boolean exception = false;
        if (msgId < 0) {
            msgId     = -msgId;
            exception = true;
        }
        final RpcResult result = promises.get(msgId);
        if (result != null) {
            synchronized (result) {
                result.value     = data;
                result.exception = exception;
                result.resolved  = true;
                result.notifyAll();
            }
        }
    }
}