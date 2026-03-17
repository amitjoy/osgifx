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
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * Represents the details and runtime state of an OSGi Condition.
 */
public class XConditionDTO extends DTO {

    /** The identifier of the condition (e.g. the value of 'osgi.condition.id' property) */
    public String identifier;

    /** The current state of the condition */
    public XConditionState state;

    /** The identifier of the bundle providing this condition service, if active */
    public long providerBundleId;

    /** The properties associated with the condition service */
    public Map<String, String> properties;

    /** The names of the components satisfied by this condition */
    public List<String> satisfiedComponents;

    /** The names of the components currently blocked/unsatisfied by this condition */
    public List<String> unsatisfiedComponents;

}
