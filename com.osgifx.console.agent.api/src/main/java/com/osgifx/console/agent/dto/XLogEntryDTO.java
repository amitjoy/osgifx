/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.dto;

import org.osgi.dto.DTO;

/**
 * Represents a log entry within the OSGi framework. This class extends the {@link DTO}
 * class to provide a standardized data transfer object for log entry-related information.
 * <p>
 * The {@code XLogEntryDTO} class includes details such as the bundle that generated
 * the log entry, the log level, the message, any associated exception, the timestamp
 * of when the log was recorded, and additional information about the thread and logger
 * involved.
 * </p>
 */
public class XLogEntryDTO extends DTO {

    // Bundle Information
    /** The bundle that generated the log entry. */
    public XBundleDTO bundle;

    // Log Details
    /** The log level of the entry (e.g., "DEBUG", "INFO", "WARN", "ERROR"). */
    public String level;

    /** The message of the log entry. */
    public String message;

    /** The exception message, if any, associated with the log entry. */
    public String exception;

    // Log Context Information
    /** The timestamp in milliseconds when the log entry was recorded. */
    public long loggedAt;

    /** Information about the thread that generated the log entry. */
    public String threadInfo;

    /** The name of the logger that created the log entry. */
    public String logger;

}