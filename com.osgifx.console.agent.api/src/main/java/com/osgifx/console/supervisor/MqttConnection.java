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
package com.osgifx.console.supervisor;

import static java.util.Objects.requireNonNull;

/**
 * The MqttConnection class represents an MQTT connection configuration.
 * It encapsulates various parameters required to establish a connection
 * to an MQTT broker.
 */
public class MqttConnection {

    // The client ID for the MQTT connection
    private final String clientId;
    // The server address of the MQTT broker
    private final String server;
    // The port number to connect to the MQTT broker
    private final int port;
    // The connection timeout value
    private final int timeout;
    // The username for authentication
    private final String username;
    // The password for authentication
    private final String password;
    // The token configuration for the connection
    private final String tokenConfig;
    // The topic to publish messages to
    private final String pubTopic;
    // The topic to subscribe to for receiving messages
    private final String subTopic;
    // The Last Will and Testament (LWT) topic
    private final String lwtTopic;

    /**
     * Private constructor to enforce the use of the builder for object creation.
     */
    private MqttConnection(final String clientId,
                           final String server,
                           final int port,
                           final int timeout,
                           final String username,
                           final String password,
                           final String tokenConfig,
                           final String pubTopic,
                           final String subTopic,
                           final String lwtTopic) {
        this.clientId    = clientId;
        this.server      = server;
        this.port        = port;
        this.timeout     = timeout;
        this.username    = username;
        this.password    = password;
        this.tokenConfig = tokenConfig;
        this.pubTopic    = pubTopic;
        this.subTopic    = subTopic;
        this.lwtTopic    = lwtTopic;
    }

    // Getter methods for each field

    public String clientId() {
        return clientId;
    }

    public String server() {
        return server;
    }

    public int port() {
        return port;
    }

    public int timeout() {
        return timeout;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String tokenConfig() {
        return tokenConfig;
    }

    public String pubTopic() {
        return pubTopic;
    }

    public String subTopic() {
        return subTopic;
    }

    public String lwtTopic() {
        return lwtTopic;
    }

    /**
     * Returns a new builder instance for constructing an MqttConnection.
     */
    public static MqttConnectionBuilder builder() {
        return new MqttConnectionBuilder();
    }

    /**
     * Builder class for constructing MqttConnection instances.
     */
    public static class MqttConnectionBuilder {
        private String clientId;
        private String server;
        private int    port;
        private int    timeout;
        private String username;
        private String password;
        private String tokenConfig;
        private String pubTopic;
        private String subTopic;
        private String lwtTopic;

        /**
         * Sets the clientId field and returns the builder instance.
         */
        public MqttConnectionBuilder clientId(final String clientId) {
            this.clientId = requireNonNull(clientId, "'clientId' cannot be null");
            return this;
        }

        /**
         * Sets the server field and returns the builder instance.
         */
        public MqttConnectionBuilder server(final String server) {
            this.server = requireNonNull(server, "'server' cannot be null");
            return this;
        }

        /**
         * Sets the port field and returns the builder instance.
         * Validates that the port is greater than zero.
         */
        public MqttConnectionBuilder port(final int port) {
            if (port <= 0) {
                throw new IllegalArgumentException("'port' cannot be less than or equal to zero");
            }
            this.port = port;
            return this;
        }

        /**
         * Sets the timeout field and returns the builder instance.
         * Validates that the timeout is not negative.
         */
        public MqttConnectionBuilder timeout(final int timeout) {
            if (timeout < -1) {
                throw new IllegalArgumentException("'timeout' cannot be negative");
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the username field and returns the builder instance.
         */
        public MqttConnectionBuilder username(final String username) {
            this.username = requireNonNull(username, "'username' cannot be null");
            return this;
        }

        /**
         * Sets the password field and returns the builder instance.
         */
        public MqttConnectionBuilder password(final String password) {
            this.password = requireNonNull(password, "'password' cannot be null");
            return this;
        }

        /**
         * Sets the tokenConfig field and returns the builder instance.
         */
        public MqttConnectionBuilder tokenConfig(final String tokenConfig) {
            this.tokenConfig = requireNonNull(tokenConfig, "'tokenConfig' cannot be null");
            return this;
        }

        /**
         * Sets the pubTopic field and returns the builder instance.
         */
        public MqttConnectionBuilder pubTopic(final String pubTopic) {
            this.pubTopic = requireNonNull(pubTopic, "'pubTopic' cannot be null");
            return this;
        }

        /**
         * Sets the subTopic field and returns the builder instance.
         */
        public MqttConnectionBuilder subTopic(final String subTopic) {
            this.subTopic = requireNonNull(subTopic, "'subTopic' cannot be null");
            return this;
        }

        /**
         * Sets the lwtTopic field and returns the builder instance.
         */
        public MqttConnectionBuilder lwtTopic(final String lwtTopic) {
            this.lwtTopic = requireNonNull(lwtTopic, "'lwtTopic' cannot be null");
            return this;
        }

        /**
         * Builds and returns an MqttConnection instance with the specified parameters.
         */
        public MqttConnection build() {
            return new MqttConnection(clientId, server, port, timeout, username, password, tokenConfig, pubTopic,
                                      subTopic, lwtTopic);
        }
    }

}
