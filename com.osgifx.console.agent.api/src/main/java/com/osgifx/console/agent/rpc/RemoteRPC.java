/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.rpc;

import java.io.IOException;

/**
 * Defines a remote RPC mechanism over an underlying connection, such as a socket or MQTT.
 *
 * @param <L> the local agent type
 * @param <R> the remote agent type
 */
public interface RemoteRPC<L, R> {

    /**
     * Opens the remote RPC communication link.
     */
    void open();

    /**
     * Closes the remote RPC communication link.
     *
     * @throws IOException if an issue occurs while closing the connection
     */
    void close() throws IOException;

    /**
     * Retrieves the remote agent proxy.
     *
     * @return the remote agent proxy
     */
    R getRemote();

    /**
     * Checks if the RPC communication link is open.
     *
     * @return {@code true} if the communication link is open, otherwise {@code false}
     */
    boolean isOpen();

}
