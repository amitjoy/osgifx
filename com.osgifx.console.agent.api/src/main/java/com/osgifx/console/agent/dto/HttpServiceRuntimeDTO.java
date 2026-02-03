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
import org.osgi.service.http.runtime.dto.RuntimeDTO;

/**
 * Represents the runtime status and configuration of an HTTP service within 
 * the OSGi framework. This class extends the {@link DTO} class to provide a 
 * standardized data transfer object for HTTP service runtime information.
 * <p>
 * The {@code HttpServiceRuntimeDTO} class contains a reference to a {@link RuntimeDTO} 
 * instance, which encapsulates details about the runtime environment of the HTTP 
 * service. It is used to transfer HTTP service runtime data between different 
 * components or systems in a consistent format.
 * </p>
 */
public class HttpServiceRuntimeDTO extends DTO {

    /** The runtime details of the HTTP service, represented by {@link RuntimeDTO}. */
    public RuntimeDTO runtime;

}