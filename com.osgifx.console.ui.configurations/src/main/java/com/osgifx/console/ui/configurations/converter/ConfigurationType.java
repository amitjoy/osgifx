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

public enum ConfigurationType {

    STRING,
    STRING_ARRAY,
    STRING_LIST,
    INTEGER,
    INTEGER_ARRAY,
    INTEGER_LIST,
    BOOLEAN,
    BOOLEAN_ARRAY,
    BOOLEAN_LIST,
    DOUBLE,
    DOUBLE_ARRAY,
    DOUBLE_LIST,
    FLOAT,
    FLOAT_ARRAY,
    FLOAT_LIST,
    CHAR,
    CHAR_ARRAY,
    CHAR_LIST,
    LONG,
    LONG_ARRAY,
    LONG_LIST;

    public static Class<?> clazz(final ConfigurationType type) {
        return switch (type) {
            case STRING -> String.class;
            case INTEGER -> Integer.class;
            case BOOLEAN -> Boolean.class;
            case DOUBLE -> Double.class;
            case FLOAT -> Float.class;
            case CHAR -> Character.class;
            case LONG -> Long.class;
            default -> null;
        };
    }

}
