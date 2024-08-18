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

import org.osgi.dto.DTO;

/**
 * Represents a filter for a component reference within the OSGi framework.
 * This class extends the {@link DTO} class to provide a standardized data transfer 
 * object for defining filters that are applied to component references.
 * <p>
 * The {@code XComponentReferenceFilterDTO} class contains information such as 
 * the component name, the target key for filtering, and the filter expression 
 * to be applied. It is used to specify criteria for selecting or matching 
 * references in the OSGi service registry.
 * </p>
 */
public class XComponentReferenceFilterDTO extends DTO {

    /** The name of the component to which this filter applies. */
    public String componentName;

    /** The key used to target a specific reference for filtering. */
    public String targetKey;

    /** The LDAP-style filter expression used to filter the component reference. */
    public String targetFilter;

}