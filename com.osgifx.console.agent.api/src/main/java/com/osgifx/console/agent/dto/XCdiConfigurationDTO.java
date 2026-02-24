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

import java.util.Map;

import org.osgi.dto.DTO;

/**
 * Represents a CDI configuration associated with a component instance.
 * This DTO encapsulates the PID, policy, and specific configuration properties.
 */
public class XCdiConfigurationDTO extends DTO {

    /**
     * The persistent identifier (PID) of the configuration.
     */
    public String pid;

    /**
     * The policy applied to this configuration.
     */
    public String policy;

    /**
     * The maximum cardinality allowed for this configuration.
     */
    public String maximumCardinality;

    /**
     * The actual properties defined within this configuration.
     */
    public Map<String, String> properties;

}
