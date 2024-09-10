/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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

import org.osgi.dto.DTO;

/**
 * Represents an unsatisfied service reference within the OSGi framework.
 * This class extends the {@link DTO} class to provide a standard data transfer 
 * object for information about service references that are currently unsatisfied.
 * <p>
 * The {@code XUnsatisfiedReferenceDTO} class contains details such as the name of 
 * the unsatisfied reference, the target filter expression, and the object class 
 * associated with the reference. It is used to transfer information about unsatisfied 
 * service references between different components or systems in a consistent format.
 * </p>
 */
public class XUnsatisfiedReferenceDTO extends DTO {

    /** The name of the unsatisfied service reference. */
    public String name;

    /** The target filter expression for the unsatisfied reference. */
    public String target;

    /** The fully qualified name of the object class associated with the reference. */
    public String objectClass;

}