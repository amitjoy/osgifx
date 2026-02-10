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
 * The SocketConnection class represents a socket connection configuration.
 * It encapsulates various parameters required to establish a connection
 * to a socket server.
 */
public class SocketConnection {

    // The host address of the socket server
    private final String host;
    // The port number to connect to the socket server
    private final int port;
    // The connection timeout value
    private final int timeout;
    // The trust store file path for SSL/TLS
    private final String trustStore;
    // The password for the trust store
    private final String trustStorePassword;

    /**
     * Private constructor to enforce the use of the builder for object creation.
     */
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

    // Getter methods for each field

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

    /**
     * Returns a new builder instance for constructing a SocketConnection.
     */
    public static SocketConnectionBuilder builder() {
        return new SocketConnectionBuilder();
    }

    /**
     * Builder class for constructing SocketConnection instances.
     */
    public static class SocketConnectionBuilder {

        private String host;
        private int    port;
        private int    timeout;
        private String trustStore;
        private String trustStorePassword;

        /**
         * Sets the host field and returns the builder instance.
         */
        public SocketConnectionBuilder host(final String host) {
            this.host = requireNonNull(host, "'host' cannot be null");
            return this;
        }

        /**
         * Sets the port field and returns the builder instance.
         * Validates that the port is greater than zero.
         */
        public SocketConnectionBuilder port(final int port) {
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
        public SocketConnectionBuilder timeout(final int timeout) {
            if (timeout < -1) {
                throw new IllegalArgumentException("'timeout' cannot be negative");
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the trustStore field and returns the builder instance.
         */
        public SocketConnectionBuilder truststore(final String trustStore) {
            this.trustStore = trustStore;
            return this;
        }

        /**
         * Sets the trustStorePassword field and returns the builder instance.
         */
        public SocketConnectionBuilder truststorePass(final String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
            return this;
        }

        /**
         * Builds and returns a SocketConnection instance with the specified parameters.
         */
        public SocketConnection build() {
            return new SocketConnection(host, port, timeout, trustStore, trustStorePassword);
        }
    }
}
