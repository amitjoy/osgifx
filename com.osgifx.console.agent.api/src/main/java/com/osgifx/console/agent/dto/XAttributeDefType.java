/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.dto;

import java.util.List;

/**
 * Enum representing various attribute definition types supported in the OSGi
 * framework. This enum provides types for different data formats such as strings,
 * integers, booleans, doubles, floats, characters, and longs, including their
 * array and list representations.
 * <p>
 * The {@code XAttributeDefType} enum also includes utility methods to determine
 * the appropriate type for a given object value and to retrieve the corresponding
 * class type for each enum constant.
 * </p>
 */
public enum XAttributeDefType {

    /** Represents a single String value. */
    STRING,

    /** Represents an array of String values. */
    STRING_ARRAY,

    /** Represents a list of String values. */
    STRING_LIST,

    /** Represents a single Integer value. */
    INTEGER,

    /** Represents an array of Integer values. */
    INTEGER_ARRAY,

    /** Represents a list of Integer values. */
    INTEGER_LIST,

    /** Represents a single Boolean value. */
    BOOLEAN,

    /** Represents an array of Boolean values. */
    BOOLEAN_ARRAY,

    /** Represents a list of Boolean values. */
    BOOLEAN_LIST,

    /** Represents a single Double value. */
    DOUBLE,

    /** Represents an array of Double values. */
    DOUBLE_ARRAY,

    /** Represents a list of Double values. */
    DOUBLE_LIST,

    /** Represents a single Float value. */
    FLOAT,

    /** Represents an array of Float values. */
    FLOAT_ARRAY,

    /** Represents a list of Float values. */
    FLOAT_LIST,

    /** Represents a single Character value. */
    CHAR,

    /** Represents an array of Character values. */
    CHAR_ARRAY,

    /** Represents a list of Character values. */
    CHAR_LIST,

    /** Represents a single Long value. */
    LONG,

    /** Represents an array of Long values. */
    LONG_ARRAY,

    /** Represents a list of Long values. */
    LONG_LIST,

    /** Represents a password value stored as a String. */
    PASSWORD;

    /**
     * Determines the appropriate {@code XAttributeDefType} for the given object value.
     *
     * @param value the object to determine the type for
     * @return the corresponding {@code XAttributeDefType} based on the object's class
     */
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

    /**
     * Returns the corresponding Java class for the specified {@code XAttributeDefType}.
     *
     * @param type the {@code XAttributeDefType} for which to get the class
     * @return the corresponding Java class, or null if the type is not recognized
     */
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