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
package com.osgifx.console.agent.helper;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.provider.PackageWirings;

import org.osgi.framework.BundleContext;

import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;

public final class AgentHelper {

    private AgentHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    /**
     * Resolves a property by first checking System properties (to allow dynamic
     * overrides via Gogo shell commands), then falling back to the OSGi framework properties.
     * 
     * @param key the property key
     * @param bundleContext the bundle context
     * @return the property value, or null if not found
     */
    public static String getProperty(final String key, final BundleContext bundleContext) {
        final String fromSystem = System.getProperty(key);
        return fromSystem != null && !fromSystem.trim().isEmpty() ? fromSystem 
               : bundleContext != null ? bundleContext.getProperty(key) : null;
    }

    public static XResultDTO createResult(final int result, final String response) {
        final XResultDTO dto = new XResultDTO();

        dto.result   = result;
        dto.response = response;

        return dto;
    }

    public static <K, V> Map<K, V> valueOf(final Dictionary<K, V> dictionary) {
        if (dictionary == null) {
            return null;
        }
        final Map<K, V>      map  = new HashMap<>(dictionary.size());
        final Enumeration<K> keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            final K key = keys.nextElement();
            map.put(key, dictionary.get(key));
        }
        return map;
    }

    public static String serviceUnavailable(final OSGiCompendiumService service) {
        return service.comprehensibleName + " service is not available";
    }

    public static String packageNotWired(final PackageWirings.Type wirings) {
        return wirings.comprehensibleName + " bundle is not installed";
    }

    public static Object convert(final ConfigValue entry) throws Exception {
        final Object            source = entry.value;
        final XAttributeDefType type   = entry.type;
        switch (type) {
            case STRING_ARRAY:
                return Converter.cnv(String[].class, source);
            case STRING_LIST:
                return Converter.cnv(new TypeReference<List<String>>() {
                }, source);
            case INTEGER_ARRAY:
                return Converter.cnv(int[].class, source);
            case INTEGER_LIST:
                return Converter.cnv(new TypeReference<List<Integer>>() {
                }, source);
            case BOOLEAN_ARRAY:
                return Converter.cnv(boolean[].class, source);
            case BOOLEAN_LIST:
                return Converter.cnv(new TypeReference<List<Boolean>>() {
                }, source);
            case DOUBLE_ARRAY:
                return Converter.cnv(double[].class, source);
            case DOUBLE_LIST:
                return Converter.cnv(new TypeReference<List<Double>>() {
                }, source);
            case FLOAT_ARRAY:
                return Converter.cnv(float[].class, source);
            case FLOAT_LIST:
                return Converter.cnv(new TypeReference<List<Float>>() {
                }, source);
            case CHAR_ARRAY:
                return Converter.cnv(char[].class, source);
            case CHAR_LIST:
                return Converter.cnv(new TypeReference<List<Character>>() {
                }, source);
            case LONG_ARRAY:
                return Converter.cnv(long[].class, source);
            case LONG_LIST:
                return Converter.cnv(new TypeReference<List<Long>>() {
                }, source);
            default:
                return Converter.cnv(XAttributeDefType.clazz(type), source);
        }
    }

}
