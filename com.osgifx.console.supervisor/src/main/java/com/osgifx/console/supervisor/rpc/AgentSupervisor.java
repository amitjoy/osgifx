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
package com.osgifx.console.supervisor.rpc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.osgifx.console.supervisor.rpc.AgentSupervisor.MqttConfig.MAX_PACKET_SIZE;
import static com.osgifx.console.supervisor.rpc.LauncherSupervisor.MQTT_CONNECTION_LISTENER_FILTER;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLSocketFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

import com.google.common.base.Strings;
import com.osgifx.console.agent.rpc.MqttRPC;
import com.osgifx.console.agent.rpc.RemoteRPC;
import com.osgifx.console.agent.rpc.SocketRPC;
import com.osgifx.console.supervisor.MqttConnection;
import com.osgifx.console.supervisor.SocketConnection;
import com.osgifx.console.util.configuration.ConfigHelper;

import in.bytehue.messaging.mqtt5.api.MqttMessageConstants;

public class AgentSupervisor<S, A> {

    @interface MqttConfig {
        final int MAX_PACKET_SIZE = 256 * 1024 * 1024; // 256 MB max allowed in MQTT

        String id();

        String server();

        int port();

        boolean cleanStart();

        boolean automaticReconnect();

        boolean simpleAuth();

        String username();

        String password();

        int maximumPacketSize();

        int sendMaximumPacketSize();

        String connectedListenerFilter();

        String disconnectedListenerFilter();
    }

    private static final int CONNECT_WAIT = 200;

    private A                    agent;
    protected int                port;
    protected int                timeout;
    protected String             host;
    private RemoteRPC<S, A>      remoteRPC;
    protected volatile int       exitCode;
    private final CountDownLatch latch = new CountDownLatch(1);

    protected void connectToSocket(final Class<A> agent,
                                   final S supervisor,
                                   final SocketConnection socketConnection) throws Exception {

        checkArgument(timeout > -1, "timeout cannot be less than -1");
        checkNotNull(supervisor, "'supervisor' cannot be null");
        checkNotNull(socketConnection.host(), "'host' cannot be null");

        var retryTimeout = timeout;
        host    = socketConnection.host();
        port    = socketConnection.port();
        timeout = socketConnection.timeout();

        while (true) {
            try {
                SSLSocketFactory sf = null;
                if (socketConnection.trustStore() != null && socketConnection.trustStorePassword() != null) {
                    System.setProperty("javax.net.ssl.trustStore", socketConnection.trustStore());
                    System.setProperty("javax.net.ssl.trustStorePassword", socketConnection.trustStorePassword());
                    System.setProperty("javax.net.ssl.trustStoreType", "JKS");

                    sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                }
                final var socket = sf == null ? new Socket() : sf.createSocket();
                socket.connect(new InetSocketAddress(host, port), Math.max(timeout, 0));
                remoteRPC = new SocketRPC<>(agent, supervisor, socket);
                this.setRemoteRPC(remoteRPC);
                remoteRPC.open();
                return;
            } catch (final ConnectException e) {
                clearSSLProperties();
                if (retryTimeout == 0) {
                    throw e;
                }
                if (retryTimeout > 0) {
                    retryTimeout = Math.max(retryTimeout - CONNECT_WAIT, 0);
                }
                Thread.sleep(CONNECT_WAIT);
            }
        }
    }

    protected void connectToMQTT(final BundleContext bundleContext,
                                 final ConfigurationAdmin configurationAdmin,
                                 final Class<A> agent,
                                 final S supervisor,
                                 final MqttConnection connection) {

        final var ch = new ConfigHelper<>(MqttConfig.class, configurationAdmin);

        ch.read(MqttMessageConstants.ConfigurationPid.CLIENT);
        ch.set(ch.d().id(), connection.clientId());
        ch.set(ch.d().server(), connection.server());
        ch.set(ch.d().port(), connection.port());
        ch.set(ch.d().cleanStart(), true);
        ch.set(ch.d().automaticReconnect(), false);

        if (!Strings.isNullOrEmpty(connection.username()) && !Strings.isNullOrEmpty(connection.password())) {
            ch.set(ch.d().simpleAuth(), true);
            ch.set(ch.d().username(), connection.username());
            ch.set(ch.d().password(), connection.password());
        }

        ch.set(ch.d().maximumPacketSize(), MAX_PACKET_SIZE);
        ch.set(ch.d().sendMaximumPacketSize(), MAX_PACKET_SIZE);
        ch.set(ch.d().connectedListenerFilter(), MQTT_CONNECTION_LISTENER_FILTER);
        ch.set(ch.d().disconnectedListenerFilter(), MQTT_CONNECTION_LISTENER_FILTER);
        ch.update();

        remoteRPC = new MqttRPC<>(bundleContext, agent, supervisor, connection.subTopic(), connection.pubTopic());
        this.setRemoteRPC(remoteRPC);
        remoteRPC.open();
    }

    private void setRemoteRPC(final RemoteRPC<S, A> rpc) {
        agent     = rpc.getRemote();
        remoteRPC = rpc;
    }

    public int join() throws InterruptedException {
        latch.await();
        return exitCode;
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

}
