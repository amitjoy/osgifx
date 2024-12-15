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

package com.osgifx.console.agent.dto;

/**
 * A data transfer object (DTO) representing memory information of a system.
 * It includes uptime, maximum memory, free memory, and total memory statistics.
 */
public class XMemoryInfoDTO {

    /** The uptime of the system in milliseconds */
    public long uptime;

    /** The maximum amount of memory that the JVM will attempt to use */
    public long maxMemory;

    /** The amount of free memory in the JVM */
    public long freeMemory;

    /** The total amount of memory in the JVM */
    public long totalMemory;

}
