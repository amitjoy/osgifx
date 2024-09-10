/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
 * Represents the details of a health check service within the OSGi framework.
 * This class extends the {@link DTO} class to provide a standardized data 
 * transfer object for health check-related information.
 * <p>
 * The {@code XHealthCheckDTO} class includes information such as the service ID, 
 * name, MBean name, scheduling details like cron expression or interval, result 
 * time-to-live (TTL), sticky duration for non-OK results, and associated tags. 
 * It is used to transfer health check data between different components or systems 
 * in a consistent format.
 * </p>
 */
public class XHealthCheckDTO extends DTO {

    /** The unique service ID of the health check. */
    public long serviceID;

    /** The name of the health check. */
    public String name;

    /** The MBean name associated with this health check. */
    public String mbeanName;

    /** The cron expression defining the schedule for this health check. */
    public String cronExpression;

    /** The interval in milliseconds between executions of this health check. */
    public Long interval;

    /** The time-to-live (TTL) in milliseconds for the health check results. */
    public Long resultTTL;

    /** The duration in milliseconds to keep non-OK results sticky. */
    public Long keepNonOkResultsSticky;

    /** A list of tags associated with this health check. */
    public List<String> tags;

}