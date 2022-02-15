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
package com.osgifx.console.ui.events.converter;

public enum ValueType {

    STRING,
    INTEGER,
    BOOLEAN,
    DOUBLE,
    FLOAT,
    CHAR,
    LONG;

    public static Class<?> clazz(final ValueType type) {
        return switch (type) {
            case STRING -> String.class;
            case INTEGER -> Integer.class;
            case BOOLEAN -> Boolean.class;
            case DOUBLE -> Double.class;
            case FLOAT -> Float.class;
            case CHAR -> Character.class;
            case LONG -> Long.class;
            default -> String.class;
        };
    }

}
