/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.mcp;

import java.util.HashMap;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A zero-dependency builder for generating JSON Schemas for {@link McpTool} inputs.
 * <p>
 * This class follows the builder pattern to construct a valid JSON Schema object map
 * required by the {@link McpTool#inputSchema()} method.
 *
 * @see McpTool
 */
@ProviderType
public class McpToolSchema {

    private final Map<String, Object>    properties = new HashMap<>();
    private final java.util.List<String> required   = new java.util.ArrayList<>();

    /**
     * Creates a new instance of the schema builder.
     *
     * @return a new {@link McpToolSchema} builder instance
     */
    public static McpToolSchema builder() {
        return new McpToolSchema();
    }

    /**
     * Adds a required argument to the schema.
     *
     * @param name The name of the argument (e.g., "bundleId").
     * @param type The JSON type (e.g., "string", "integer", "boolean", "number").
     * @param description A clear description of the argument for the AI model.
     * @return this builder instance
     */
    public McpToolSchema arg(final String name, final String type, final String description) {
        final var prop = new HashMap<String, String>();
        prop.put("type", type);
        prop.put("description", description);
        properties.put(name, prop);
        required.add(name);
        return this;
    }

    /**
     * Adds an optional argument to the schema.
     *
     * @param name The name of the argument.
     * @param type The JSON type.
     * @param description A clear description of the argument.
     * @return this builder instance
     */
    public McpToolSchema optionalArg(final String name, final String type, final String description) {
        final var prop = new HashMap<String, String>();
        prop.put("type", type);
        prop.put("description", description);
        properties.put(name, prop);
        return this;
    }

    /**
     * Builds the final JSON Schema map.
     *
     * @return A map representing the JSON Schema object with "type", "properties", and "required" fields.
     */
    public Map<String, Object> build() {
        return Map.of("type", "object", "properties", properties, "required", required);
    }
}