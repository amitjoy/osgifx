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
package com.osgifx.console.agent.rpc.mqtt;

import static com.osgifx.console.agent.Agent.AGENT_RPC_MAX_DECOMPRESSED_SIZE_KEY;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.util.promise.TimeoutException;
import org.osgi.util.tracker.ServiceTracker;

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
import in.bytehue.messaging.mqtt5.api.CancellablePromise;
import in.bytehue.messaging.mqtt5.api.MqttRequestMultiplexer;

/**
 * MQTT Implementation of RemoteRPC.
 * Uses MqttRequestMultiplexer for correlating Request/Response.
 */
public class MqttRPC<L, R> implements Closeable, RemoteRPC<L, R> {

    // Service Trackers
    private final ServiceTracker<MqttRequestMultiplexer, MqttRequestMultiplexer> multiplexerTracker;
    private final ServiceTracker<MessageContextBuilder, MessageContextBuilder>   contextBuilderTracker;
    private final ServiceTracker<MessagePublisher, MessagePublisher>             publisherTracker;
    private final ServiceTracker<MessageSubscription, MessageSubscription>       subscriptionTracker;

    private final String        pubTopic;
    private final String        subTopic;
    private final BundleContext bundleContext;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final FluentLogger  logger  = LoggerFactory.getFluentLogger(getClass());

    // Optimization: Shared Codec & Buffers
    private final BinaryCodec                            codec;
    private final long                                   maxDecompressedSize;
    private final ThreadLocal<FastByteArrayOutputStream> buffer          = ThreadLocal
            .withInitial(() -> new FastByteArrayOutputStream(4096));
    private final ThreadLocal<DataOutputStream>          bufferOut       = ThreadLocal
            .withInitial(() -> new DataOutputStream(buffer.get()));
    private final ThreadLocal<FastByteArrayOutputStream> argBuffer       = ThreadLocal
            .withInitial(() -> new FastByteArrayOutputStream(1024));
    private final ThreadLocal<DataOutputStream>          argBufferOut    = ThreadLocal
            .withInitial(() -> new DataOutputStream(argBuffer.get()));
    private final Map<String, Method>                    methodCache     = new HashMap<>();
    private final Map<MethodKey, Method>                 methodKeyCache  = new HashMap<>();
    private final ThreadLocal<MethodKey>                 methodKeyHolder = ThreadLocal.withInitial(MethodKey::new);
    private final ThreadLocal<Object[]>                  parameterBuffer = ThreadLocal.withInitial(() -> new Object[8]);

    // Method Signature Caching
    private final Map<Method, Type[]>     genericParameterTypeCache = new HashMap<>();
    private final Map<Method, byte[]>     methodNameBytesCache      = new HashMap<>();
    private final Map<Method, Class<?>[]> parameterTypeCache        = new HashMap<>();
    private final Map<Method, Class<?>>   returnTypeCache           = new HashMap<>();

    // Empty array constant for zero-arg methods
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    // Adaptive Compression Threshold
    private final AtomicInteger compressionThreshold  = new AtomicInteger(1024);
    private final AtomicInteger compressionSamples    = new AtomicInteger(0);
    private final AtomicInteger totalCompressionRatio = new AtomicInteger(0);

    private final L               local;
    private R                     remote;
    private final Class<R>        remoteClass;
    private final ExecutorService executor;
    private final ReentrantLock   remoteLock = new ReentrantLock();

    @SuppressWarnings("unchecked")
    public MqttRPC(final BundleContext bundleContext,
                   final Class<R> remoteClass,
                   final L local,
                   final String pubTopic,
                   final String subTopic,
                   final ExecutorService executor) {

        this.bundleContext = bundleContext;
        this.remoteClass   = remoteClass;
        this.local         = local == null ? (L) this : local;
        this.pubTopic      = pubTopic;
        this.subTopic      = subTopic;
        this.executor      = executor;
        this.codec         = new BinaryCodec(bundleContext);

        // Read max decompressed size from configuration
        this.maxDecompressedSize = readMaxDecompressedSize(bundleContext);

        // Initialize Trackers
        this.multiplexerTracker    = new ServiceTracker<>(bundleContext, MqttRequestMultiplexer.class, null);
        this.contextBuilderTracker = new ServiceTracker<>(bundleContext, MessageContextBuilder.class, null);
        this.publisherTracker      = new ServiceTracker<>(bundleContext, MessagePublisher.class, null);
        this.subscriptionTracker   = new ServiceTracker<>(bundleContext, MessageSubscription.class, null);

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
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("MQTT RPC is already running");
        }

