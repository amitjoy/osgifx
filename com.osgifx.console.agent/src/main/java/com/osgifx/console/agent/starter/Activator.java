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

import static com.osgifx.console.agent.Agent.AGENT_MQTT_PUB_TOPIC_KEY;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_SUB_TOPIC_KEY;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.di.module.DIModule;
import com.osgifx.console.agent.provider.AgentServer;
import com.osgifx.console.agent.provider.AgentServer.RpcType;
import com.osgifx.console.agent.provider.ClassloaderLeakDetector;
import com.osgifx.console.agent.provider.PackageWirings;
import com.osgifx.console.agent.rpc.MqttRPC;
import com.osgifx.console.agent.rpc.RemoteRPC;
import com.osgifx.console.agent.rpc.SocketRPC;
import com.osgifx.console.supervisor.Supervisor;

/**
 * The agent bundles uses an activator instead of DS to not constrain the target
 * environment in any way.
 */
@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public final class Activator extends Thread implements BundleActivator {

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
            System.err.println("[OSGi.fx] Socket Agent: " + socketContext.host() + ":" + socketContext.port());
        } catch (final IllegalArgumentException e) {
            System.err.println("[OSGi.fx] Socket agent not configured");
        }
        module.start();

        final boolean isMQTTwired = module.di().getInstance(PackageWirings.class).isMqttWired();
        if (isMQTTwired) {
            final String pubTopic = bundleContext.getProperty(AGENT_MQTT_PUB_TOPIC_KEY);
            final String subTopic = bundleContext.getProperty(AGENT_MQTT_SUB_TOPIC_KEY);

            if (pubTopic == null || pubTopic.isEmpty() || subTopic == null || subTopic.isEmpty()) {
                System.err.print("[OSGi.fx] MQTT agent not configured");
                return;
            }
            final AgentServer agentServer = new AgentServer(module.di(), RpcType.MQTT_RPC);
            agents.add(agentServer);
            final RemoteRPC<Agent, Supervisor> mqttRPC = new MqttRPC<>(bundleContext, Supervisor.class, agentServer,
                                                                       pubTopic, subTopic);

            module.bindInstance(AgentServer.class, agentServer);
            module.bindInstance(RemoteRPC.class, mqttRPC);
            module.bindInstance(Supervisor.class, mqttRPC.getRemote());

            mqttRPC.open();
            agentServer.setEndpoint(mqttRPC);
        }
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
                    final AgentServer agentServer = new AgentServer(module.di(), RpcType.SOCKET_RPC);
                    agents.add(agentServer);

                    final SocketRPC<Agent, Supervisor> socketRPC = new SocketRPC<Agent, Supervisor>(Supervisor.class,
                                                                                                    agentServer,
                                                                                                    socket) {
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
