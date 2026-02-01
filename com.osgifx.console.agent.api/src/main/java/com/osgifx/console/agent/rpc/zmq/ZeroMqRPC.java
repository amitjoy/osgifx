package com.osgifx.console.agent.rpc.zmq;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import com.osgifx.console.agent.rpc.RemoteRPC;
import com.osgifx.console.agent.rpc.RpcResponse;

import aQute.lib.json.JSONCodec;

public class ZeroMqRPC<L, R> implements RemoteRPC<L, R>, Closeable {

    public static class RpcRequest {
        public String   method;
        public Object[] args;

        public RpcRequest() {
        }

        public RpcRequest(String m, Object[] a) {
            this.method = m;
            this.args   = a;
        }
    }

    private static final int COMMAND_PORT = 5555;
    private static final int EVENT_PORT   = 5556;
    private static final int MAX_RETRIES  = 3;

    private final L               localImpl;
    private final Class<R>        remoteClass;
    private final ZContext        context;
    private final ExecutorService executor;

    private final String  host;
    private final int     timeout;
    private final boolean isServer;

    private ZMQ.Socket   commandSocket;
    private ZMQ.Socket   eventSocket;
    private final Object commandSocketLock = new Object();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private R                   remoteProxy;

    public ZeroMqRPC(final Class<R> remoteClass, final L localImpl) {
        this(remoteClass, localImpl, null, -1);
    }

    public ZeroMqRPC(final Class<R> remoteClass, final L localImpl, final String host, final int timeout) {
        this.remoteClass = remoteClass;
        this.localImpl   = localImpl;
        this.host        = host;
        this.timeout     = timeout;
        this.isServer    = (host == null);
        this.context     = new ZContext();
        this.executor    = Executors.newCachedThreadPool();
    }

    @Override
    public void open() {
        if (running.getAndSet(true)) {
            return;
        }
        try {
            if (isServer) {
                setupServer();
            } else {
                setupClient();
            }
        } catch (final Exception e) {
            // Log fatal startup errors to stderr
            System.err.println("[ZeroMQ] Failed to start RPC: " + e.getMessage());
            close();
        }
    }

    private void setupServer() {
        commandSocket = context.createSocket(SocketType.REP);
        commandSocket.bind("tcp://*:" + COMMAND_PORT);

        eventSocket = context.createSocket(SocketType.PUB);
        eventSocket.bind("tcp://*:" + EVENT_PORT);

        executor.submit(this::serverCommandLoop);
    }

    private void setupClient() {
        reconnectCommandSocket();

        eventSocket = context.createSocket(SocketType.SUB);
        eventSocket.connect("tcp://" + host + ":" + EVENT_PORT);
        // Subscribe to all topics explicitly (Empty filter = All)
        eventSocket.subscribe(new byte[0]);

        executor.submit(this::clientEventLoop);
    }

    private void reconnectCommandSocket() {
        synchronized (commandSocketLock) {
            if (commandSocket != null) {
                try {
                    commandSocket.setLinger(0);
                    commandSocket.close();
                } catch (Exception e) {
                    /* ignore */ }
            }
            commandSocket = context.createSocket(SocketType.REQ);
            if (timeout > 0) {
                commandSocket.setReceiveTimeOut(timeout);
                commandSocket.setSendTimeOut(timeout);
            }
            commandSocket.setLinger(0);
            commandSocket.connect("tcp://" + host + ":" + COMMAND_PORT);
        }
    }

