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
import static com.osgifx.console.agent.Agent.AGENT_SOCKET_SECURE_COMMUNICATION_KEY;
import static com.osgifx.console.agent.Agent.AGENT_SOCKET_SECURE_COMMUNICATION_SSL_CONTEXT_FILTER_KEY;

import java.util.Map;

import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.helper.AgentHelper;

/**
 * A Gogo command that dynamically configures and starts/stops the OSGi.fx agent
 * without introducing dependencies on ConfigAdmin, SCR, or Felix annotations.
 */
public final class AgentCommand {

    private final Activator    activator;
    private final BundleContext bundleContext;

    public AgentCommand(final Activator activator, final BundleContext bundleContext) {
        this.activator     = activator;
        this.bundleContext = bundleContext;
    }

    /**
     * Starts the socket agent using the provided configuration properties.
     * Properties will be coerced from Gogo Map syntax: agent:startSocket [port=1099 host=localhost]
     *
     * @param config the configuration properties
     */
    public void startSocket(final Map<String, String> config) {
        if (config != null) {
            final String port = config.getOrDefault("port", "1099");
            final String host = config.getOrDefault("host", "localhost");
            System.setProperty(AGENT_SOCKET_PORT_KEY, host + ":" + port);

            if (config.containsKey("secure")) {
                System.setProperty(AGENT_SOCKET_SECURE_COMMUNICATION_KEY, config.get("secure"));
            }
            if (config.containsKey("sslContextFilter")) {
                System.setProperty(AGENT_SOCKET_SECURE_COMMUNICATION_SSL_CONTEXT_FILTER_KEY,
                        config.get("sslContextFilter"));
            }
        }
        startSocket();
    }

    /**
     * Starts the socket agent using the existing system properties or defaults.
     */
    public void startSocket() {
        try {
            activator.startSocketAgent();
        } catch (final Exception e) {
            System.err.println("[OSGi.fx] Failed to start Socket agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops the socket agent and clears the related system properties.
     */
    public void stopSocket() {
        System.setProperty(AGENT_SOCKET_PORT_KEY, "");
        System.setProperty(AGENT_SOCKET_SECURE_COMMUNICATION_KEY, "");
        System.setProperty(AGENT_SOCKET_SECURE_COMMUNICATION_SSL_CONTEXT_FILTER_KEY, "");

        try {
            activator.stopSocketAgent();
        } catch (final Exception e) {
            System.err.println("[OSGi.fx] Failed to stop Socket agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Starts the MQTT agent using the provided configuration properties.
     * Properties will be coerced from Gogo Map syntax: agent:startMqtt [provider=osgi-messaging pubTopic=/out
     * subTopic=/in]
     *
     * @param config the configuration properties
     */
    public void startMqtt(final Map<String, String> config) {
        if (config != null) {
            if (config.containsKey("provider")) {
                System.setProperty(AGENT_MQTT_PROVIDER_KEY, config.get("provider"));
            }
            if (config.containsKey("pubTopic")) {
                System.setProperty(AGENT_MQTT_PUB_TOPIC_KEY, config.get("pubTopic"));
            }
            if (config.containsKey("subTopic")) {
                System.setProperty(AGENT_MQTT_SUB_TOPIC_KEY, config.get("subTopic"));
            }
        }
        startMqtt();
    }

    /**
     * Starts the MQTT agent using the existing system properties or defaults.
     */
    public void startMqtt() {
        try {
            activator.startMqttAgent();
        } catch (final Exception e) {
            System.err.println("[OSGi.fx] Failed to start MQTT agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops the MQTT agent and clears the related system properties.
     */
    public void stopMqtt() {
        System.setProperty(AGENT_MQTT_PROVIDER_KEY, "");
        System.setProperty(AGENT_MQTT_PUB_TOPIC_KEY, "");
        System.setProperty(AGENT_MQTT_SUB_TOPIC_KEY, "");

        try {
            activator.stopMqttAgent();
        } catch (final Exception e) {
            System.err.println("[OSGi.fx] Failed to stop MQTT agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prints the current running status and configured properties of the OSGi.fx agent.
     */
    public void status() {
        System.out.println("===============================");
        System.out.println("     OSGi.fx Agent Status      ");
        System.out.println("===============================");

        final boolean isSocketRunning = activator.isSocketAgentRunning();
        System.out.println("Socket Endpoint: " + (isSocketRunning ? "RUNNING" : "DORMANT"));
        if (isSocketRunning) {
            printProperty(AGENT_SOCKET_PORT_KEY, "localhost:1099");
            printProperty(AGENT_SOCKET_SECURE_COMMUNICATION_KEY, "false");
            printProperty(AGENT_SOCKET_SECURE_COMMUNICATION_SSL_CONTEXT_FILTER_KEY, "");
        }

        System.out.println();

        final boolean isMqttRunning = activator.isMqttAgentRunning();
        System.out.println("MQTT Endpoint  : " + (isMqttRunning ? "RUNNING" : "DORMANT"));
        if (isMqttRunning) {
            printProperty(AGENT_MQTT_PROVIDER_KEY, AGENT_MQTT_PROVIDER_DEFAULT_VALUE);
            printProperty(AGENT_MQTT_PUB_TOPIC_KEY, "");
            printProperty(AGENT_MQTT_SUB_TOPIC_KEY, "");
        }
    }

    private void printProperty(final String key, final String defaultVal) {
        String val = AgentHelper.getProperty(key, bundleContext);
        if (val == null || val.trim().isEmpty()) {
            val = defaultVal;
        }
        if (val != null && !val.trim().isEmpty()) {
            final String shortKey = key.replace("osgi.fx.agent.", "");
            System.out.printf("  %s : %s%n", shortKey, val);
        }
    }
}
