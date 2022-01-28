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
    INTEGER,
    BOOLEAN,
    DOUBLE,
    FLOAT,
    CHAR,
    LONG;

    public static Class<?> clazz(final ConfigurationType type) {
        switch (type) {
            case STRING:
                return String.class;
            case INTEGER:
                return Integer.class;
            case BOOLEAN:
                return Boolean.class;
            case DOUBLE:
                return Double.class;
            case FLOAT:
                return Float.class;
            case CHAR:
                return Character.class;
            case LONG:
                return Long.class;
            default:
                return String.class;
        }
    }

}
