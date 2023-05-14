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
package com.osgifx.console.agent.starter;

import static com.osgifx.console.agent.Agent.AGENT_MQTT_PROVIDER_KEY;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_PROVIDER_OSGI_VALUE;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_PUB_TOPIC_KEY;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_SUB_TOPIC_KEY;
import static com.osgifx.console.agent.provider.AgentServer.RpcType.MQTT_RPC;
import static com.osgifx.console.agent.provider.AgentServer.RpcType.SOCKET_RPC;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.di.module.DIModule;
import com.osgifx.console.agent.helper.ThreadFactoryBuilder;
import com.osgifx.console.agent.provider.AgentServer;
import com.osgifx.console.agent.provider.ClassloaderLeakDetector;
import com.osgifx.console.agent.provider.PackageWirings;
import com.osgifx.console.agent.provider.mqtt.OSGiMqtt5Publisher;
import com.osgifx.console.agent.provider.mqtt.OSGiMqtt5Subscriber;
import com.osgifx.console.agent.rpc.RemoteRPC;
import com.osgifx.console.agent.rpc.mqtt.MqttRPC;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Publisher;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Subscriber;
import com.osgifx.console.agent.rpc.socket.SocketRPC;
import com.osgifx.console.supervisor.Supervisor;

/**
 * The agent bundles uses an activator instead of DS to not constrain the target
 * environment in any way.
 */
@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public final class Activator extends Thread implements BundleActivator {

    private static final int    RPC_POOL_THREADS            = 5;
    private static final String RPC_POOL_THREAD_NAME_PREFIX = "osgifx-agent";
    private static final String RPC_POOL_THREAD_NAME_SUFFIX = "-%d";
    // @formatter:off
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
                                                              .setThreadFactoryName(RPC_POOL_THREAD_NAME_PREFIX)
                                                              .setThreadNameFormat(RPC_POOL_THREAD_NAME_SUFFIX)
                                                              .setDaemon(true)
                                                              .build();
    // @formatter:on

    private DIModule                module;
    private ServerSocket            serverSocket;
    private final List<AgentServer> agents = new CopyOnWriteArrayList<>();

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        module = new DIModule(bundleContext);
        module.di().getInstance(ClassloaderLeakDetector.class).start();

        try {
            final SocketContext socketContext = new SocketContext(bundleContext);
            serverSocket = socketContext.getSocket();
            start();
            System.err.println("[OSGi.fx] Socket agent configured");
            System.err.println(String.format("[OSGi.fx] Host: %s", socketContext.host()));
            System.err.println(String.format("[OSGi.fx] Port: %s", socketContext.port()));
        } catch (final IllegalArgumentException e) {
            System.err.println("[OSGi.fx] Socket agent not configured");
        }
        module.start();

        final String mqttProviderValue = System.getProperty(AGENT_MQTT_PROVIDER_KEY);
        if (mqttProviderValue == null) {
            System.err.println("[OSGi.fx] MQTT agent not configured");
            return;
        }
        final boolean isMQTTwired = module.di().getInstance(PackageWirings.class).isMqttWired();
        if (isMQTTwired) {
            System.err.println("[OSGi.fx] OSGi messaging bundle for MQTT is not installed");
            return;
        }
        final String pubTopic = bundleContext.getProperty(AGENT_MQTT_PUB_TOPIC_KEY);
        final String subTopic = bundleContext.getProperty(AGENT_MQTT_SUB_TOPIC_KEY);

        if (pubTopic == null || pubTopic.isEmpty() || subTopic == null || subTopic.isEmpty()) {
            System.err.println("[OSGi.fx] MQTT agent topics not configured");
            return;
        }

        final AgentServer agentServer = new AgentServer(module.di(), MQTT_RPC);
        agents.add(agentServer);

        final boolean isOSGiMessagingProvider = AGENT_MQTT_PROVIDER_OSGI_VALUE.equalsIgnoreCase(mqttProviderValue);
        if (isOSGiMessagingProvider) {
            bundleContext.registerService(Mqtt5Publisher.class, new OSGiMqtt5Publisher(bundleContext), null);
            bundleContext.registerService(Mqtt5Subscriber.class, new OSGiMqtt5Subscriber(bundleContext), null);
        } else {
            final ServiceReference<Mqtt5Publisher>  pubRef = bundleContext.getServiceReference(Mqtt5Publisher.class);
            final ServiceReference<Mqtt5Subscriber> subRef = bundleContext.getServiceReference(Mqtt5Subscriber.class);

            if (pubRef == null || subRef == null) {
                System.err.println("[OSGi.fx] MQTT services ain't available");
                return;
            }
        }
        final ExecutorService              executor = Executors.newFixedThreadPool(RPC_POOL_THREADS, THREAD_FACTORY);
        final RemoteRPC<Agent, Supervisor> mqttRPC  = new MqttRPC<>(bundleContext, Supervisor.class, agentServer,
                                                                    pubTopic, subTopic, executor);

        module.bindInstance(AgentServer.class, agentServer);
        module.bindInstance(RemoteRPC.class, mqttRPC);
        module.bindInstance(Supervisor.class, mqttRPC.getRemote());

        mqttRPC.open();
        agentServer.setEndpoint(mqttRPC);

        System.err.println("[OSGi.fx] MQTT agent configured");
        System.err.println(String.format("[OSGi.fx] PUB Topic: %s", pubTopic));
        System.err.println(String.format("[OSGi.fx] SUB Topic: %s", subTopic));
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                try {
                    final Socket socket = serverSocket.accept();
                    // timeout to get interrupts
                    socket.setSoTimeout(1000);

                    // create a new agent, and link it up.
                    final AgentServer agentServer = new AgentServer(module.di(), SOCKET_RPC);
                    agents.add(agentServer);

                    final ExecutorService              executor  = Executors.newFixedThreadPool(RPC_POOL_THREADS,
                            THREAD_FACTORY);
                    final SocketRPC<Agent, Supervisor> socketRPC = new SocketRPC<Agent, Supervisor>(Supervisor.class,
                                                                                                    agentServer, socket,
                                                                                                    executor) {
                                                                     @Override
                                                                     public void close() throws IOException {
                                                                         agents.remove(agentServer);
                                                                         super.close();
                                                                     }
                                                                 };
                    module.bindInstance(AgentServer.class, agentServer);
                    module.bindInstance(RemoteRPC.class, socketRPC);
                    module.bindInstance(Supervisor.class, socketRPC.getRemote());

                    agentServer.setEndpoint(socketRPC);
                    socketRPC.run();
                } catch (final Exception e) {
                } catch (final Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (final Throwable t) {
            t.printStackTrace(System.err);
            throw t;
        } finally {
            close(serverSocket);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        interrupt();
        close(serverSocket);
        agents.forEach(this::close);
        module.di().getInstance(ClassloaderLeakDetector.class).stop();
        module.stop();
    }

    private Throwable close(final Closeable in) {
        try {
            if (in != null) {
                in.close();
            }
        } catch (final Throwable e) {
            return e;
        }
        return null;
    }

}
