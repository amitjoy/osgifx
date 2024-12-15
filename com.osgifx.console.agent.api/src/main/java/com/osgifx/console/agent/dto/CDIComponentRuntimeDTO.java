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

import java.util.Collection;

import org.osgi.dto.DTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ContainerTemplateDTO;

/**
 * Represents the runtime state and configuration of CDI (Contexts and Dependency 
 * Injection) components within the OSGi framework. This class extends the 
 * {@link DTO} class to provide a standardized data transfer object for CDI component 
 * runtime information.
 * <p>
 * The {@code CDIComponentRuntimeDTO} class contains collections of {@link ContainerDTO} 
 * instances, which represent the current state of CDI containers, and 
 * {@link ContainerTemplateDTO} instances, which define the template configurations 
 * for these containers. It is used to transfer CDI component runtime data between 
 * different components or systems in a consistent format.
 * </p>
 */
public class CDIComponentRuntimeDTO extends DTO {

    /** A collection of {@link ContainerDTO} instances representing the current state of CDI containers. */
    public Collection<ContainerDTO> containers;

    /** A collection of {@link ContainerTemplateDTO} instances representing the template configurations of CDI containers. */
    public Collection<ContainerTemplateDTO> containerTemplates;

}