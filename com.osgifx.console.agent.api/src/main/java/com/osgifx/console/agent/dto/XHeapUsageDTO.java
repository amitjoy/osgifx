/*******************************************************************************
 * COPYRIGHT 2021-2025 AMIT KUMAR MONDAL
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
 * Data Transfer Object (DTO) representing heap usage information.
 */
public class XHeapUsageDTO extends DTO {

    /** Uptime of the Java virtual machine. */
    public long uptime;

    /** Memory usage details. */
    public XMemoryUsage memoryUsage;

    /** Memory pool management beans. */
    public XMemoryPoolMXBean[] memoryPoolBeans;

    /** Garbage collector beans. */
    public XGarbageCollectorMXBean[] gcBeans;

    /**
     * Inner class representing memory usage details.
     */
    public static class XMemoryUsage extends DTO {
        /** Used memory. */
        public long used;

        /** Maximum available memory. */
        public long max;
    }

    /**
     * Inner class representing memory pool management beans.
     */
    public static class XMemoryPoolMXBean extends DTO {
        /** Type of memory pool. */
        public String type;

        /** Name of memory pool. */
        public String name;

        /** Memory usage details for the pool. */
        public XMemoryUsage memoryUsage;
    }

    /**
     * Inner class representing garbage collector beans.
     */
    public static class XGarbageCollectorMXBean extends DTO {
        /** Name of the garbage collector. */
        public String name;

        /** Number of collections performed by the garbage collector. */
        public long collectionCount;

        /** Total time spent in garbage collection in milliseconds. */
        public long collectionTime;
    }

}
