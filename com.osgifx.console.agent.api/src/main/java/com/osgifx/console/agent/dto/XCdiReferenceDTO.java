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
 * Represents a CDI reference within a component instance.
 * This DTO holds information regarding service references and target filters.
 */
public class XCdiReferenceDTO extends DTO {

    /**
     * The name of the reference.
     */
    public String name;

    /**
     * The service type interfaces that the reference targets.
     */
    public String serviceType;

    /**
     * The filter used to select the target services for this reference.
     */
    public String targetFilter;

    /**
     * The minimum cardinality required for this reference to be satisfied.
     */
    public int minimumCardinality;

    /**
     * The maximum cardinality allowed for this reference.
     */
    public String maximumCardinality;

    /**
     * The reference policy, such as static or dynamic.
     */
    public String policy;

    /**
     * The policy option, such as reluctant or greedy.
     */
    public String policyOption;

    /**
     * A list of service IDs that match this reference.
     */
    public List<Long> matches;

}
