/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
 * Represents an object class definition (OCD) within the OSGi framework.
 * This class extends the {@link DTO} class to provide a standardized data transfer 
 * object for information related to object class definitions, which are used for 
 * configuration metadata.
 * <p>
 * The {@code XObjectClassDefDTO} class contains details such as the ID, PID, factory 
 * PID, name, description, descriptor location, and associated attribute definitions. 
 * It is used to transfer object class definition data between different components 
 * or systems in a consistent format.
 * </p>
 */
public class XObjectClassDefDTO extends DTO {

    // Identification Information
    /** The unique identifier of the object class definition. */
    public String id;

    /** The persistent identifier (PID) associated with this object class definition. */
    public String pid;

    /** The factory persistent identifier (factory PID), if this is a factory object class definition. */
    public String factoryPid;

    // Descriptive Information
    /** The name of the object class definition. */
    public String name;

    /** A description of the object class definition. */
    public String description;

    /** The location of the descriptor associated with this object class definition. */
    public String descriptorLocation;

    // Attribute Definitions
    /** A list of attribute definitions associated with this object class definition. */
    public List<XAttributeDefDTO> attributeDefs;

}