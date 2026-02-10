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

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * Represents a role within the OSGi framework, which can be a user, group, or
 * default role. This class extends the {@link DTO} class to provide a standard
 * data transfer object for roles and their associated properties.
 * <p>
 * The {@code XRoleDTO} class holds information about the role type, name, properties,
 * credentials, and members (both basic and required). It is used for transferring
 * role data between different components or systems in a consistent format.
 * </p>
 */
public class XRoleDTO extends DTO {

    /**
     * Enumeration of the possible types of roles.
     */
    public enum Type {
        /** A role representing an individual user. */
        USER,
        /** A role representing a group of users. */
        GROUP,
        /** A default role used when no specific role is assigned. */
        DEFAULT
    }

    /** The type of the role, represented by the {@link Type} enum. */
    public Type type;

    /** The name of the role. */
    public String name;

    /** A map containing the properties associated with the role. */
    public Map<String, Object> properties;

    /** A map containing the credentials associated with the role. */
    public Map<String, Object> credentials;

    /** A list of basic member roles that belong to this role. */
    public List<XRoleDTO> basicMembers;

    /** A list of required member roles necessary for this role. */
    public List<XRoleDTO> requiredMembers;

}