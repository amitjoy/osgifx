/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.mcp.data;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import com.osgifx.console.mcp.dto.McpLogEntryDTO;
import com.osgifx.console.mcp.dto.McpToolDTO;

@ProviderType
/**
 * Provides read-only access to MCP (Model Context Protocol) server metadata for UI/clients.
 * <p>
 * Implementations are expected to return snapshots of the currently registered tools and the
 * server log entries.
 */
public interface McpDataProvider {

    /**
     * Returns a snapshot of the currently registered MCP tools.
     *
     * @return a list of tool DTOs
     */
    List<McpToolDTO> tools();

    /**
     * Returns a snapshot of the current MCP server log entries.
     *
     * @return a list of log entry DTOs
     */
    List<McpLogEntryDTO> logs();

}
