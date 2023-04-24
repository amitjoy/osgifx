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

import org.osgi.annotation.versioning.ProviderType;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;

/**
 * A Supervisor handles the initiating side of a session with a remote agent.
 * The methods defined in this interface are intended to be called by the remote
 * agent, not the initiator. I.e. this is not the interface the initiator will
 * use to control the session.
 */
@ProviderType
public interface Supervisor {

    public enum RpcType {
        MQTT_RPC,
        SOCKET_RPC
    }

    /** The topic where an event will be sent after the agent gets connected */
    String AGENT_CONNECTED_EVENT_TOPIC = "fx/console/agent/connected";

    /** The topic where an event will be sent after the agent gets disconnected */
    String AGENT_DISCONNECTED_EVENT_TOPIC = "fx/console/agent/disconnected";

    /** The topic where an event will be sent when the log listener is added */
    String LOG_LISTENER_ADDED_EVENT_TOPIC = "fx/console/supervisor/log/listener/added";

    /** The topic where an event will be sent when the log listener is removed */
    String LOG_LISTENER_REMOVED_EVENT_TOPIC = "fx/console/supervisor/log/listener/removed";

    /** The topic where an event will be sent when the event listener is added */
    String EVENT_LISTENER_ADDED_EVENT_TOPIC = "fx/console/supervisor/event/listener/added";

    /** The topic where an event will be sent when the event listener is removed */
    String EVENT_LISTENER_REMOVED_EVENT_TOPIC = "fx/console/supervisor/event/listener/removed";

    /**
     * Returns the type of the communication
     */
    RpcType getType();

    /**
     * Connects to the provided socket using the specified options
     *
     * @param socketConnection the socket connection (cannot be {@code null})
     * @throws Exception if any issue occurs during connection
     */
    void connect(SocketConnection socketConnection) throws Exception;

    /**
     * Connects to the provided MQTT broker using the specified options
     *
     * @param mqttConnection the MQTT connection (cannot be {@code null})
     * @throws Exception if any issue occurs during connection
     */
    void connect(MqttConnection mqttConnection) throws Exception;

    /**
     * Disconnects the remote agent
     */
    void disconnect() throws Exception;

    /**
     * Redirected standard output
     *
     * @param out the text that was redirected
     * @return ignored (to make sync)
     */
    boolean stdout(String out) throws Exception;

    /**
     * Redirected standard error.
     *
     * @param out the text that was redirected
     * @return ignored (to make sync)
     */
    boolean stderr(String out) throws Exception;

    /**
     * Callback method for OSGi Event Admin Events
     */
    void onOSGiEvent(XEventDTO event);

    /**
     * Callback method for logs
     */
    void logged(XLogEntryDTO event);

    /**
     * Registers the specified listener to listen to the OSGi events from the remote
     * machine
     *
     * @param eventListener the event listener to register
     */
    void addOSGiEventListener(EventListener eventListener);

    /**
     * Deregisters previously registered listener
     *
     * @param eventListener the listener to deregister
     */
    void removeOSGiEventListener(EventListener eventListener);

    /**
     * Registers the specified listener to receive to the OSGi logs from the remote
     * machine
     *
     * @param logEntryListener the log entry listener to register
     */
    void addOSGiLogListener(LogEntryListener logEntryListener);

    /**
     * Deregisters previously registered log entry listener
     *
     * @param logEntryListener the log consumer to deregister
     */
    void removeOSGiLogListener(LogEntryListener logEntryListener);

    /**
     * Returns the associated agent
     *
     * @return the agent
     */
    Agent getAgent();
}
