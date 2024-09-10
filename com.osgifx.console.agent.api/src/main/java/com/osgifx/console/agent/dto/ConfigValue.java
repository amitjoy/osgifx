/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.dto;

/**
 * Represents a configuration value consisting of a key, a value, and a type.
 * This class is used to encapsulate the configuration properties that can be 
 * serialized to JSON and transferred between components or systems.
 * <p>
 * Each instance of this class holds a single configuration entry, defined by 
 * its key, value, and type, which can be used in different contexts such as 
 * configuration management or data exchange.
 * </p>
 */
public class ConfigValue {

    /** The key of the configuration entry. */
    public String key;

    /** The value associated with the configuration key. */
    public Object value;

    /** The type of the configuration value, represented by {@link XAttributeDefType}. */
    public XAttributeDefType type;

    /**
     * Default constructor required for JSON serialization.
     */
    public ConfigValue() {
        // required for JSON serialization
    }

    /**
     * Constructs a new {@code ConfigValue} instance with the specified key, value, and type.
     *
     * @param key   the key of the configuration entry, must not be null
     * @param value the value associated with the key, can be any object
     * @param type  the type of the configuration value, must not be null
     */
    public ConfigValue(final String key, final Object value, final XAttributeDefType type) {
        this.key = key;
        this.value = value;
        this.type = type;
    }

    /**
     * Factory method to create a new {@code ConfigValue} instance.
     *
     * @param key   the key of the configuration entry, must not be null
     * @param value the value associated with the key, can be any object
     * @param type  the type of the configuration value, must not be null
     * @return a new {@code ConfigValue} instance with the specified key, value, and type
     */
    public static ConfigValue create(final String key, final Object value, final XAttributeDefType type) {
        return new ConfigValue(key, value, type);
    }

}