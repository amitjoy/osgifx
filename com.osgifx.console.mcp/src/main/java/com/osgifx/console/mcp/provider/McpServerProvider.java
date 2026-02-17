/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.mcp.provider;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.converter.Converters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.osgifx.console.mcp.data.McpDataProvider;
import com.osgifx.console.mcp.dto.McpLogEntryDTO;
import com.osgifx.console.mcp.dto.McpLogEntryType;
import com.osgifx.console.mcp.dto.McpToolDTO;
import com.osgifx.console.mcp.McpTool;
import com.osgifx.console.mcp.server.McpJsonRpcServer;

/**
 * Central component that bridges {@link McpTool} services to the internal {@link McpJsonRpcServer}.
 * <p>
 * This component listens for any service registered with the {@link McpTool} interface
 * and registers them as tools in the embedded MCP server.
 */
@Component(service = { McpServerProvider.class, McpDataProvider.class })
public class McpServerProvider implements McpDataProvider {

    @interface ToolProps {
        String mcp_tool_name();

        String mcp_tool_description();
    }

    @Reference
    private LoggerFactory          factory;
    private FluentLogger           logger;
    private final McpJsonRpcServer internalServer = new McpJsonRpcServer(createGson());

    private static Gson createGson() {
        return new GsonBuilder().registerTypeHierarchyAdapter(DTO.class, new JsonSerializer<DTO>() {
            @Override
            public JsonElement serialize(final DTO src, final Type typeOfSrc, final JsonSerializationContext context) {
                final JsonObject json = new JsonObject();
                // DTOs have public fields. valid JSON-RPC requires we strictly read them.
                for (final Field field : src.getClass().getFields()) {
                    try {
                        json.add(field.getName(), context.serialize(field.get(src)));
                    } catch (final Exception e) {
                        // ignore inaccessible
                    }
                }
                return json;
            }
        }).create();
    }

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
        logger.atInfo().log("MCP server has been started successfully");
    }

    @Deactivate
    void deactivate() {
        logger.atInfo().log("MCP server has been stopped successfully");
    }

    /**
     * Binds a new MCP Tool service.
     * <p>
     * The tool name and description are extracted from the service properties
     * (using "mcp.tool.name" and "mcp.tool.description").
     *
     * @param tool The tool service instance
     * @param properties The service properties containing metadata
     */
    @Reference(cardinality = MULTIPLE, policy = DYNAMIC)
    void bindTool(final McpTool tool, final Map<String, Object> properties) {
        final var dto         = Converters.standardConverter().convert(properties).to(ToolProps.class);
        final var name        = dto.mcp_tool_name();
        final var description = dto.mcp_tool_description();

        if (name == null) {
            logger.atWarning().log("Skipping McpTool without 'mcp.tool.name' property");
            return;
        }

        internalServer.registerTool(name, description != null ? description : "", tool.inputSchema(), args -> {
            try {
                return tool.execute(args);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Unbinds an MCP Tool service.
     *
     * @param tool The tool service instance
     * @param properties The service properties
     */
    void unbindTool(final McpTool tool, final Map<String, Object> properties) {
        final var dto  = Converters.standardConverter().convert(properties).to(ToolProps.class);
        final var name = dto.mcp_tool_name();
        if (name != null) {
            internalServer.removeTool(name);
        }
    }

    public McpJsonRpcServer getServer() {
        return internalServer;
    }

    @Override
    public List<McpToolDTO> tools() {
        final var              registeredTools = internalServer.getTools();
        final List<McpToolDTO> toolDTOs        = new ArrayList<>();
        for (final var tool : registeredTools) {
            final var dto = new McpToolDTO();
            dto.name        = (String) tool.get("name");
            dto.description = (String) tool.get("description");
            @SuppressWarnings("unchecked")
            final Map<String, Object> schema = (Map<String, Object>) tool.get("inputSchema");
            dto.inputSchema = schema;
            toolDTOs.add(dto);
        }
        return toolDTOs;
    }

    @Override
    public List<McpLogEntryDTO> logs() {
        final var                  entries = internalServer.getLogs();
        final List<McpLogEntryDTO> logDTOs = new ArrayList<>();
        for (final var entry : entries) {
            final var dto = new McpLogEntryDTO();
            dto.timestamp = entry.getTimestamp();
            dto.type      = McpLogEntryType.valueOf(entry.getType().name());
            dto.content   = entry.getContent();
            logDTOs.add(dto);
        }
        return logDTOs;
    }
}