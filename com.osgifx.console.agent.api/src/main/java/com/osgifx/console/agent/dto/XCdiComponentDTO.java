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
 * Represents a CDI component managed by a CDI container.
 * This DTO contains details about a specific component, its state,
 * and its associated instances.
 */
public class XCdiComponentDTO extends DTO {

    /**
     * The name of the CDI component.
     */
    public String name;

    /**
     * The type of the CDI component, such as Application, Extension, or Resource.
     */
    public String type;

    /**
     * Indicates whether the CDI component is currently enabled.
     */
    public boolean enabled;

    /**
     * A list of beans provided by this component.
     */
    public List<String> beans;

    /**
     * The properties associated with the CDI component.
     */
    public Map<String, String> properties;

    /**
     * The list of active instances of this component.
     */
    public List<XCdiComponentInstanceDTO> instances;

}
