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
package com.osgifx.console.agent.rpc;

import java.io.IOException;

/**
 * This is used to provide a remote RPC mechanism over the underlying connection,
 * for example, socket or MQTT.
 *
 * @param <L> the local agent
 * @param <R> the remote agent
 */
public interface RemoteRPC<L, R> {

    /**
     * Open the remote RPC communication link
     */
    void open();

    /**
     * Closes the remote RPC communication link
     *
     * @throws IOException if any issues occurs while closing
     */
    void close() throws IOException;

    /**
     * Returns the remote agent proxy
     *
     * @return the remote agent proxy
     */
    R getRemote();

    /**
     * Checks if the RPC communication link is already open
     *
     * @return {@code true} if the connection link is already open, otherwise {@code false}
     */
    boolean isOpen();

}