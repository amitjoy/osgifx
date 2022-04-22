/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XFrameworkEventDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;

/**
 * A Supervisor handles the initiating side of a session with a remote agent.
 * The methods defined in this interface are intended to be called by the remote
 * agent, not the initiator. I.e. this is not the interface the initiator will
 * use to control the session.
 */
public interface Supervisor {

	/** The topic where an event will be sent after the agent gets connected */
	String AGENT_CONNECTED_EVENT_TOPIC = "fx/console/agent/connected";

	/** The topic where an event will be sent after the agent gets disconnected */
	String AGENT_DISCONNECTED_EVENT_TOPIC = "fx/console/agent/disconnected";

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
	 * Return the contents of the file that has the given SHA-1. The initiator of
	 * the connection should in general register the files it refers to in the
	 * communication to the agent. The agent then calls this method to retrieve the
	 * contents if it does not have it in its local cache.
	 *
	 * @param sha the SHA-1
	 * @return the contents of that file or null if no such file exists.
	 */
	byte[] getFile(String sha) throws Exception;

	/**
	 * Connects to the specific host and port using the provided timeout in
	 * connection
	 *
	 * @param host    the host name
	 * @param port    the port address
	 * @param timeout the timeout in milliseconds
	 *
	 * @throws Exception if any issue occurs during connection
	 */
	void connect(String host, int port, int timeout) throws Exception;

	/**
	 * Callback method for OSGi Event Admin Events
	 */
	void onOSGiEvent(XEventDTO event);

	/**
	 * Callback method for OSGi Framework Events
	 */
	void onFrameworkEvent(XFrameworkEventDTO event);

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
	 * Registers the specified listener to listen to the OSGi framework events from
	 * the remote machine
	 *
	 * @param eventListener the event listener to register
	 */
	void addOSGiFrameworkEventListener(FrameworkEventListener eventListener);

	/**
	 * Deregisters previously registered listener
	 *
	 * @param eventListener the listener to deregister
	 */
	void removeOSGiFrameworkEventListener(FrameworkEventListener eventListener);

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
