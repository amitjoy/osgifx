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
package com.osgifx.console.ui.logs.helper;

import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

public final class LogsHelper {

    private LogsHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static boolean validateKeyValuePairs(final String value) {
        try {
            if (value.isBlank()) {
                return true;
            }
            prepareKeyValuePairs(value);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    public static Map<String, String> prepareKeyValuePairs(final Object value) {
        final var v = value.toString();
        if (v.isBlank()) {
            return Map.of();
        }
        final var splittedMap = Splitter.on(System.lineSeparator()).trimResults().withKeyValueSeparator('=').split(v);
        return Maps.newHashMap(splittedMap);
    }

    public static String mapToString(final Map<?, ?> map) {
        return Joiner.on(System.lineSeparator()).withKeyValueSeparator("=").join(map);
    }

}