        multiplexerTracker.open();
        contextBuilderTracker.open();
        publisherTracker.open();
        subscriptionTracker.open();

        // Subscribe to incoming RPC requests (Server Mode)
        // Note: Using Optional here is safe for startup, but in dynamic OSGi,
        // a better pattern would be addingService() in the tracker.
        // For simplicity/readability, we assume services are present or will retry.
        Optional.ofNullable(subscriptionTracker.getService()).ifPresent(sub -> {
            sub.subscribe(subTopic).forEach(msg -> {
                executor.execute(() -> handleIncomingMessage(msg));
            });
        });
    }

    @Override
    public void close() {
        if (stopped.getAndSet(true)) {
            return;
        }
        if (local instanceof Closeable) {
            try {
                ((Closeable) local).close();
            } catch (final Exception e) {
            }
        }
        multiplexerTracker.close();
        contextBuilderTracker.close();
        publisherTracker.close();
        subscriptionTracker.close();
        executor.shutdownNow();
    }

    /**
     * Safely executes an action with a fresh MessageContextBuilder instance.
     * Required because MessageContextBuilder is PROTOTYPE scoped.
     */
    private <T> T withMessageContextBuilder(Function<MessageContextBuilder, T> action) throws IOException {
        ServiceReference<MessageContextBuilder> ref = contextBuilderTracker.getServiceReference();
        if (ref == null)
            throw new IOException("MessageContextBuilder service unavailable");

        ServiceObjects<MessageContextBuilder> serviceObjects = bundleContext.getServiceObjects(ref);
        if (serviceObjects == null)
            throw new IOException("ServiceObjects for MessageContextBuilder unavailable");

        MessageContextBuilder builder = serviceObjects.getService();
        if (builder == null)
            throw new IOException("MessageContextBuilder instance is null");

        try {
            return action.apply(builder);
        } finally {
            serviceObjects.ungetService(builder);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public R getRemote() {
        if (stopped.get())
            return null;

        remoteLock.lock();
        try {
            if (remote == null) {
                remote = (R) Proxy.newProxyInstance(remoteClass.getClassLoader(), new Class<?>[] { remoteClass },
                        (target, method, args) -> {
                            if (method.getDeclaringClass() == Object.class) {
                                return method.invoke(this, args);
                            }

                            // Encode Request
                            byte[] payload = encodeRequest(method, args);

                            // Check Service Availability
                            MqttRequestMultiplexer multiplexer = multiplexerTracker.getService();
                            if (multiplexer == null) {
                                throw new IOException("MqttRequestMultiplexer is unavailable");
                            }

                            // Build & Send using Fresh ContextBuilder
                            return withMessageContextBuilder(mcb -> {
                                Message request = mcb.channel(pubTopic).correlationId(UUID.randomUUID().toString())
                                        .content(ByteBuffer.wrap(payload)).buildMessage();

                                // The multiplexer uses this context to know where to listen for the reply
                                MessageContext responseCtx = mcb.channel(pubTopic + "/reply").buildContext();

                                CancellablePromise<Message> promise = multiplexer.request(request, responseCtx);
                                try {
                                    // Block for response with timeout (300 seconds, same as SocketRPC)
                                    Message response = promise.timeout(300_000L).onFailure(failure -> {
                                        if (failure instanceof TimeoutException) {
                                            // Explicitly cancel to clean up resources
                                            promise.cancel();
                                        }
                                    }).getValue();
                                    return decodeResponse(response, method.getGenericReturnType());
                                } catch (InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException("RPC Interrupted", e);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        });
            }
            return remote;
        } finally {
            remoteLock.unlock();
        }
    }

    private byte[] encodeRequest(Method method, Object[] args) throws IOException {
        final FastByteArrayOutputStream bout = buffer.get();
        return adaptivelyCompress(bout, out -> {
            out.writeUTF(method.getName());
            if (args != null) {
                out.writeShort(args.length);
                for (Object arg : args) {
                    final FastByteArrayOutputStream argBout = argBuffer.get();
                    argBout.reset();
                    final DataOutputStream argOut = argBufferOut.get();
                    try {
                        codec.encode(arg, argOut);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                    int argLen = argBout.size();
                    out.writeInt(argLen);
                    out.write(argBout.getBuffer(), 0, argLen);

                    if (argLen > 1024 * 1024) {
                        argBuffer.set(new FastByteArrayOutputStream(1024));
                        argBufferOut.set(new DataOutputStream(argBuffer.get()));
                    }
                }
            } else {
                out.writeShort(0);
            }
        });
    }

    private byte[] adaptivelyCompress(FastByteArrayOutputStream bout,
                                      IOConsumer<DataOutputStream> writer) throws IOException {
        // 1. Write to buffer uncompressed first
        bout.reset();
        final DataOutputStream out = bufferOut.get();
        writer.accept(out);

        int rawLength = bout.size();

        // 2. Decide whether to compress using adaptive threshold
        final int threshold = compressionThreshold.get();
        if (rawLength >= threshold) {
            // LZ4 compression - use Lz4Codec directly
            byte[] compressed = Lz4Codec.compress(bout.getBuffer(), 0, rawLength);

            // Lz4Codec returns uncompressed data if compression doesn't help
            // Check if actually compressed by comparing sizes
            if (compressed.length < rawLength) {
                // Build LZ4 payload: header (8 bytes) + compressed data
                byte[] result = new byte[8 + compressed.length];
                // Write compressed length
                result[0] = (byte) (compressed.length >>> 24);
                result[1] = (byte) (compressed.length >>> 16);
                result[2] = (byte) (compressed.length >>> 8);
                result[3] = (byte) compressed.length;
                // Write uncompressed length
                result[4] = (byte) (rawLength >>> 24);
                result[5] = (byte) (rawLength >>> 16);
                result[6] = (byte) (rawLength >>> 8);
                result[7] = (byte) rawLength;
                // Copy compressed data
                System.arraycopy(compressed, 0, result, 8, compressed.length);

                // Update adaptive threshold based on compression ratio
                updateCompressionThreshold(rawLength, compressed.length);
                return result;
            }
        }
        // Return a copy of raw data (since buffer will be reused)
        byte[] result = new byte[rawLength];
        System.arraycopy(bout.getBuffer(), 0, result, 0, rawLength);

        if (rawLength > 1024 * 1024) {
            buffer.set(new FastByteArrayOutputStream(4096));
            bufferOut.set(new DataOutputStream(buffer.get()));
        }
        return result;
    }

    @FunctionalInterface
    private interface IOConsumer<T> {
        void accept(T t) throws IOException;
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

    private Object decodeResponse(Message msg, Type returnType) throws Exception {
        ByteBuffer payload = msg.payload();
        byte[]     data    = new byte[payload.remaining()];
        payload.get(data);

        InputStream source = new FastByteArrayInputStream(data);
        if (isLz4(data)) {
            source = new Lz4InputStream(source, maxDecompressedSize);
        }

        try (DataInputStream in = new DataInputStream(source)) {
            boolean isError = in.readBoolean();
            if (isError) {
                String errorMsg = (String) codec.decode(in, String.class);
                throw new RuntimeException("Remote RPC Error: " + errorMsg);
            }
            if (returnType == void.class)
                return null;
            return codec.decode(in, returnType);
        }
    }

    private void handleIncomingMessage(Message msg) {
        try {
            ByteBuffer payload = msg.payload();
            byte[]     data    = new byte[payload.remaining()];
            payload.get(data);

            // Decode Request
            String       methodName;
            List<byte[]> rawArgs = new ArrayList<>();

            InputStream source = new FastByteArrayInputStream(data);
            if (isLz4(data)) {
                source = new Lz4InputStream(source, maxDecompressedSize);
            }

            try (DataInputStream in = new DataInputStream(source)) {
                methodName = in.readUTF();
                int count = in.readShort();
                for (int i = 0; i < count; i++) {
                    int    len = in.readInt();
                    byte[] arg = new byte[len];
                    in.readFully(arg);
                    rawArgs.add(arg);
                }
            }

            // Execute
            Object  result   = null;
            boolean isError  = false;
            String  errorMsg = null;

            // Use ThreadLocal MethodKey for zero-allocation lookup
            MethodKey key = methodKeyHolder.get();
            key.set(methodName, rawArgs.size());
            Method m = methodKeyCache.get(key);
            if (m == null) {
                // Fallback to old cache
                m = methodCache.get(methodName + "#" + rawArgs.size());
            }
            if (m != null) {
                // Fast-path for zero-argument methods
                if (rawArgs.size() == 0) {
                    try {
                        result = m.invoke(local, EMPTY_OBJECT_ARRAY);
                    } catch (Throwable t) {
                        isError  = true;
                        errorMsg = Exceptions.unrollCause(t, InvocationTargetException.class).getMessage();
                    }
                } else {
                    // Use cached generic parameter types
                    final Type[] genericTypes = genericParameterTypeCache.get(m);

                    // Reuse parameter array for decoding, but create exact-sized array for invoke
                    Object[] tempParams = parameterBuffer.get();
                    if (tempParams.length < rawArgs.size()) {
                        tempParams = new Object[Math.max(8, rawArgs.size())];
                        parameterBuffer.set(tempParams);
                    }

                    for (int i = 0; i < rawArgs.size(); i++) {
                        try (DataInputStream argIn = new DataInputStream(new FastByteArrayInputStream(rawArgs
                                .get(i)))) {
                            tempParams[i] = codec.decode(argIn, genericTypes[i]);
                        }
                    }

                    // Create exact-sized array for method invocation
                    final Object[] args = Arrays.copyOf(tempParams, rawArgs.size());

                    try {
                        // Clear used slots in temp array to avoid memory leaks
                        for (int i = 0; i < rawArgs.size(); i++) {
                            tempParams[i] = null;
                        }
                        result = m.invoke(local, args);
                    } catch (Throwable t) {
                        isError  = true;
                        errorMsg = Exceptions.unrollCause(t, InvocationTargetException.class).getMessage();
                    }
                }
            } else {
                isError  = true;
                errorMsg = "Method not found: " + methodName;
            }

            // Send Response (Dynamic Routing)
            String replyTo  = msg.getContext().getReplyToChannel();
            String correlId = msg.getContext().getCorrelationId();

            if (replyTo != null && correlId != null) {
                sendResponse(replyTo, correlId, result, isError, errorMsg);
            }

        } catch (Exception e) {
            logger.atError().msg("Error processing incoming MQTT RPC").throwable(e).log();
        }
    }

    private void sendResponse(String topic, String correlationId, Object result, boolean isError, String errorMsg) {
        Optional<MessagePublisher> publisherOpt = Optional.ofNullable(publisherTracker.getService());

        if (publisherOpt.isPresent()) {
            try {
                // Use Helper to get FRESH builder
                withMessageContextBuilder(mcb -> {
                    FastByteArrayOutputStream bout = buffer.get();
                    byte[]                    payload;
                    try {
                        payload = adaptivelyCompress(bout, out -> {
                            out.writeBoolean(isError);
                            try {
                                if (isError) {
                                    codec.encode(errorMsg, out);
                                } else {
                                    codec.encode(result, out);
                                }
                            } catch (Exception e) {
                                throw new IOException(e);
                            }
                        });
                    } catch (IOException e) {
                        throw new RuntimeException("Compression failed", e);
                    }

                    Message responseMsg = mcb.channel(topic).correlationId(correlationId)
                            .content(ByteBuffer.wrap(payload)).buildMessage();

                    publisherOpt.get().publish(responseMsg);
                    return null;
                });

            } catch (Exception e) {
                logger.atError().msg("Failed to send RPC response").throwable(e).log();
            }
        }
    }

    @Override
    public boolean isOpen() {
        return !stopped.get();
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