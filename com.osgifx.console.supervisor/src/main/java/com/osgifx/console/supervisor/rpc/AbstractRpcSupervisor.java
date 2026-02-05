/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.supervisor.rpc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectRestrictions.DEFAULT_MAXIMUM_PACKET_SIZE;
import static com.hivemq.client.mqtt.mqtt5.message.connect.Mqtt5ConnectRestrictions.DEFAULT_SEND_MAXIMUM_PACKET_SIZE;
import static com.osgifx.console.supervisor.rpc.AbstractRpcSupervisor.MqttConfig.MAX_CONCURRENT_MSG_TO_RECEIVE;
import static com.osgifx.console.supervisor.rpc.AbstractRpcSupervisor.MqttConfig.MAX_CONCURRENT_MSG_TO_SEND;
import static com.osgifx.console.supervisor.rpc.RpcSupervisor.MQTT_CONNECTION_LISTENER_FILTER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.service.condition.Condition.CONDITION_ID;
import static org.osgi.service.condition.Condition.INSTANCE;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.net.ssl.SSLSocketFactory;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.condition.Condition;

import com.google.common.base.Strings;
import com.google.mu.util.concurrent.Retryer;
import com.google.mu.util.concurrent.Retryer.Delay;
import com.osgifx.console.agent.rpc.RemoteRPC;
import com.osgifx.console.agent.rpc.mqtt.MqttRPC;
import com.osgifx.console.agent.rpc.socket.SocketRPC;
import com.osgifx.console.supervisor.MqttConnection;
import com.osgifx.console.supervisor.SocketConnection;
import com.osgifx.console.supervisor.rpc.TokenProvider.TokenConfigDTO;
import com.osgifx.console.util.configuration.ConfigHelper;

import in.bytehue.messaging.mqtt5.api.MqttMessageConstants;

public abstract class AbstractRpcSupervisor<S, A> {

    @interface MqttConfig {
        final int MAX_CONCURRENT_MSG_TO_SEND    = 5;
        final int MAX_CONCURRENT_MSG_TO_RECEIVE = 5;

        String id();

        String server();

        int port();

        boolean cleanStart();

        boolean automaticReconnect();

        boolean simpleAuth();

        String username();

        String password();

        int sendMaximum();

        int receiveMaximum();

        int keepAliveInterval();

        int maximumPacketSize();

        int sendMaximumPacketSize();

        long sessionExpiryInterval();

        String[] connectedListenerFilters();

        String[] disconnectedListenerFilters();

        String osgi_ds_satisfying_condition_target();
    }

    private static final int    SOCKET_RPC_BACKOFF_LIMIT      = 4;
    private static final double SOCKET_RPC_BACKOFF_MULTIPLIER = 1.5d;

    private static final int RPC_POOL_CORE_THREADS_SIZE          = 10;
    private static final int RPC_POOL_MAX_THREADS_SIZE           = 30;
    private static final int RPC_POOL_KEEP_ALIVE_TIME_IN_SECONDS = 60;

    private A                 agent;
    protected int             port;
    protected int             timeout;
    protected String          host;
    protected RemoteRPC<S, A> remoteRPC;
    protected volatile int    exitCode;

    protected void connectToSocket(final Class<A> agent,
                                   final S supervisor,
                                   final SocketConnection socketConnection) throws Exception {

        checkArgument(timeout > -1, "timeout cannot be less than -1");
        checkNotNull(supervisor, "'supervisor' cannot be null");
        checkNotNull(socketConnection.host(), "'host' cannot be null");

        host    = socketConnection.host();
        port    = socketConnection.port();
        timeout = socketConnection.timeout();

        new Retryer().upon(ConnectException.class,
                Delay.ofMillis(timeout).exponentialBackoff(SOCKET_RPC_BACKOFF_MULTIPLIER, SOCKET_RPC_BACKOFF_LIMIT))
                .retryBlockingly(() -> {
                    try {
                        SSLSocketFactory sf = null;
                        if (socketConnection.trustStore() != null && socketConnection.trustStorePassword() != null) {
                            System.setProperty("javax.net.ssl.trustStore", socketConnection.trustStore());
                            System.setProperty("javax.net.ssl.trustStorePassword",
                                    socketConnection.trustStorePassword());
                            System.setProperty("javax.net.ssl.trustStoreType", "JKS");

                            sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                        }
                        final var socket = sf == null ? new Socket() : sf.createSocket();
                        socket.connect(new InetSocketAddress(host, port), Math.max(timeout, 0));

                        final var executor = newFixedThreadPool("fx-supervisor-socket-%d");
                        remoteRPC = new SocketRPC<>(agent, supervisor, socket, executor);
                        this.setRemoteRPC(remoteRPC);
                        remoteRPC.open();
                        return null;
                    } catch (final ConnectException e) {
                        clearSSLProperties();
                        throw e;
                    }
                });
    }

