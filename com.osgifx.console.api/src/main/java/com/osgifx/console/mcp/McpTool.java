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

import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Service interface for defining an MCP (Model Context Protocol) tool.
 * <p>
 * Services registered with this interface are automatically exposed to the
 * connected MCP client (e.g., an AI assistant). The tool's capabilities
 * are described by {@link #inputSchema()} and invoked via {@link #execute(Map)}.
 *
 * @see McpToolSchema
 */
@ConsumerType
public interface McpTool {

    /**
     * Defines the structure of the arguments this tool accepts.
     * <p>
     * The returned map must represent a valid JSON Schema object.
     * It is recommended to use {@link McpToolSchema} to build this map.
     *
     * @return A Map representing the JSON Schema of the tool's input arguments.
     */
    Map<String, Object> inputSchema();

    /**
     * Executes the tool logic.
     *
     * @param args The input arguments provided by the MCP client, matching the schema.
     * @return The result of the execution. Can be a simple value, a Map, or a custom object.
     * @throws Exception If the execution fails.
     */
    Object execute(Map<String, Object> args) throws Exception;
}