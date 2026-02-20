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
package com.osgifx.console.agent.starter;

import static com.osgifx.console.agent.Agent.AGENT_MQTT_PROVIDER_DEFAULT_VALUE;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_PROVIDER_KEY;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_PUB_TOPIC_KEY;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_SUB_TOPIC_KEY;
import static com.osgifx.console.agent.Agent.AGENT_SOCKET_PORT_KEY;
import static com.osgifx.console.agent.provider.AgentServer.RpcType.MQTT_RPC;
import static com.osgifx.console.agent.provider.AgentServer.RpcType.SOCKET_RPC;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.di.module.DIModule;
import com.osgifx.console.agent.helper.AgentHelper;
import com.osgifx.console.agent.helper.ThreadFactoryBuilder;
import com.osgifx.console.agent.provider.AgentServer;
import com.osgifx.console.agent.provider.ClassloaderLeakDetector;
import com.osgifx.console.agent.provider.PackageWirings;
import com.osgifx.console.agent.rpc.RemoteRPC;
import com.osgifx.console.agent.rpc.mqtt.MqttRPC;
import com.osgifx.console.agent.rpc.socket.SocketRPC;
import com.osgifx.console.supervisor.Supervisor;

import aQute.lib.io.IO;

/**
 * The agent bundles uses an activator instead of DS to not constrain the target
 * environment in any way.
 */
