/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.dto;

import org.osgi.dto.DTO;

/**
 * Represents the state and attributes of a thread within the OSGi framework.
 * This class extends the {@link DTO} class to provide a standard data transfer
 * object for thread-related information.
 * <p>
 * The {@code XThreadDTO} class holds various details about a thread, such as its
 * name, identifier, priority, state, and whether it is interrupted, alive, daemon,
 * or deadlocked. It is used for transferring thread data between different components
 * or systems in a consistent format.
 * </p>
 */
public class XThreadDTO extends DTO {

    /** The name of the thread. */
    public String name;

    /** The unique identifier of the thread. */
    public long id;

    /** The priority of the thread. */
    public int priority;

    /** The current state of the thread, represented as a string. */
    public String state;

    /** Indicates whether the thread is currently interrupted. */
    public boolean isInterrupted;

    /** Indicates whether the thread is currently alive. */
    public boolean isAlive;

    /** Indicates whether the thread is a daemon thread. */
    public boolean isDaemon;

    /** Indicates whether the thread is in a deadlocked state. */
    public boolean isDeadlocked;

}