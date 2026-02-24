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

import org.osgi.dto.DTO;

/**
 * Represents a CDI container within the OSGi framework.
 * This DTO encapsulates the main attributes and state of a CDI container.
 */
public class XCdiContainerDTO extends DTO {

    /**
     * The unique identifier of the CDI container.
     */
    public String id;

    /**
     * The ID of the bundle associated with this CDI container.
     */
    public long bundleId;

    /**
     * The number of times the state of the CDI container has changed.
     */
    public long changeCount;

    /**
     * A list of errors encountered by the CDI container, if any.
     */
    public List<String> errors;

    /**
     * The list of CDI extensions available within this container.
     */
    public List<XCdiExtensionDTO> extensions;

    /**
     * The list of CDI components managed by this container.
     */
    public List<XCdiComponentDTO> components;

}
