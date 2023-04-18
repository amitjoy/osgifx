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

public class SocketConnection {

    private final String host;
    private final int    port;
    private final int    timeout;
    private final String trustStore;
    private final String trustStorePassword;

    private SocketConnection(final String host,
                             final int port,
                             final int timeout,
                             final String trustStore,
                             final String trustStorePassword) {
        this.host               = host;
        this.port               = port;
        this.timeout            = timeout;
        this.trustStore         = trustStore;
        this.trustStorePassword = trustStorePassword;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public int timeout() {
        return timeout;
    }

    public String trustStore() {
        return trustStore;
    }

    public String trustStorePassword() {
        return trustStorePassword;
    }

    public static SocketConnectionBuilder builder() {
        return new SocketConnectionBuilder();
    }

    public static class SocketConnectionBuilder {

        private String host;
        private int    port;
        private int    timeout;
        private String trustStore;
        private String trustStorePassword;

        public SocketConnectionBuilder host(final String host) {
            this.host = requireNonNull(host, "'host' cannot be null");
            return this;
        }

        public SocketConnectionBuilder port(final int port) {
            if (port <= 0) {
                throw new IllegalArgumentException("'port' cannot be less than or equal to zero");
            }
            this.port = port;
            return this;
        }

        public SocketConnectionBuilder timeout(final int timeout) {
            if (timeout < -1) {
                throw new IllegalArgumentException("'timeout' cannot be negative");
            }
            this.timeout = timeout;
            return this;
        }

        public SocketConnectionBuilder truststore(final String trustStore) {
            this.trustStore = trustStore;
            return this;
        }

        public SocketConnectionBuilder truststorePass(final String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
            return this;
        }

        public SocketConnection build() {
            return new SocketConnection(host, port, timeout, trustStore, trustStorePassword);
        }

    }

}
