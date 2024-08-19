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
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * Represents a configuration object within the OSGi framework. This class extends 
 * the {@link DTO} class to provide a standardized data transfer object for 
 * configuration-related information.
 * <p>
 * The {@code XConfigurationDTO} class contains details such as the PID (Persistent 
 * Identifier), factory PID, location, whether the configuration is a factory or 
 * persisted configuration, and its properties. Additionally, it includes information 
 * about the object class definition and any component reference filters that apply 
 * to the configuration. It is used to transfer configuration data between different 
 * components or systems in a consistent format.
 * </p>
 */
public class XConfigurationDTO extends DTO {

    /** The persistent identifier (PID) of the configuration. */
    public String pid;

    /** The factory persistent identifier (factory PID) if this is a factory configuration. */
    public String factoryPid;

    /** The location where the configuration is bound. */
    public String location;

    /** Indicates whether this configuration is a factory configuration. */
    public boolean isFactory;

    /** Indicates whether this configuration is persisted. */
    public boolean isPersisted;

    /** The object class definition (OCD) associated with this configuration. */
    public XObjectClassDefDTO ocd;

    /** A map of properties associated with this configuration, represented by {@link ConfigValue}. */
    public Map<String, ConfigValue> properties;

    /** A list of component reference filters associated with this configuration. */
    public List<XComponentReferenceFilterDTO> componentReferenceFilters;

}