    private void serverCommandLoop() {
        Thread.currentThread().setName("zmq-server-loop");
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                final byte[] requestData = commandSocket.recv(0);
                if (requestData == null)
                    break;

                // --- CRITICAL FIX ---
                // Clear any leaked interrupt status before processing
                if (Thread.interrupted()) {
                    // Just clearing flag
                }

                final RpcResponse response = processRequest(requestData);
                commandSocket.send(serialize(response), 0);
            } catch (final ZMQException e) {
                if (e.getErrorCode() == ZMQ.Error.ETERM.getCode())
                    break;
            } catch (final Exception e) {
                try {
                    commandSocket.send(serialize(RpcResponse.error(e)), 0);
                } catch (final Exception ex) {
                    /* ignore */ }
            }
        }
    }

    private void clientEventLoop() {
        Thread.currentThread().setName("zmq-client-loop");
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                final byte[] data = eventSocket.recv(0);
                if (data == null)
                    break;
                processRequest(data);
            } catch (final Exception e) {
                // Silent catch
            }
        }
    }

    private RpcResponse processRequest(final byte[] data) {
        try {
            final RpcRequest req  = deserialize(data, RpcRequest.class);
            final Method     m    = findMethod(localImpl.getClass(), req.method,
                    req.args != null ? req.args.length : 0);
            final Object[]   args = decodeArgs(m, req.args);

            final Object result = m.invoke(localImpl, args);
            return RpcResponse.success(result);
        } catch (final InvocationTargetException e) {
            // FIX: Unwrap the real exception (e.g. CommandNotFound, InterruptedException)
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                return RpcResponse.error(new RuntimeException("Command execution interrupted"));
            }
            return RpcResponse.error(cause);
        } catch (final Throwable t) {
            return RpcResponse.error(t);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized R getRemote() {
        if (remoteProxy == null) {
            remoteProxy = (R) Proxy.newProxyInstance(remoteClass.getClassLoader(), new Class[] { remoteClass },
                    isServer ? new ServerInvocationHandler() : new ClientInvocationHandler());
        }
        return remoteProxy;
    }

    private class ServerInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (!running.get())
                return null;
            try {
                final byte[] payload = serialize(new RpcRequest(method.getName(), args));
                synchronized (eventSocket) {
                    eventSocket.send(payload, 0);
                }
            } catch (final Exception e) {
                /* ignore */ }
            return null;
        }
    }

    private class ClientInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (!running.get())
                throw new IllegalStateException("RPC Client is closed");
            if (method.getReturnType() == void.class)
                return null;

            final RpcRequest request = new RpcRequest(method.getName(), args);
            byte[]           payload;
            try {
                payload = serialize(request);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize request", e);
            }

            Exception lastError = null;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                synchronized (commandSocketLock) {
                    try {
                        if (!commandSocket.send(payload, 0)) {
                            throw new ZMQException("Send failed", ZMQ.Error.EHOSTUNREACH.getCode());
                        }

                        byte[] reply = commandSocket.recv(0);
                        if (reply == null) {
                            throw new RuntimeException("Timeout waiting for data");
                        }

                        RpcResponse resp = deserialize(reply, RpcResponse.class);
                        if (resp.error != null) {
                            // Do not retry Application Errors
                            throw new RuntimeException(resp.error + "\nRemote Stack:\n" + resp.stackTrace);
                        }
                        if (resp.data == null)
                            return null;

                        return reInflate(resp.data, method.getGenericReturnType(),
                                method.getDeclaringClass().getClassLoader());

                    } catch (RuntimeException e) {
                        throw e; // Propagate application errors immediately
                    } catch (Exception e) {
                        lastError = e;
                        reconnectCommandSocket();
                    }
                }
            }
            throw new RuntimeException("RPC failed after " + MAX_RETRIES + " attempts", lastError);
        }
    }

    // --- Helpers ---

    private Object reInflate(Object data, Type returnType, ClassLoader contextLoader) throws Exception {
        // Optimization: For Gogo commands (Strings), skip heavy JSON cycle
        if (returnType instanceof Class && ((Class<?>) returnType).isAssignableFrom(data.getClass())) {
            return data;
        }

        final ByteArrayOutputStream bout         = new ByteArrayOutputStream();
        ClassLoader                 targetLoader = (contextLoader != null) ? contextLoader
                : Thread.currentThread().getContextClassLoader();
        ClassLoader                 old          = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(targetLoader);
            new JSONCodec().enc().to(bout).put(data);
            String json = new String(bout.toByteArray(), StandardCharsets.UTF_8);
            return new JSONCodec().dec().from(json).get(returnType);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private Object[] decodeArgs(final Method m, final Object[] args) throws Exception {
        if (args == null || args.length == 0)
            return new Object[0];

        final Object[]   typedArgs    = new Object[args.length];
        final Type[]     targetTypes  = m.getGenericParameterTypes();
        final Class<?>[] paramClasses = m.getParameterTypes();

        final ClassLoader methodLoader = m.getDeclaringClass().getClassLoader();

        for (int i = 0; i < args.length; i++) {
            final Object currentArg = args[i];
            if (currentArg == null) {
                typedArgs[i] = null;
                continue;
            }
            boolean isGeneric = (targetTypes[i] instanceof ParameterizedType);
            if (!isGeneric && paramClasses[i].isAssignableFrom(currentArg.getClass())) {
                typedArgs[i] = currentArg;
            } else {
                typedArgs[i] = reInflate(currentArg, targetTypes[i], methodLoader);
            }
        }
        return typedArgs;
    }

    private byte[] serialize(final Object obj) throws Exception {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new JSONCodec().enc().deflate().to(bout).put(obj);
        return bout.toByteArray();
    }

    private <T> T deserialize(final byte[] data, final Class<T> type) throws Exception {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(type.getClassLoader());
            return new JSONCodec().dec().inflate().from(data).get(type);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public void close() {
        if (running.getAndSet(false)) {
            if (context != null)
                context.close();
            if (executor != null)
                executor.shutdownNow();
        }
    }

    @Override
    public boolean isOpen() {
        return running.get();
    }

    private Method findMethod(final Class<?> type, final String name, final int argCount) {
        for (final Method m : type.getMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == argCount) {
                return m;
            }
        }
        throw new RuntimeException("Method not found: " + name);
    }
}