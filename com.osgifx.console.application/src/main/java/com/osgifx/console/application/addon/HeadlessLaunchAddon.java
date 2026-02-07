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
package com.osgifx.console.application.addon;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.REMOTE_RPC;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.SNAPSHOT;

import java.io.File;
import java.io.FileReader;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.gson.Gson;
import com.osgifx.console.application.dto.HeadlessConfigDTO;
import com.osgifx.console.application.dto.HeadlessConnectionType;
import com.osgifx.console.supervisor.MqttConnection;
import com.osgifx.console.supervisor.SocketConnection;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.supervisor.factory.SupervisorFactory;
import com.osgifx.console.util.fx.FxDialog;

public final class HeadlessLaunchAddon {

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    private IEventBroker               eventBroker;
    @Inject
    @Optional
    private Supervisor                 supervisor;
    @Inject
    private SupervisorFactory          supervisorFactory;
    @Inject
    @Optional
    @ContextValue("headless.config")
    private HeadlessConfigDTO          headlessConfig;
    @Inject
    @Optional
    @ContextValue("is_connected")
    private ContextBoundValue<Boolean> isConnected;
    @Inject
    @Optional
    @ContextValue("is_local_agent")
    private ContextBoundValue<Boolean> isLocalAgent;
    @Inject
    @Optional
    @ContextValue("is_snapshot_agent")
    private ContextBoundValue<Boolean> isSnapshotAgent;
    @Inject
    @Optional
    @ContextValue("connected.agent")
    private ContextBoundValue<String>  connectedAgent;

    @PostConstruct
    public void init() {
        logger.atInfo().log("HeadlessLaunchAddon initialized");
        // check if a configuration file is provided for headless launch
        final var configPath = System.getProperty("osgifx.config");
        if (configPath != null) {
            logger.atInfo().log("Headless configuration config path found: %s", configPath);
            try (var reader = new FileReader(new File(configPath))) {
                final var gson = new Gson();
                headlessConfig = gson.fromJson(reader, HeadlessConfigDTO.class);
                logger.atInfo().log("Headless configuration parsed: %s", headlessConfig);
            } catch (final Exception e) {
                logger.atError().withException(e).log("Failed to parse headless configuration");
                // if parsing fails, log and proceed to show the connection wizard or handle it in the addon
                e.printStackTrace();
            }
        } else {
            logger.atInfo().log("System property 'osgifx.config' is not set");
        }
        if (headlessConfig == null) {
            logger.atInfo()
                    .log("No headless configuration found (dependency injection is null and system property is null)");
            return;
        }
        logger.atInfo().log("Headless configuration found: %s", headlessConfig);
        try {
            // Remove any existing supervisor (e.g. Snapshot)
            supervisorFactory.removeSupervisor(SNAPSHOT);
            supervisorFactory.createSupervisor(REMOTE_RPC);

            if (HeadlessConnectionType.SOCKET == headlessConfig.type) {
                connectSocket();
            } else if (HeadlessConnectionType.MQTT == headlessConfig.type) {
                connectMqtt();
            } else {
                logger.atWarning().log("Unknown connection type: %s", headlessConfig.type);
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Failed to establish headless connection");
            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
        }
    }

    private void connectSocket() throws Exception {
        if (headlessConfig.socket == null) {
            logger.atWarning().log("Socket configuration is missing");
            return;
        }
        // @formatter:off
        final var socketConnection = SocketConnection
                .builder()
                .host(headlessConfig.socket.host)
                .port(headlessConfig.socket.port)
                .timeout(headlessConfig.socket.timeout)
                .truststore(headlessConfig.socket.trustStorePath)
                .truststorePass(headlessConfig.socket.trustStorePassword)
                .build();
        // @formatter:on

        supervisor.connect(socketConnection);
        logger.atInfo().log("Successfully connected to Socket Agent at %s:%s", headlessConfig.socket.host,
                headlessConfig.socket.port);
        broadcastConnection("[SOCKET] " + headlessConfig.socket.host + ":" + headlessConfig.socket.port);
    }

    private void connectMqtt() throws Exception {
        if (headlessConfig.mqtt == null) {
            logger.atWarning().log("MQTT configuration is missing");
            return;
        }
        // @formatter:off
        final var mqttConnection = MqttConnection
                .builder()
                .clientId(headlessConfig.mqtt.clientId)
                .server(headlessConfig.mqtt.server)
                .port(headlessConfig.mqtt.port)
                .timeout(headlessConfig.mqtt.timeout)
                .username(headlessConfig.mqtt.username)
                .password(headlessConfig.mqtt.password)
                .tokenConfig(null) // Token config not yet supported in CLI JSON for simplicity, can be added if needed
                .pubTopic(headlessConfig.mqtt.pubTopic)
                .subTopic(headlessConfig.mqtt.subTopic)
                .lwtTopic(headlessConfig.mqtt.lwtTopic)
                .build();
        // @formatter:on

        supervisor.connect(mqttConnection);
        logger.atInfo().log("Successfully connected to MQTT Agent at %s:%s", headlessConfig.mqtt.server,
                headlessConfig.mqtt.port);
        broadcastConnection("[MQTT] " + headlessConfig.mqtt.server + ":" + headlessConfig.mqtt.port);
    }

    private void broadcastConnection(final String connectionString) {
        eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC, connectionString);
        connectedAgent.publish(connectionString);
        isConnected.publish(true);
        isLocalAgent.publish(false);
        isSnapshotAgent.publish(false);
    }
}
