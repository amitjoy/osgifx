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

import org.osgi.dto.DTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

/**
 * Represents a satisfied reference within the OSGi framework. This class extends 
 * the {@link DTO} class to provide a standardized data transfer object for satisfied 
 * service reference-related information.
 * <p>
 * The {@code XSatisfiedReferenceDTO} class includes details such as the reference 
 * name, target, object class, and an array of associated service references. It 
 * is used to transfer information about satisfied references between different 
 * components or systems in a consistent format.
 * </p>
 */
public class XSatisfiedReferenceDTO extends DTO {

    /** The name of the satisfied reference. */
    public String name;

    /** The target filter expression for the satisfied reference. */
    public String target;

    /** The fully qualified name of the object class associated with the satisfied reference. */
    public String objectClass;

    /** An array of service references that satisfy this reference. */
    public ServiceReferenceDTO[] serviceReferences;

}