/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * Represents a CDI activation record for a component instance.
 * This DTO contains information about the service scope, classes, and properties
 * activated within a CDI container.
 */
public class XCdiActivationDTO extends DTO {

    /**
     * The service scope (e.g., singleton, bundle, prototype) of the activation.
     */
    public String scope;

    /**
     * The fully qualified names of the service classes that are activated.
     */
    public List<String> serviceClasses;

    /**
     * The properties associated with the activated service.
     */
    public Map<String, String> properties;

    /**
     * The unique identifier (service ID) of the activated service.
     */
    public long serviceId;

    /**
     * A list of errors encountered during activation, if any.
     */
    public List<String> errors;

}
