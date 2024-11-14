/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
import org.osgi.framework.dto.FrameworkDTO;

/**
 * Represents the comprehensive runtime information of various components and 
 * services within the OSGi framework. This class extends the {@link DTO} class 
 * to provide a standardized data transfer object for aggregating runtime details 
 * of the OSGi environment.
 * <p>
 * The {@code RuntimeDTO} class includes references to runtime details of several 
 * key components, such as the framework itself ({@link FrameworkDTO}), service 
 * components, JAX-RS services, HTTP services, and CDI components. It is used to 
 * transfer an aggregated view of the OSGi runtime state between different components 
 * or systems in a consistent format.
 * </p>
 */
public class RuntimeDTO extends DTO {

    /** The runtime details of the OSGi framework, represented by {@link FrameworkDTO}. */
    public FrameworkDTO framework;

    /** The runtime details of the Service Component Runtime (SCR). */
    public ServiceComponentRuntimeDTO scr;

    /** The runtime details of the JAX-RS service. */
    public JaxRsServiceRuntimeDTO jaxrs;

    /** The runtime details of the HTTP service. */
    public HttpServiceRuntimeDTO http;

    /** The runtime details of the CDI (Contexts and Dependency Injection) components. */
    public CDIComponentRuntimeDTO cdi;

}