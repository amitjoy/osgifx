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
package com.osgifx.console.agent.admin;

import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.FrameworkDTO;

import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XPropertyDTO.XPropertyType;

public class XPropertyAdmin {

	private XPropertyAdmin() {
		throw new IllegalAccessError("Cannot be instantiated");
	}

	public static List<XPropertyDTO> get(final BundleContext context) {
		try {
			final FrameworkDTO dto = context.getBundle(SYSTEM_BUNDLE_ID).adapt(FrameworkDTO.class);
			return prepareProperties(dto.properties);
		} catch (final Exception e) {
			return Collections.emptyList();
		}
	}

	private static List<XPropertyDTO> prepareProperties(final Map<String, Object> properties) {
		final Map<String, XPropertyDTO> allProperties = new HashMap<>();

		for (final Entry<String, Object> property : properties.entrySet()) {
			final String       key   = property.getKey();
			final String       value = property.getValue().toString();
			final XPropertyDTO dto   = createPropertyDTO(key, value, XPropertyType.FRAMEWORK);
			allProperties.put(key, dto);
		}

		@SuppressWarnings("rawtypes")
		final Map                        systemProperties = System.getProperties();
		@SuppressWarnings("unchecked")
		final Set<Entry<String, String>> sets             = ((Map<String, String>) systemProperties).entrySet();
		for (final Entry<String, String> property : sets) {
			final String       key   = property.getKey();
			final String       value = property.getValue();
			final XPropertyDTO dto   = createPropertyDTO(key, value, XPropertyType.SYSTEM);
			allProperties.put(key, dto);
		}
		return allProperties.values().stream().collect(Collectors.toList());
	}

	private static XPropertyDTO createPropertyDTO(final String name, final String value, final XPropertyType type) {
		final XPropertyDTO dto = new XPropertyDTO();
		dto.name  = name;
		dto.value = value;
		dto.type  = type;
		return dto;
	}

}
