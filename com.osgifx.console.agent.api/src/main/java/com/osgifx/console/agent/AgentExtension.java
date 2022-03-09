/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.agent;

import java.util.Map;

/**
 * Service interface to be used by consumers for providing custom
 * functionalities that can be invoked through the bnd agent.
 * <p>
 * The services must provide the following key as a service property.
 */
@FunctionalInterface
public interface AgentExtension {

    /** The service property key to be set */
    String PROPERTY_KEY = "agent.extension.name";

    /**
     * Returns the results as a type supported by the bnd converter
     *
     * @param context the context for the extension
     * @return the result as a bnd converter supported type
     */
    Object execute(Map<String, Object> context);
}
