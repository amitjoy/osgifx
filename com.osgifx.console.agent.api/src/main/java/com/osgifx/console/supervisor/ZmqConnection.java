/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
 * The ZmqConnection class represents a ZeroMQ connection configuration.
 * It encapsulates various parameters required to establish a connection
 * to a socket server.
 */
public class ZmqConnection {

    // The host address of the ZeroMQ server
    private final String host;
    // The event port number to connect to the ZeroMQ server
    private final int eventPort;
    // The command port number to connect to the ZeroMQ server
    private final int commandPort;
    // The connection timeout value
    private final int timeout;

    /**
     * Private constructor to enforce the use of the builder for object creation.
     */
    private ZmqConnection(final String host, final int commandPort, final int eventPort, final int timeout) {
        this.host        = host;
        this.eventPort   = eventPort;
        this.commandPort = commandPort;
        this.timeout     = timeout;
    }

    // Getter methods for each field

    public String host() {
        return host;
    }

    public int eventPort() {
        return eventPort;
    }

    public int commandPort() {
        return commandPort;
    }

    public int timeout() {
        return timeout;
    }

    /**
     * Returns a new builder instance for constructing a ZmqConnection.
     */
    public static ZmqConnectionBuilder builder() {
        return new ZmqConnectionBuilder();
    }

    /**
     * Builder class for constructing ZmqConnection instances.
     */
    public static class ZmqConnectionBuilder {

        private String host;
        private int    eventPort;
        private int    commandPort;
        private int    timeout;

        /**
         * Sets the host field and returns the builder instance.
         */
        public ZmqConnectionBuilder host(final String host) {
            this.host = requireNonNull(host, "'host' cannot be null");
            return this;
        }

        /**
         * Sets the command port field and returns the builder instance.
         * Validates that the port is greater than zero.
         */
        public ZmqConnectionBuilder commandPort(final int port) {
            if (port <= 0) {
                throw new IllegalArgumentException("'commandPort' cannot be less than or equal to zero");
            }
            this.commandPort = port;
            return this;
        }

        /**
         * Sets the event port field and returns the builder instance.
         * Validates that the port is greater than zero.
         */
        public ZmqConnectionBuilder eventPort(final int port) {
            if (port <= 0) {
                throw new IllegalArgumentException("'eventPort' cannot be less than or equal to zero");
            }
            this.eventPort = port;
            return this;
        }

        /**
         * Sets the timeout field and returns the builder instance.
         * Validates that the timeout is not negative.
         */
        public ZmqConnectionBuilder timeout(final int timeout) {
            if (timeout < -1) {
                throw new IllegalArgumentException("'timeout' cannot be negative");
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * Builds and returns a ZmqConnection instance with the specified parameters.
         */
        public ZmqConnection build() {
            return new ZmqConnection(host, commandPort, eventPort, timeout);
        }
    }
}
