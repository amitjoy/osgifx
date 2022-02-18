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
package com.osgifx.console.agent.dto;

import java.util.List;

public enum XAttributeDefType {

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
    LONG_LIST,
    PASSWORD;

    public static XAttributeDefType getType(final Object value) {
        final Class<?> clazz = value.getClass();
        if (clazz.equals(String.class)) {
            return XAttributeDefType.STRING;
        }
        if (clazz.equals(String[].class)) {
            return XAttributeDefType.STRING_ARRAY;
        }
        if (value instanceof List<?>) {
            final List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                final Object element = list.get(0);
                if (element.getClass().equals(String.class)) {
                    return XAttributeDefType.STRING_LIST;
                }
            }
        }

        if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            return XAttributeDefType.INTEGER;
        }
        if (clazz.equals(int[].class)) {
            return XAttributeDefType.INTEGER_ARRAY;
        }
        if (value instanceof List<?>) {
            final List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                final Object element = list.get(0);
                if (element.getClass().equals(Integer.class)) {
                    return XAttributeDefType.INTEGER_LIST;
                }
            }
        }

        if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            return XAttributeDefType.BOOLEAN;
        }
        if (clazz.equals(boolean[].class)) {
            return XAttributeDefType.BOOLEAN_ARRAY;
        }
        if (value instanceof List<?>) {
            final List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                final Object element = list.get(0);
                if (element.getClass().equals(Boolean.class)) {
                    return XAttributeDefType.BOOLEAN_LIST;
                }
            }
        }

        if (clazz.equals(double.class) || clazz.equals(Double.class)) {
            return XAttributeDefType.DOUBLE;
        }
        if (clazz.equals(double[].class)) {
            return XAttributeDefType.DOUBLE_ARRAY;
        }
        if (value instanceof List<?>) {
            final List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                final Object element = list.get(0);
                if (element.getClass().equals(Double.class)) {
                    return XAttributeDefType.DOUBLE_LIST;
                }
            }
        }

        if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            return XAttributeDefType.FLOAT;
        }
        if (clazz.equals(float[].class)) {
            return XAttributeDefType.FLOAT_ARRAY;
        }
        if (value instanceof List<?>) {
            final List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                final Object element = list.get(0);
                if (element.getClass().equals(Float.class)) {
                    return XAttributeDefType.FLOAT_LIST;
                }
            }
        }

        if (clazz.equals(char.class) || clazz.equals(Character.class)) {
            return XAttributeDefType.CHAR;
        }
        if (clazz.equals(char[].class)) {
            return XAttributeDefType.CHAR_ARRAY;
        }
        if (value instanceof List<?>) {
            final List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                final Object element = list.get(0);
                if (element.getClass().equals(Character.class)) {
                    return XAttributeDefType.CHAR_LIST;
                }
            }
        }

        if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            return XAttributeDefType.LONG;
        }
        if (clazz.equals(long[].class)) {
            return XAttributeDefType.LONG_ARRAY;
        }
        if (value instanceof List<?>) {
            final List<?> list = (List<?>) value;
            if (!list.isEmpty()) {
                final Object element = list.get(0);
                if (element.getClass().equals(Long.class)) {
                    return XAttributeDefType.LONG_LIST;
                }
            }
        }
        return XAttributeDefType.STRING;
    }

    public static Class<?> clazz(final XAttributeDefType type) {
        switch (type) {
            case STRING:
            case PASSWORD:
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
                return null;
        }
    }

}
