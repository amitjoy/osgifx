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

import static com.osgifx.console.agent.Agent.AGENT_RPC_MAX_DECOMPRESSED_SIZE_KEY;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.rpc.BinaryCodec;
import com.osgifx.console.agent.rpc.BinaryCodec.FastByteArrayInputStream;
import com.osgifx.console.agent.rpc.BinaryCodec.FastByteArrayOutputStream;
import com.osgifx.console.agent.rpc.Lz4Codec;
import com.osgifx.console.agent.rpc.Lz4InputStream;
import com.osgifx.console.agent.rpc.MethodKey;
import com.osgifx.console.agent.rpc.RemoteRPC;

import aQute.bnd.exceptions.Exceptions;

/**
 * Socket Implementation of RemoteRPC.
 * Optimized for constrained networks using TCP_NODELAY, Buffering, and LZ4 compression.
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
    private final BinaryCodec                            codec;
    private final long                                   maxDecompressedSize;
    private final ThreadLocal<FastByteArrayOutputStream> buffer          = ThreadLocal
            .withInitial(() -> new FastByteArrayOutputStream(4096));
    private final ThreadLocal<DataOutputStream>          encodingOut     = ThreadLocal
            .withInitial(() -> new DataOutputStream(buffer.get()));
    private final Map<String, Method>                    methodCache     = new HashMap<>();
    private final Map<MethodKey, Method>                 methodKeyCache  = new HashMap<>();
    private final ThreadLocal<MethodKey>                 methodKeyHolder = ThreadLocal.withInitial(MethodKey::new);
    private final ThreadLocal<Object[]>                  parameterBuffer = ThreadLocal.withInitial(() -> new Object[8]);

    // Method Signature Caching
    private final Map<Method, Type[]>     genericParameterTypeCache = new HashMap<>();
    private final Map<Method, byte[]>     methodNameBytesCache      = new HashMap<>();
    private final Map<Method, Class<?>>   returnTypeCache           = new HashMap<>();
    private final Map<Method, Class<?>[]> parameterTypeCache        = new HashMap<>();

    // Empty array constant for zero-arg methods
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    // Promise Pool (max 16 results)
    private final Queue<RpcResult> resultPool           = new ConcurrentLinkedQueue<>();
    private static final int       MAX_RESULT_POOL_SIZE = 16;

    // Adaptive Compression Threshold
    private final AtomicInteger compressionThreshold  = new AtomicInteger(1024);
    private final AtomicInteger compressionSamples    = new AtomicInteger(0);
    private final AtomicInteger totalCompressionRatio = new AtomicInteger(0);

    private L                   local;
    private R                   remote;
    private final Class<R>      remoteClass;
    private ExecutorService     executor;
    private final ReentrantLock outLock = new ReentrantLock();

    private static class RpcResult {
        CountDownLatch latch;
        byte[]         value;
        boolean        exception;

        RpcResult() {
            reset();
        }

        void reset() {
            this.latch     = new CountDownLatch(1);
            this.value     = null;
            this.exception = false;
        }
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
        this(null, remoteClass, local, new BufferedInputStream(configureSocket(socket).getInputStream()),
             new BufferedOutputStream(socket.getOutputStream()), executor);
    }

    public SocketRPC(final BundleContext context,
                     final Class<R> remoteClass,
                     final L local,
                     final Socket socket,
                     final ExecutorService executor) throws IOException {
        this(context, remoteClass, local, new BufferedInputStream(configureSocket(socket).getInputStream()),
             new BufferedOutputStream(socket.getOutputStream()), executor);
    }

    public SocketRPC(final Class<R> remoteClass,
                     final L local,
                     final InputStream in,
                     final OutputStream out,
                     final ExecutorService executor) {
        this(null, remoteClass, local, (in instanceof DataInputStream) ? (DataInputStream) in : new DataInputStream(in),
             (out instanceof DataOutputStream) ? (DataOutputStream) out : new DataOutputStream(out), executor);
    }

    public SocketRPC(final BundleContext context,
                     final Class<R> remoteClass,
                     final L local,
                     final InputStream in,
                     final OutputStream out,
                     final ExecutorService executor) {
        this(context, remoteClass, local,
             (in instanceof DataInputStream) ? (DataInputStream) in : new DataInputStream(in),
             (out instanceof DataOutputStream) ? (DataOutputStream) out : new DataOutputStream(out), executor);
    }

    public SocketRPC(final Class<R> remoteClass,
                     final L local,
                     final DataInputStream in,
                     final DataOutputStream out,
                     final ExecutorService executor) {
        this(null, remoteClass, local, in, out, executor);
    }

    @SuppressWarnings("unchecked")
    public SocketRPC(final BundleContext context,
                     final Class<R> remoteClass,
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
        this.codec       = new BinaryCodec(context);

        // Read max decompressed size from configuration
        this.maxDecompressedSize = readMaxDecompressedSize(context);

        // Cache local methods and their signatures
        for (Method m : this.local.getClass().getMethods()) {
            if (m.getDeclaringClass() != RemoteRPC.class && m.getDeclaringClass() != Object.class) {
                // Intern method name to reduce memory footprint
                String internedName = m.getName().intern();
                methodCache.put(internedName + "#" + m.getParameterTypes().length, m);
                methodKeyCache.put(new MethodKey(internedName, m.getParameterTypes().length), m);
                // Cache method signatures
                parameterTypeCache.put(m, m.getParameterTypes());
                genericParameterTypeCache.put(m, m.getGenericParameterTypes());
                returnTypeCache.put(m, m.getReturnType());
                // Cache method name bytes for faster serialization
                try {
                    ByteArrayOutputStream nameOut = new ByteArrayOutputStream();
                    DataOutputStream      dos     = new DataOutputStream(nameOut);
                    dos.writeUTF(internedName);
                    dos.flush();
                    methodNameBytesCache.put(m, nameOut.toByteArray());
                } catch (IOException e) {
                    // Fallback to runtime encoding
                }
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
        if (local instanceof Closeable) {
            try {
                ((Closeable) local).close();
            } catch (Exception e) {
            }
        }
        terminate("RPC channel closed");
    }

    protected void terminate(final String message) {
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

        final FastByteArrayOutputStream bout = new FastByteArrayOutputStream(256);
        try (DataOutputStream dataOut = new DataOutputStream(bout)) {
            codec.encode(message, dataOut);
        } catch (Exception e) {
            // ignore
        }
        final byte[] errorData = bout.toByteArray();
        for (final RpcResult result : promises.values()) {
            result.exception = true;
            result.value     = errorData;
            result.latch.countDown();
        }
        promises.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public R getRemote() {
        if (stopped.get())
            return null;
        outLock.lock();
        try {
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
                                final Throwable cause = Exceptions.unrollCause(e, RuntimeException.class);
                                if (cause instanceof RuntimeException) {
                                    throw (RuntimeException) cause;
                                }
                                throw new RuntimeException(cause);
                            }
                        });
            }
            return remote;
        } finally {
            outLock.unlock();
        }
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
                terminate("RPC channel closed: " + ee.getMessage());
                return;
            }
        }
    }

    protected void terminate() {
        terminate("RPC channel closed");
    }

    private Method getMethod(final String cmd, final int count) {
        // Use ThreadLocal MethodKey for zero-allocation lookup
        MethodKey key = methodKeyHolder.get();
        key.set(cmd, count);
        Method m = methodKeyCache.get(key);
        if (m != null) {
            return m;
        }
        // Fallback to old cache
        return methodCache.get(cmd + "#" + count);
    }

    private boolean isLz4(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return false;
        }
        // LZ4 format: 4 bytes compressed length + 4 bytes uncompressed length + compressed data
        // Read header
        int compLen   = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
        int uncompLen = ((bytes[4] & 0xFF) << 24) | ((bytes[5] & 0xFF) << 16) | ((bytes[6] & 0xFF) << 8)
                | (bytes[7] & 0xFF);

        // Validate: totalLength must equal 8 (header) + compressedLength
        // This prevents false positives where random data looks like a valid header
        return compLen > 0 && compLen < 100_000_000 && uncompLen > 0 && uncompLen < 100_000_000
                && bytes.length == (8 + compLen);
    }

    private int send(final int msgId, final Method m, Object[] values) throws Exception {
        if (stopped.get())
            throw new IOException("RPC channel is closed");
        if (m != null) {
            // Get RpcResult from pool or create new
            RpcResult result = resultPool.poll();
            if (result == null) {
                result = new RpcResult();
            } else {
                result.reset();
            }
            promises.put(msgId, result);
        }
        outLock.lock();
        try {
            // Use cached method name bytes if available
            if (m != null) {
                byte[] nameBytes = methodNameBytesCache.get(m);
                if (nameBytes != null) {
                    out.write(nameBytes);
                } else {
                    out.writeUTF(m.getName());
                }
            } else {
                out.writeUTF("");
            }
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
                    final FastByteArrayOutputStream bout = buffer.get();
                    bout.reset();
                    // Adaptive Compression: Serialize first, then decide
                    // We use a cached DataOutputStream wrapper around the buffer
                    final DataOutputStream dataOut = encodingOut.get();
                    codec.encode(value, dataOut);

                    final int    length    = bout.size();
                    final byte[] rawBuffer = bout.getBuffer();

                    // Use adaptive compression threshold
                    final int threshold = compressionThreshold.get();
                    if (length >= threshold) {
                        // LZ4 compression - use Lz4Codec directly
                        byte[] compressed = Lz4Codec.compress(rawBuffer, 0, length);

                        // Lz4Codec returns uncompressed data if compression doesn't help
                        // Check if actually compressed by comparing sizes
                        if (compressed.length < length) {
                            // Write LZ4 header: compressed length + uncompressed length
                            int totalLength = 8 + compressed.length;
                            out.writeInt(totalLength); // Total length including header
                            out.writeInt(compressed.length); // Compressed length
                            out.writeInt(length); // Uncompressed length
                            out.write(compressed);

                            // Update adaptive threshold based on compression ratio
                            updateCompressionThreshold(length, compressed.length);
                        } else {
                            // Compression didn't help, send raw
                            out.writeInt(length);
                            out.write(rawBuffer, 0, length);
                        }
                    } else {
                        // Send raw - Zero Copy from buffer
                        out.writeInt(length);
                        out.write(rawBuffer, 0, length);
                    }

                    if (length > 1024 * 1024) {
                        buffer.set(new FastByteArrayOutputStream(4096));
                        encodingOut.set(new DataOutputStream(buffer.get()));
                    }
                }
            }
            out.flush();
        } finally {
            outLock.unlock();
        }
        return msgId;
    }

    @SuppressWarnings("unchecked")
    private <T> T waitForResult(final int id, final Type type) throws Exception {
        final long      deadlineInMillis = 300_000L;
        final RpcResult result           = promises.get(id);
        try {
            // Wait for result with timeout using CountDownLatch
            boolean completed = result.latch.await(deadlineInMillis, TimeUnit.MILLISECONDS);
            if (!completed)
                return null;

            if (result.value == null)
                return null;
            if (type == byte[].class && !result.exception)
                return (T) result.value;

            InputStream source = new FastByteArrayInputStream(result.value);
            if (isLz4(result.value)) {
                source = new Lz4InputStream(source, maxDecompressedSize);
            }

            try (DataInputStream dataIn = new DataInputStream(source)) {
                if (result.exception) {
                    final String msg = (String) codec.decode(dataIn, String.class);
                    throw new RuntimeException(msg);
                }
                return (T) codec.decode(dataIn, type);
            }
        } finally {
            promises.remove(id);
            // Return RpcResult to pool
            if (result != null && resultPool.size() < MAX_RESULT_POOL_SIZE) {
                result.reset();
                resultPool.offer(result);
            }
        }
    }

    private void executeCommand(final String cmd, final int id, final List<byte[]> args) throws Exception {
        if (cmd.isEmpty()) {
            response(id, args.get(0));
        } else {
            final Method m = getMethod(cmd, args.size());
            if (m == null)
                return;

            // Fast-path for zero-argument methods
            if (args.size() == 0) {
                try {
                    final Object result = m.invoke(local, EMPTY_OBJECT_ARRAY);
                    if (returnTypeCache.get(m) == void.class)
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
                return;
            }

            // Use cached parameter types
            final Class<?>[] paramTypes   = parameterTypeCache.get(m);
            final Type[]     genericTypes = genericParameterTypeCache.get(m);

            // Reuse parameter array for decoding, but create exact-sized array for invoke
            Object[] tempParams = parameterBuffer.get();
            if (tempParams.length < args.size()) {
                tempParams = new Object[Math.max(8, args.size())];
                parameterBuffer.set(tempParams);
            }

            for (int i = 0; i < args.size(); i++) {
                final Class<?> type = paramTypes[i];
                if (type == byte[].class) {
                    tempParams[i] = args.get(i);
                } else {
                    final byte[] argData = args.get(i);
                    InputStream  source  = new FastByteArrayInputStream(argData);
                    if (isLz4(argData)) {
                        source = new Lz4InputStream(source, maxDecompressedSize);
                    }

                    try (DataInputStream dataIn = new DataInputStream(source)) {
                        tempParams[i] = codec.decode(dataIn, genericTypes[i]);
                    }
                }
            }

            // Create exact-sized array for method invocation
            final Object[] parameters = Arrays.copyOf(tempParams, args.size());

            try {
                // Clear used slots in temp array to avoid memory leaks
                for (int i = 0; i < args.size(); i++) {
                    tempParams[i] = null;
                }
                final Object result = m.invoke(local, parameters);
                if (returnTypeCache.get(m) == void.class)
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
            result.value     = data;
            result.exception = exception;
            // Signal completion instead of notifyAll()
            result.latch.countDown();
        }
    }

    // Update adaptive compression threshold based on compression ratio
    private void updateCompressionThreshold(int uncompressedSize, int compressedSize) {
        // Calculate compression ratio as percentage (e.g., 50 = 50% compression)
        int ratio = 100 - (compressedSize * 100 / uncompressedSize);

        // Update running average
        int samples = compressionSamples.incrementAndGet();
        totalCompressionRatio.addAndGet(ratio);

        // Every 100 samples, adjust threshold
        if (samples >= 100) {
            int avgRatio         = totalCompressionRatio.get() / samples;
            int currentThreshold = compressionThreshold.get();

            // If compression is very effective (>30% reduction), lower threshold
            if (avgRatio > 30 && currentThreshold > 512) {
                compressionThreshold.set(currentThreshold - 256);
            }
            // If compression is poor (<15% reduction), raise threshold
            else if (avgRatio < 15 && currentThreshold < 2048) {
                compressionThreshold.set(currentThreshold + 256);
            }

            // Reset counters
            compressionSamples.set(0);
            totalCompressionRatio.set(0);
        }
    }

    private static long readMaxDecompressedSize(final BundleContext context) {
        final long defaultMaxSize = 250L * 1024 * 1024; // 250 MB
        if (context != null) {
            final String sizeStr = context.getProperty(AGENT_RPC_MAX_DECOMPRESSED_SIZE_KEY);
            if (sizeStr != null) {
                try {
                    return Long.parseLong(sizeStr);
                } catch (final NumberFormatException e) {
                    return defaultMaxSize;
                }
            }
        }
        return defaultMaxSize;
    }
}