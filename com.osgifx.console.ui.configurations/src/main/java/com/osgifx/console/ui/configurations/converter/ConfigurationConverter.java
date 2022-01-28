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
package com.osgifx.console.ui.configurations.converter;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

@Component(service = ConfigurationConverter.class)
public final class ConfigurationConverter {

    private final Converter converter;

    @Activate
    public ConfigurationConverter() {
        converter = Converters.standardConverter();
    }

    public Object convert(final String value, final ConfigurationType target) throws Exception {
        final Class<?> targetClass = ConfigurationType.clazz(target);
        return converter.convert(value).to(targetClass);
    }

}
