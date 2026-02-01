/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
package com.osgifx.console.agent.starter;

import static com.osgifx.console.agent.Agent.AGENT_MQTT_PROVIDER_DEFAULT_VALUE;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_PROVIDER_KEY;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_PUB_TOPIC_KEY;
import static com.osgifx.console.agent.Agent.AGENT_MQTT_SUB_TOPIC_KEY;
import static com.osgifx.console.agent.provider.AgentServer.RpcType.MQTT_RPC;
import static com.osgifx.console.agent.provider.AgentServer.RpcType.ZMQ_RPC;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

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
import com.osgifx.console.agent.helper.ThreadFactoryBuilder;
import com.osgifx.console.agent.provider.AgentServer;
import com.osgifx.console.agent.provider.ClassloaderLeakDetector;
import com.osgifx.console.agent.provider.PackageWirings;
import com.osgifx.console.agent.rpc.RemoteRPC;
import com.osgifx.console.agent.rpc.mqtt.MqttRPC;
import com.osgifx.console.agent.rpc.mqtt.SimpleMqtt5Publisher;
import com.osgifx.console.agent.rpc.mqtt.SimpleMqtt5Subscriber;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Publisher;
import com.osgifx.console.agent.rpc.mqtt.api.Mqtt5Subscriber;
import com.osgifx.console.agent.rpc.zmq.ZeroMqRPC;
import com.osgifx.console.supervisor.Supervisor;

/**
 * The agent bundles uses an activator instead of DS to not constrain the target
 * environment in any way.
 */
@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public final class Activator implements BundleActivator {

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

    private DIModule                     module;
    private RemoteRPC<Agent, Supervisor> rpc;
    private AgentServer                  agentServer;
    private final FluentLogger           logger = LoggerFactory.getFluentLogger(getClass());

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        module = new DIModule(bundleContext);
        module.di().getInstance(ClassloaderLeakDetector.class).start();
        module.start();

        // Check if MQTT is configured via property
        final String mqttProviderProperty = bundleContext.getProperty(AGENT_MQTT_PROVIDER_KEY);

        if (mqttProviderProperty != null) {
            // =================================================================
            // MQTT MODE
            // =================================================================

            // Validate Topics
            final String pubTopic = bundleContext.getProperty(AGENT_MQTT_PUB_TOPIC_KEY);
            final String subTopic = bundleContext.getProperty(AGENT_MQTT_SUB_TOPIC_KEY);

            if (pubTopic == null || pubTopic.isEmpty() || subTopic == null || subTopic.isEmpty()) {
                logger.atWarn().msg("[OSGi.fx] MQTT agent topics not configured").log();
                return;
            }

            // Validate OSGi Messaging / Package Wiring
            final boolean isOSGiMessagingProviderConfigured = AGENT_MQTT_PROVIDER_DEFAULT_VALUE
                    .equals(mqttProviderProperty);

            if (isOSGiMessagingProviderConfigured) {
                logger.atInfo().msg("[OSGi.fx] OSGi messaging provider configured for MQTT communication").log();

                final boolean isOSGiMessagingPackageWired = module.di().getInstance(PackageWirings.class).isMqttWired();
                if (!isOSGiMessagingPackageWired) {
                    logger.atWarn().msg("[OSGi.fx] OSGi messaging bundle for MQTT not installed").log();
                    return;
                }

                // Register MQTT Services
                bundleContext.registerService(Mqtt5Publisher.class, new SimpleMqtt5Publisher(bundleContext), null);
                bundleContext.registerService(Mqtt5Subscriber.class, new SimpleMqtt5Subscriber(bundleContext), null);
            } else {
                logger.atInfo().msg("[OSGi.fx] Custom messaging provider configured for MQTT communication").log();
            }

            // Start MQTT RPC
            agentServer = new AgentServer(module.di(), MQTT_RPC);

            final ExecutorService              executor = newFixedThreadPool();
            final RemoteRPC<Agent, Supervisor> mqttRPC  = new MqttRPC<>(bundleContext, Supervisor.class, agentServer,
                                                                        pubTopic, subTopic, executor);

            // Bind instances to DI Module
            module.bindInstance(AgentServer.class, agentServer);
            module.bindInstance(RemoteRPC.class, mqttRPC);
            module.bindInstance(Supervisor.class, mqttRPC.getRemote());

            mqttRPC.open();
            agentServer.setEndpoint(mqttRPC);
            this.rpc = mqttRPC;

            logger.atInfo().msg("[OSGi.fx] MQTT agent configured").log();
            logger.atInfo().msg("[OSGi.fx] PUB Topic: {}").arg(pubTopic).log();
            logger.atInfo().msg("[OSGi.fx] SUB Topic: {}").arg(subTopic).log();

        } else {
            // =================================================================
            // ZERO MQ MODE (Default/LAN)
            // =================================================================

            agentServer = new AgentServer(module.di(), ZMQ_RPC);

            // Initialize ZeroMqRPC in SERVER mode (host = null).
            final ZeroMqRPC<Agent, Supervisor> zmqRpc = new ZeroMqRPC<>(Supervisor.class, agentServer);

            // Bind instances to DI Module
            module.bindInstance(AgentServer.class, agentServer);
            module.bindInstance(RemoteRPC.class, zmqRpc);
            module.bindInstance(Supervisor.class, zmqRpc.getRemote());

            agentServer.setEndpoint(zmqRpc);
            zmqRpc.open();
            this.rpc = zmqRpc;

            logger.atInfo().msg("[OSGi.fx] ZeroMQ agent configured on ports 5555/5556").log();
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        // Close RPC (Stops ZMQ threads or Disconnects MQTT)
        if (rpc != null) {
            rpc.close();
        }

        // Close Agent Server
        if (agentServer != null) {
            agentServer.close();
        }

        // Stop DI Module
        if (module != null) {
            module.di().getInstance(ClassloaderLeakDetector.class).stop();
            module.stop();
        }
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