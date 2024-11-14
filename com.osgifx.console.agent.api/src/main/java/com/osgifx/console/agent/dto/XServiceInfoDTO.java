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

import org.osgi.dto.DTO;

/**
 * Represents information about a service in the OSGi framework. This class extends 
 * the {@link DTO} class to provide a standard data transfer object for service-related 
 * information.
 * <p>
 * The {@code XServiceInfoDTO} class holds details such as the unique identifier of the 
 * service and its object class, which can be used for transferring service data between 
 * different components or systems in a consistent format.
 * </p>
 */
public class XServiceInfoDTO extends DTO {

    /** The unique identifier of the service. */
    public long id;

    /** The fully qualified name of the service's object class. */
    public String objectClass;

}