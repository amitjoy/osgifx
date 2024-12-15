/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
package com.osgifx.console.util.agent;

import java.util.Map;

import org.eclipse.fx.core.ExceptionUtils;
import org.osgi.dto.DTO;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TypeReference;

import com.osgifx.console.agent.Agent;

public final class ExtensionHelper {

    private ExtensionHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static <R extends DTO> R executeExtension(final Agent agent,
                                                     final String name,
                                                     final DTO context,
                                                     final Class<R> resultType) {
        try {
            final var                 converter        = Converters.standardConverter();
            final Map<String, Object> properties       = converter.convert(context)
                    .to(new TypeReference<Map<String, Object>>() {
                                                               });
            final var                 executeExtension = agent.executeExtension(name, properties);
            return converter.convert(executeExtension).to(resultType);
        } catch (final Exception e) {
            throw ExceptionUtils.wrap(e);
        }
    }

}
