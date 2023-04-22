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
package com.osgifx.console.agent.rpc.mqtt;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.service.messaging.Message;
import org.osgi.service.messaging.MessageContextBuilder;
import org.osgi.service.messaging.MessagePublisher;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.rpc.RemoteRPC;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.json.JSONCodec;

public class MqttRPC<L, R> implements Closeable, RemoteRPC<L, R> {

    private static final JSONCodec codec = new JSONCodec();

    private MqttClient                              mqttClient;
    private final String                            pubTopic;
    private final String                            subTopic;
    private final BundleContext                     bundleContext;
    private final AtomicInteger                     id       = new AtomicInteger(10_000);
    private final ConcurrentMap<Integer, RpcResult> promises = new ConcurrentHashMap<>();
    private final AtomicBoolean                     started  = new AtomicBoolean();
    private final AtomicBoolean                     stopped  = new AtomicBoolean();
    private final ThreadLocal<Integer>              msgId    = new ThreadLocal<>();

    private final L        local;
    private R              remote;
    private final Class<R> remoteClass;

    private final ExecutorService executor;

    public static class RpcMessage {
        public int      id;
        public String   methodName;
        public String[] methodArgs;

        @Override
        public String toString() {
            return "[id=" + id + ", methodName=" + methodName + "]";
        }
    }

    public static class RpcResult {
        public boolean resolved;
        public byte[]  value;
        public boolean exception;
    }

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
    }

    @Override
    public void open() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("MQTT RPC is already running");
        }
        mqttClient = new MqttClient(bundleContext, subscriber -> {
            subscriber.subscribe(subTopic).forEach(msg -> {
                try {
                    final ByteBuffer   payload    = msg.payload();
                    final RpcMessage   message    = decodeMessage(payload);
                    final List<byte[]> methodArgs = new ArrayList<>();
                    if (message.methodArgs != null) {
                        for (final String arg : message.methodArgs) {
                            methodArgs.add(Base64.getDecoder().decode(arg));
                        }
                    }
                    final Runnable r = () -> {
                        try {
                            msgId.set(message.id);
                            executeCommand(message.methodName, message.id, methodArgs);
                        } catch (final Exception e) {
                            // nothing to do
                        }
                        msgId.remove();
                    };
                    executor.execute(r);
                } catch (final Exception e) {
                    return;
                }
            });
        });
        mqttClient.open();
    }

    private RpcMessage decodeMessage(final ByteBuffer payload) throws Exception {
        return codec.dec().from(payload.array()).get(RpcMessage.class);
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
        mqttClient.close();
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
                                msgId = send(msg(id.getAndIncrement(), method, args));
                                if (method.getReturnType() == void.class) {
                                    promises.remove(msgId);
                                    return null;
                                }
                            } catch (final Exception e1) {
                                terminate();
                                return null;
                            }
                            return waitForResult(msgId, method.getGenericReturnType());
                        } catch (final InvocationTargetException ite) {
                            throw Exceptions.unrollCause(ite, InvocationTargetException.class);
                        } catch (final Exception e) {
                            throw e;
                        }
                    });
        }
        return remote;
    }

    @Override
    public boolean isOpen() {
        return !stopped.get();
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

    private int send(final RpcMessage msg) throws Exception {
        if (msg.methodName != null) {
            promises.put(msg.id, new RpcResult());
        }
        trace("Sending MQTT RPC: " + msg);
        final Optional<MessagePublisher> publisher = mqttClient.pub();
        if (publisher.isPresent()) {
            final Optional<MessageContextBuilder> msgCtx = mqttClient.msgCtx();
            if (!msgCtx.isPresent()) {
                throw new IllegalStateException("Required service 'MessageContextBuilder' is unavailable");
            }
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try {
                codec.enc().to(bout).put(msg);
                final byte[]  data    = bout.toByteArray();
                final Message message = msgCtx.get().channel(pubTopic).content(ByteBuffer.wrap(data)).buildMessage();
                publisher.get().publish(message);
                trace("Sent MQTT RPC: " + msg);
            } catch (final Exception e) {
                throw new RuntimeException("Message cannot be encoded");
            }
        }
        return msg.id;
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
                trace("Resolved RPC");
                result.value     = data;
                result.exception = exception;
                result.resolved  = true;
                result.notifyAll();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T waitForResult(final int id, final Type type) throws Exception {
        final long      deadlineInMillis = 10_000L;
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
                            final String msg = codec.dec().from(result.value).get(String.class);
                            trace("Exception during agent communication: " + msg);
                            throw new RuntimeException(msg);
                        }
                        if (type == byte[].class) {
                            return (T) result.value;
                        }
                        return (T) codec.dec().from(result.value).get(type);
                    }
                    long elapsedInNanos = System.nanoTime() - startInNanos;
                    long delayInMillis  = deadlineInMillis - TimeUnit.NANOSECONDS.toMillis(elapsedInNanos);
                    if (delayInMillis <= 0L) {
                        return null;
                    }
                    trace("Start Delay (MQTT RPC)" + delayInMillis);
                    result.wait(delayInMillis);
                    elapsedInNanos = System.nanoTime() - startInNanos;
                    delayInMillis  = deadlineInMillis - TimeUnit.NANOSECONDS.toMillis(elapsedInNanos);
                    if (delayInMillis <= 0L) {
                        return null;
                    }
                    trace("End Delay (MQTT RPC)" + delayInMillis);
                }
            } while (true);
        } finally {
            promises.remove(id);
        }
    }

    private void trace(final String message) {
        final boolean isTracingEnabled = Boolean.getBoolean(Agent.AGENT_TRACE_LOG_KEY);
        if (isTracingEnabled) {
            System.out.println("[OSGi.fx] " + message);
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
                    parameters[i] = codec.dec().from(args.get(i)).get(m.getGenericParameterTypes()[i]);
                }
            }
            try {
                final Object result = m.invoke(local, parameters);
                if (m.getReturnType() == void.class) {
                    return;
                }
                try {
                    send(msg(id, null, new Object[] { result }));
                } catch (final Exception e) {
                    terminate();
                }
            } catch (Throwable t) {
                t = Exceptions.unrollCause(t, InvocationTargetException.class);
                try {
                    send(msg(-id, null, new Object[] { t + "" }));
                } catch (final Exception e) {
                    terminate();
                }
            }
        }
    }

    private RpcMessage msg(final int msgId, final Method method, final Object[] args) throws Exception {
        final RpcMessage msg = new RpcMessage();
        msg.methodName = method == null ? "" : method.getName();
        msg.id         = msgId;

        final List<String> methodArgs = new ArrayList<>();
        if (args != null) {
            for (final Object arg : args) {
                byte[] argValue;
                if (arg instanceof byte[]) {
                    argValue = (byte[]) arg;
                } else {
                    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    codec.enc().to(bout).put(arg);
                    argValue = bout.toByteArray();
                }
                final String encodedValue = Base64.getEncoder().encodeToString(argValue);
                methodArgs.add(encodedValue);
            }
        }
        msg.methodArgs = methodArgs.toArray(new String[0]);
        return msg;
    }

}
