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
package com.osgifx.console.agent.dto;

import org.osgi.dto.DTO;

public class XHeapUsageDTO extends DTO {

    public long                      uptime;
    public XMemoryUsage              memoryUsage;
    public XMemoryPoolMXBean[]       memoryPoolBeans;
    public XGarbageCollectorMXBean[] gcBeans;

    public static class XMemoryUsage extends DTO {
        public long used;
        public long max;
    }

    public static class XMemoryPoolMXBean extends DTO {
        public String       type;
        public String       name;
        public XMemoryUsage memoryUsage;
    }

    public static class XGarbageCollectorMXBean extends DTO {
        public String name;
        public long   collectionCount;
        public long   collectionTime;
    }

}
