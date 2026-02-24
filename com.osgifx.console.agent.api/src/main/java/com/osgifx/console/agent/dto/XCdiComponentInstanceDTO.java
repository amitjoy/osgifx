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
 * Represents a specific instance of a CDI component.
 * This DTO contains the runtime properties, activations, and configurations of the instance.
 */
public class XCdiComponentInstanceDTO extends DTO {

    /**
     * The runtime properties associated with this component instance.
     */
    public Map<String, String> properties;

    /**
     * The configurations applied to this component instance.
     */
    public List<XCdiConfigurationDTO> configurations;

    /**
     * The references managed by this component instance.
     */
    public List<XCdiReferenceDTO> references;

    /**
     * The activation records for this component instance.
     */
    public List<XCdiActivationDTO> activations;

}
