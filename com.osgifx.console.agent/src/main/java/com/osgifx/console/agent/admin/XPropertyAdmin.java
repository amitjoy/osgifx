/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.agent.admin;

import static java.util.stream.Collectors.toList;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.FrameworkDTO;

import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XPropertyDTO.XPropertyType;

import jakarta.inject.Inject;

public final class XPropertyAdmin {

    private final BundleContext context;

    @Inject
    public XPropertyAdmin(final BundleContext context) {
        this.context = context;
    }

    public List<XPropertyDTO> get() {
        try {
            final FrameworkDTO dto = context.getBundle(SYSTEM_BUNDLE_ID).adapt(FrameworkDTO.class);
            return prepareProperties(dto.properties);
        } catch (final Exception e) {
            return Collections.emptyList();
        }
    }

    private List<XPropertyDTO> prepareProperties(final Map<String, Object> properties) {
        final Map<String, XPropertyDTO> allProperties = new HashMap<>();
        for (final Entry<String, Object> property : properties.entrySet()) {
            final String       key   = property.getKey();
            final String       value = property.getValue().toString();
            final XPropertyDTO dto   = createPropertyDTO(key, value, XPropertyType.FRAMEWORK);
            allProperties.put(key, dto);
        }
        final Properties     systemProperties = System.getProperties();
        final Enumeration<?> keys             = systemProperties.propertyNames();
        while (keys.hasMoreElements()) {
            final String       key   = keys.nextElement().toString();
            final String       value = systemProperties.getProperty(key);
            final XPropertyDTO dto   = createPropertyDTO(key, value, XPropertyType.SYSTEM);
            allProperties.put(key, dto);
        }
        return allProperties.values().stream().collect(toList());
    }

    private XPropertyDTO createPropertyDTO(final String name, final String value, final XPropertyType type) {
        final XPropertyDTO dto = new XPropertyDTO();

        dto.name  = name;
        dto.value = value;
        dto.type  = type;

        return dto;
    }

}
