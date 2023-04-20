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
package com.osgifx.console.supervisor;

import static java.util.Objects.requireNonNull;

public class MqttConnection {

    private final String clientId;
    private final String server;
    private final int    port;
    private final int    timeout;
    private final String username;
    private final String password;
    private final String tokenConfig;
    private final String pubTopic;
    private final String subTopic;
    private final String lwtTopic;

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

    public static MqttConnectionBuilder builder() {
        return new MqttConnectionBuilder();
    }

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

        public MqttConnectionBuilder clientId(final String clientId) {
            this.clientId = requireNonNull(clientId, "'clientId' cannot be null");
            return this;
        }

        public MqttConnectionBuilder server(final String server) {
            this.server = requireNonNull(server, "'server' cannot be null");
            return this;
        }

        public MqttConnectionBuilder port(final int port) {
            if (port <= 0) {
                throw new IllegalArgumentException("'port' cannot be less than or equal to zero");
            }
            this.port = port;
            return this;
        }

        public MqttConnectionBuilder timeout(final int timeout) {
            if (timeout < -1) {
                throw new IllegalArgumentException("'timeout' cannot be negative");
            }
            this.timeout = timeout;
            return this;
        }

        public MqttConnectionBuilder username(final String username) {
            this.username = requireNonNull(username, "'username' cannot be null");
            return this;
        }

        public MqttConnectionBuilder password(final String password) {
            this.password = requireNonNull(password, "'password' cannot be null");
            return this;
        }

        public MqttConnectionBuilder tokenConfig(final String tokenConfig) {
            this.tokenConfig = requireNonNull(tokenConfig, "'tokenConfig' cannot be null");
            return this;
        }

        public MqttConnectionBuilder pubTopic(final String pubTopic) {
            this.pubTopic = requireNonNull(pubTopic, "'pubTopic' cannot be null");
            return this;
        }

        public MqttConnectionBuilder subTopic(final String subTopic) {
            this.subTopic = requireNonNull(subTopic, "'subTopic' cannot be null");
            return this;
        }

        public MqttConnectionBuilder lwtTopic(final String lwtTopic) {
            this.lwtTopic = requireNonNull(lwtTopic, "'lwtTopic' cannot be null");
            return this;
        }

        public MqttConnection build() {
            return new MqttConnection(clientId, server, port, timeout, username, password, tokenConfig, pubTopic,
                                      subTopic, lwtTopic);
        }
    }

}