@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public final class Activator extends Thread implements BundleActivator {

    private static final int    RPC_POOL_CORE_THREADS_SIZE          = 10;
    private static final int    RPC_POOL_MAX_THREADS_SIZE           = 20;
    private static final int    RPC_POOL_KEEP_ALIVE_TIME_IN_SECONDS = 60;
    private static final String RPC_POOL_THREAD_NAME_SUFFIX         = "-%d";
    private static final String RPC_POOL_THREAD_NAME_PREFIX         = "osgifx-agent";

    // @formatter:off
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
                                                              .setThreadFactoryName(RPC_POOL_THREAD_NAME_PREFIX)
                                                              .setThreadNameFormat(RPC_POOL_THREAD_NAME_SUFFIX)
                                                              .setDaemon(true)
                                                              .build();
    // @formatter:on

    private DIModule                module;
    private ServerSocket            serverSocket;
    private final FluentLogger      logger = LoggerFactory.getFluentLogger(getClass());
    private final List<AgentServer> agents = new CopyOnWriteArrayList<>();

    private volatile boolean isSocketAgentRunning = false;
    private volatile boolean isMqttAgentRunning   = false;

    private ExecutorService              mqttExecutor;
    private RemoteRPC<Agent, Supervisor> mqttRpc;
    private AgentServer                  mqttAgentServer;

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        module = new DIModule(bundleContext);
        module.di().getInstance(ClassloaderLeakDetector.class).start();
        module.start(); // Start DIModule and ServiceTrackers unconditionally for fast connection times

        registerAgentCommand(bundleContext);

        if (isSocketConfigured(bundleContext)) {
            startSocketAgent();
        } else {
            logger.atInfo()
                    .msg("[OSGi.fx] Socket agent dormant. Awaiting 'osgifx:agent startSocket' or system properties.")
                    .log();
        }

        if (isMqttConfigured(bundleContext)) {
            startMqttAgent();
        } else {
            logger.atInfo().msg("[OSGi.fx] MQTT agent dormant. Awaiting 'osgifx:agent startMqtt' or system properties.")
                    .log();
        }
    }

    private void registerAgentCommand(final BundleContext bundleContext) {
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("osgi.command.scope", "agent");
        props.put("osgi.command.function",
                new String[] { "startSocket", "stopSocket", "startMqtt", "stopMqtt", "status" });
        bundleContext.registerService(Object.class.getName(), new AgentCommand(this, bundleContext), props);
    }

    private boolean isSocketConfigured(final BundleContext bundleContext) {
        final String port = AgentHelper.getProperty(AGENT_SOCKET_PORT_KEY, bundleContext);
        return port != null && !port.trim().isEmpty();
    }

    private boolean isMqttConfigured(final BundleContext bundleContext) {
        final String provider = AgentHelper.getProperty(AGENT_MQTT_PROVIDER_KEY, bundleContext);
        return provider != null && !provider.trim().isEmpty();
    }

    public synchronized void startSocketAgent() throws Exception {
        if (isSocketAgentRunning) {
            logger.atInfo().msg("[OSGi.fx] Socket agent is already running.").log();
            return;
        }

        final SocketContext socketContext = new SocketContext(module.di().getInstance(BundleContext.class));
        serverSocket = socketContext.getSocket();
        start(); // starts the thread for accepting socket connections

        isSocketAgentRunning = true;
        logger.atInfo().msg("[OSGi.fx] Socket agent configured").log();
        logger.atInfo().msg("[OSGi.fx] Host: {}").arg(socketContext.host()).log();
        logger.atInfo().msg("[OSGi.fx] Port: {}").arg(socketContext.port()).log();
    }

    public synchronized void stopSocketAgent() throws Exception {
        if (!isSocketAgentRunning) {
            return;
        }
        interrupt(); // interrupt the socket accept thread
        IO.close(serverSocket);

        // Close all active socket agents so it actually disconnects
        agents.stream().filter(a -> a.getRpcType() == SOCKET_RPC).forEach(a -> {
            try {
                IO.close(a);
            } catch (Exception e) {
                // ignore
            }
        });
        agents.removeIf(a -> a.getRpcType() == SOCKET_RPC);

        isSocketAgentRunning = false;
        logger.atInfo().msg("[OSGi.fx] Socket agent stopped").log();
    }

    public synchronized void startMqttAgent() throws Exception {
        if (isMqttAgentRunning) {
            logger.atInfo().msg("[OSGi.fx] MQTT agent is already running.").log();
            return;
        }

        final BundleContext bundleContext        = module.di().getInstance(BundleContext.class);
        final String        mqttProviderProperty = AgentHelper.getProperty(AGENT_MQTT_PROVIDER_KEY, bundleContext);
        if (mqttProviderProperty == null) {
            logger.atInfo().msg("[OSGi.fx] MQTT agent not configured").log();
            return;
        }
        final String pubTopic = AgentHelper.getProperty(AGENT_MQTT_PUB_TOPIC_KEY, bundleContext);
        final String subTopic = AgentHelper.getProperty(AGENT_MQTT_SUB_TOPIC_KEY, bundleContext);

        if (pubTopic == null || pubTopic.isEmpty() || subTopic == null || subTopic.isEmpty()) {
            logger.atWarn().msg("[OSGi.fx] MQTT agent topics not configured").log();
            return;
        }

        final boolean isOSGiMessagingProviderConfigured = AGENT_MQTT_PROVIDER_DEFAULT_VALUE
                .equals(mqttProviderProperty);

        if (isOSGiMessagingProviderConfigured) {
            logger.atInfo().msg("[OSGi.fx] OSGi messaging provider configured for MQTT communication").log();

            final boolean isOSGiMessagingPackageWired = module.di().getInstance(PackageWirings.class).isMqttWired();
            if (!isOSGiMessagingPackageWired) {
                logger.atWarn().msg("[OSGi.fx] OSGi messaging bundle for MQTT not installed").log();
                return;
            }
        } else {
            logger.atInfo().msg("[OSGi.fx] Custom messaging provider configured for MQTT communication").log();
        }

        mqttAgentServer = new AgentServer(module.di(), MQTT_RPC);
        agents.add(mqttAgentServer);

        mqttExecutor = newFixedThreadPool();
        mqttRpc      = new MqttRPC<>(bundleContext, Supervisor.class, mqttAgentServer, pubTopic, subTopic,
                                     mqttExecutor);

        module.bindInstance(AgentServer.class, mqttAgentServer);
        module.bindInstance(RemoteRPC.class, mqttRpc);
        module.bindInstance(Supervisor.class, mqttRpc.getRemote());

        mqttRpc.open();
        mqttAgentServer.setEndpoint(mqttRpc);

        isMqttAgentRunning = true;
        logger.atInfo().msg("[OSGi.fx] MQTT agent configured").log();
        logger.atInfo().msg("[OSGi.fx] PUB Topic: {}").arg(pubTopic).log();
        logger.atInfo().msg("[OSGi.fx] SUB Topic: {}").arg(subTopic).log();
    }

    public synchronized void stopMqttAgent() throws Exception {
        if (!isMqttAgentRunning) {
            return;
        }
        if (mqttRpc != null) {
            try {
                mqttRpc.close();
            } catch (Exception e) {
                logger.atWarn().msg("[OSGi.fx] Error closing MQTT RPC").throwable(e).log();
            }
            mqttRpc = null;
        }
        if (mqttExecutor != null) {
            mqttExecutor.shutdownNow();
            mqttExecutor = null;
        }
        // Close all active MQTT agents so it actually disconnects
        agents.stream().filter(a -> a.getRpcType() == MQTT_RPC).forEach(a -> {
            try {
                IO.close(a);
            } catch (Exception e) {
                // ignore
            }
        });
        agents.removeIf(a -> a.getRpcType() == MQTT_RPC);
        mqttAgentServer    = null;
        isMqttAgentRunning = false;
        logger.atInfo().msg("[OSGi.fx] MQTT agent stopped").log();
    }

    public boolean isSocketAgentRunning() {
        return isSocketAgentRunning;
    }

    public boolean isMqttAgentRunning() {
        return isMqttAgentRunning;
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

                    final ExecutorService              executor  = newFixedThreadPool();
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
                } catch (final SocketException e) {
                    if (!isInterrupted()) {
                        logger.atWarn().msg("[OSGi.fx] Accepting agent requests").throwable(e).log();
                    }
                } catch (final Exception e) {
                    logger.atWarn().msg("[OSGi.fx] Accepting agent requests").throwable(e).log();
                } catch (final Throwable t) {
                    logger.atError().throwable(t).log();
                }
            }
        } catch (final Throwable t) {
            logger.atError().throwable(t).log();
            throw t;
        } finally {
            IO.close(serverSocket);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        stopSocketAgent();
        stopMqttAgent();
        agents.forEach(IO::close);
        module.di().getInstance(ClassloaderLeakDetector.class).stop();
        module.stop();
    }

    public static ExecutorService newFixedThreadPool() {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(RPC_POOL_CORE_THREADS_SIZE,
                                                                   RPC_POOL_MAX_THREADS_SIZE,
                                                                   RPC_POOL_KEEP_ALIVE_TIME_IN_SECONDS, SECONDS,
                                                                   new LinkedBlockingQueue<>(), THREAD_FACTORY);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

}
