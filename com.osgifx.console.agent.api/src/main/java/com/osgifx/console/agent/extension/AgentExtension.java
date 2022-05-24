/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.agent.extension;

import org.osgi.dto.DTO;

/**
 * Service interface to be used by consumers for providing custom
 * functionalities that can be invoked through the agent.
 * <p>
 * The service must provide the following key as a service property.
 *
 * @see AgentExtensionName
 */
public interface AgentExtension<C extends DTO, R extends DTO> {

	/** The service property key to be set */
	String PROPERTY_KEY = "agent.extension.name";

	/**
	 * Returns the result compliant with {@code OSGi DTO specification}
	 *
	 * @param context the context for the extension (also to be compliant with
	 *                {@code OSGi DTO specification})
	 * @return the result in compliance with {@code OSGi DTO specification}
	 */
	R execute(C context);

	/**
	 * The source type of the context
	 *
	 * @return {@code OSGi DTO specification} compliant DTO
	 */
	Class<C> getContextType();

	/**
	 * The source type of the context
	 *
	 * @return {@code OSGi DTO specification} compliant DTO
	 */
	Class<R> getResultType();

}