    protected OSGiResult connectToMQTT(final BundleContext bundleContext,
                                       final ConfigurationAdmin configurationAdmin,
                                       final Class<A> agent,
                                       final S supervisor,
                                       final MqttConnection connection,
                                       final String conditionID) throws Exception {

        final var ch = new ConfigHelper<>(MqttConfig.class, configurationAdmin);

        ch.read(MqttMessageConstants.ConfigurationPid.CLIENT);
        ch.set(ch.d().id(), connection.clientId());
        ch.set(ch.d().server(), connection.server());
        ch.set(ch.d().port(), connection.port());
        ch.set(ch.d().cleanStart(), true);
        ch.set(ch.d().automaticReconnect(), false);
        ch.set(ch.d().sessionExpiryInterval(), 0L);

        String password = null;
        if (!Strings.isNullOrEmpty(connection.username())) {
            if (!Strings.isNullOrEmpty(connection.password())) {
                password = connection.password();
            } else if (!Strings.isNullOrEmpty(connection.tokenConfig())) {
                final var tokenConfig = TokenProvider.parseJson(connection.tokenConfig(), TokenConfigDTO.class);
                if (tokenConfig.clientId == null) {
                    tokenConfig.clientId = connection.clientId();
                }
                final var tokenProvider = new TokenProvider(tokenConfig);
                password = tokenProvider.get();
            }
        }
        if (!Strings.isNullOrEmpty(password)) {
            ch.set(ch.d().simpleAuth(), true);
            ch.set(ch.d().username(), connection.username());
            ch.set(ch.d().password(), password);
        }
        ch.set(ch.d().sendMaximum(), MAX_CONCURRENT_MSG_TO_SEND);
        ch.set(ch.d().receiveMaximum(), MAX_CONCURRENT_MSG_TO_RECEIVE);
        ch.set(ch.d().maximumPacketSize(), DEFAULT_MAXIMUM_PACKET_SIZE);
        ch.set(ch.d().keepAliveInterval(), 0);
        ch.set(ch.d().sendMaximumPacketSize(), DEFAULT_SEND_MAXIMUM_PACKET_SIZE);
        ch.set(ch.d().connectedListenerFilters(), new String[] { MQTT_CONNECTION_LISTENER_FILTER });
        ch.set(ch.d().disconnectedListenerFilters(), new String[] { MQTT_CONNECTION_LISTENER_FILTER });
        ch.set(ch.d().osgi_ds_satisfying_condition_target(), "(" + CONDITION_ID + "=" + conditionID + ")");
        ch.update();

        // register the condition service to enable messaging client
        final var result = OSGi.register(Condition.class, INSTANCE, Map.of(CONDITION_ID, conditionID))
                .run(bundleContext);

        final var executor = newFixedThreadPool("fx-supervisor-mqtt-%d");
        remoteRPC = new MqttRPC<>(bundleContext, agent, supervisor, connection.subTopic(), connection.pubTopic(),
                                  executor);
        this.setRemoteRPC(remoteRPC);
        remoteRPC.open();

        return result;
    }

    private void setRemoteRPC(final RemoteRPC<S, A> rpc) {
        agent     = rpc.getRemote();
        remoteRPC = rpc;
    }

    public A getAgent() {
        return agent;
    }

    public boolean isOpen() {
        return remoteRPC.isOpen();
    }

    private void clearSSLProperties() {
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
        System.clearProperty("javax.net.ssl.trustStoreType");
    }

    public static ExecutorService newFixedThreadPool(final String namingPattern) {
        final var threadFactory = BasicThreadFactory.builder().namingPattern(namingPattern).daemon(true).build();
        final var executor      = new ThreadPoolExecutor(RPC_POOL_CORE_THREADS_SIZE, RPC_POOL_MAX_THREADS_SIZE,
                                                         RPC_POOL_KEEP_ALIVE_TIME_IN_SECONDS, SECONDS,
                                                         new LinkedBlockingQueue<>(), threadFactory);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

}
