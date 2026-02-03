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

import java.util.Collection;

import org.osgi.dto.DTO;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

/**
 * Represents the runtime details of service components within the OSGi framework.
 * This class extends the {@link DTO} class to provide a standardized data transfer 
 * object for information related to the runtime state and configuration of service 
 * components.
 * <p>
 * The {@code ServiceComponentRuntimeDTO} class contains collections of 
 * {@link ComponentDescriptionDTO} instances, which describe the service components, 
 * and {@link ComponentConfigurationDTO} instances, which provide details about 
 * the current configurations of these components. It is used to transfer service 
 * component runtime data between different components or systems in a consistent format.
 * </p>
 */
public class ServiceComponentRuntimeDTO extends DTO {

    /** A collection of {@link ComponentDescriptionDTO} instances representing the service component descriptions. */
    public Collection<ComponentDescriptionDTO> componentDescriptionDTOs;

    /** A collection of {@link ComponentConfigurationDTO} instances representing the current configurations of service components. */
    public Collection<ComponentConfigurationDTO> componentConfigurationDTOs;

}