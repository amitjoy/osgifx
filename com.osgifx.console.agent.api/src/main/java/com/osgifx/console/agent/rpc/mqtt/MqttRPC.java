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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContext;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;
import org.osgi.service.messaging.MessageSubscription;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.rpc.BinaryCodec;
import com.osgifx.console.agent.rpc.RemoteRPC;

import aQute.bnd.exceptions.Exceptions;
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
    private static final BinaryCodec                 codec       = new BinaryCodec();
    private final ThreadLocal<ByteArrayOutputStream> buffer      = ThreadLocal
            .withInitial(() -> new ByteArrayOutputStream(4096));
    private final ThreadLocal<ByteArrayOutputStream> argBuffer   = ThreadLocal
            .withInitial(() -> new ByteArrayOutputStream(1024));
    private final Map<String, Method>                methodCache = new HashMap<>();

    private final L               local;
    private R                     remote;
    private final Class<R>        remoteClass;
    private final ExecutorService executor;

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

        // Initialize Trackers
        this.multiplexerTracker    = new ServiceTracker<>(bundleContext, MqttRequestMultiplexer.class, null);
        this.contextBuilderTracker = new ServiceTracker<>(bundleContext, MessageContextBuilder.class, null);
        this.publisherTracker      = new ServiceTracker<>(bundleContext, MessagePublisher.class, null);
        this.subscriptionTracker   = new ServiceTracker<>(bundleContext, MessageSubscription.class, null);

        // Cache local methods
        for (Method m : this.local.getClass().getMethods()) {
            if (m.getDeclaringClass() != RemoteRPC.class && m.getDeclaringClass() != Object.class) {
                methodCache.put(m.getName() + "#" + m.getParameterTypes().length, m);
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
    public synchronized R getRemote() {
        if (stopped.get())
            return null;

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

                        // uild & Send using Fresh ContextBuilder
                        return withMessageContextBuilder(mcb -> {
                            Message request = mcb.channel(pubTopic).correlationId(UUID.randomUUID().toString())
                                    .content(ByteBuffer.wrap(payload)).buildMessage();

                            // The multiplexer uses this context to know where to listen for the reply
                            MessageContext responseCtx = mcb.channel(pubTopic + "/reply").buildContext();

                            Promise<Message> promise = multiplexer.request(request, responseCtx);
                            try {
                                // Block for response
                                Message responseMsg = promise.getValue();
                                return decodeResponse(responseMsg, method.getGenericReturnType());
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
    }

    private byte[] encodeRequest(Method method, Object[] args) throws IOException {
        final ByteArrayOutputStream bout = buffer.get();
        bout.reset();

        try (GZIPOutputStream gzip = new GZIPOutputStream(bout); DataOutputStream out = new DataOutputStream(gzip)) {

            out.writeUTF(method.getName());
            if (args == null)
                args = new Object[0];
            out.writeShort(args.length);

            for (Object arg : args) {
                // Encode each arg to temp buffer to get length
                final ByteArrayOutputStream argBout = argBuffer.get();
                argBout.reset();
                try (DataOutputStream argOut = new DataOutputStream(argBout)) {
                    try {
                        codec.encode(arg, argOut);
                    } catch (Exception e) {
                        throw new IOException("Failed to encode argument", e);
                    }
                }
                byte[] argBytes = argBout.toByteArray();
                out.writeInt(argBytes.length);
                out.write(argBytes);
            }
            out.flush();
            gzip.finish();
        }
        return bout.toByteArray();
    }

    private Object decodeResponse(Message msg, Type returnType) throws Exception {
        ByteBuffer payload = msg.payload();
        byte[]     data    = new byte[payload.remaining()];
        payload.get(data);

        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))) {
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

            try (DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))) {
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

            Method m = methodCache.get(methodName + "#" + rawArgs.size());
            if (m != null) {
                Object[] args = new Object[rawArgs.size()];
                for (int i = 0; i < args.length; i++) {
                    try (DataInputStream argIn = new DataInputStream(new ByteArrayInputStream(rawArgs.get(i)))) {
                        args[i] = codec.decode(argIn, m.getGenericParameterTypes()[i]);
                    }
                }
                try {
                    result = m.invoke(local, args);
                } catch (Throwable t) {
                    isError  = true;
                    errorMsg = Exceptions.unrollCause(t, InvocationTargetException.class).getMessage();
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
                    ByteArrayOutputStream bout = buffer.get();
                    bout.reset();
                    try (GZIPOutputStream gzip = new GZIPOutputStream(bout);
                            DataOutputStream out = new DataOutputStream(gzip)) {

                        out.writeBoolean(isError);
                        if (isError) {
                            codec.encode(errorMsg, out);
                        } else {
                            codec.encode(result, out);
                        }
                        out.flush();
                        gzip.finish();
                    } catch (Exception e) {
                        throw new RuntimeException("Encoding failed", e);
                    }

                    Message responseMsg = mcb.channel(topic).correlationId(correlationId)
                            .content(ByteBuffer.wrap(bout.toByteArray())).buildMessage();

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
}