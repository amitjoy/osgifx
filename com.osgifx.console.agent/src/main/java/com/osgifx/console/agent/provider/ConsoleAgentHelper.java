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
package com.osgifx.console.agent.provider;

import static java.util.function.Function.identity;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public final class ConsoleAgentHelper {

    private ConsoleAgentHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static String bsn(final long id, final BundleContext context) {
        for (final Bundle b : context.getBundles()) {
            if (b.getBundleId() == id) {
                return b.getSymbolicName();
            }
        }
        return null;
    }

    public static Map<String, String> toStringMap(final Dictionary<String, Object> dictionary) {
        final List<String> keys = Collections.list(dictionary.keys());
        return keys.stream().collect(Collectors.toMap(identity(), v -> dictionary.get(v).toString()));
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

}
