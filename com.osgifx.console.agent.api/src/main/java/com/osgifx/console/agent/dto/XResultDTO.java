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
 * Represents a result within the OSGi framework. This class extends the {@link DTO} 
 * class to provide a standardized data transfer object for result-related information.
 * <p>
 * The {@code XResultDTO} class includes details such as the result status and a 
 * response message. It is used to transfer result data between different components 
 * or systems in a consistent format.
 * </p>
 */
public class XResultDTO extends DTO {

    /** Constant representing a successful result. */
    public static final int SUCCESS = 1;

    /** Constant representing an error result. */
    public static final int ERROR = 2;

    /** Constant representing a skipped result. */
    public static final int SKIPPED = 3;

    /** The result status (e.g., SUCCESS, ERROR, SKIPPED). */
    public int result;

    /** The response message associated with the result. */
    public String response;

}