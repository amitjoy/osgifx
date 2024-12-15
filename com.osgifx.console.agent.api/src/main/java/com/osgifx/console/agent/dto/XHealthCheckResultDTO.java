/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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

import java.util.List;

import org.osgi.dto.DTO;

/**
 * Represents the result of a health check execution within the OSGi framework.
 * This class extends the {@link DTO} class to provide a standardized data 
 * transfer object for health check result-related information.
 * <p>
 * The {@code XHealthCheckResultDTO} class includes details such as the name and 
 * tags of the health check, a list of individual results, the time taken to 
 * execute the health check, the timestamp when it finished, and whether the 
 * health check execution timed out. It is used to transfer health check results 
 * between different components or systems in a consistent format.
 * </p>
 */
public class XHealthCheckResultDTO extends DTO {

    /**
     * Represents an individual result entry for a health check execution.
     * This class extends the {@link DTO} class to provide structured information 
     * about the outcome of a health check.
     */
    public static class ResultDTO extends DTO {

        /** The status of the health check result (e.g., "OK", "WARN", "CRITICAL"). */
        public String status;

        /** The message associated with the health check result, providing details about the status. */
        public String message;

        /** The log level associated with the health check result (e.g., "INFO", "ERROR"). */
        public String logLevel;

        /** The exception message, if any, associated with the health check result. */
        public String exception;
    }

    /** The name of the health check that was executed. */
    public String healthCheckName;

    /** A list of tags associated with the health check. */
    public List<String> healthCheckTags;

    /** A list of individual results from the health check execution. */
    public List<ResultDTO> results;

    /** The elapsed time in milliseconds for the health check execution. */
    public long elapsedTime;

    /** The timestamp in milliseconds when the health check execution finished. */
    public long finishedAt;

    /** Indicates whether the health check execution timed out. */
    public boolean isTimedOut;

}