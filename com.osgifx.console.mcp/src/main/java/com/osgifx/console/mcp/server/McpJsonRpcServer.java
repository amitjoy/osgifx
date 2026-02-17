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
package com.osgifx.console.mcp.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.Gson;

/**
 * A lightweight implementation of an MCP (Model Context Protocol) Server.
 * <p>
 * This class handles:
 * <ul>
 * <li>JSON-RPC 2.0 Request/Response parsing (via GSON)</li>
 * <li>MCP Protocol Handshakes (initialize, notifications)</li>
 * <li>Tool Registration and Execution</li>
 * </ul>
 * <p>
 * It communicates using pure JSON strings, making it transport-agnostic (works
 * over Stdio, Socket, etc.).
 *
 * @see JsonRpc
 */
public class McpJsonRpcServer {

    private final Gson                          gson;
    private final Map<String, ToolRegistration> tools = new ConcurrentHashMap<>();
    private Consumer<String>                    notificationSender;
    // Circular buffer for logs
    private final List<McpLogEntry> logs  = new ArrayList<>();
    private static final int        LIMIT = 100;

    // MCP Spec Version
    private static final String LATEST_PROTOCOL_VERSION = "2024-11-05";

    public McpJsonRpcServer(final Gson gson) {
        this.gson = gson;
    }

    public synchronized void addLog(final McpLogEntry.Type type, final String content) {
        if (logs.size() >= LIMIT) {
            logs.remove(0);
        }
        logs.add(new McpLogEntry(type, content));
    }

    public synchronized List<McpLogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public void setNotificationSender(final Consumer<String> notificationSender) {
        this.notificationSender = notificationSender;
    }

    // --- API ---

    /**
     * Registers a new tool capability with this server.
     *
     * @param name The unique name of the tool (e.g. "list_bundles").
     * @param description A human/AI-readable description of what the tool does.
     * @param inputSchema A JSON Schema map defining the expected arguments.
     * @param handler A function that executes the tool logic.
     */
    public void registerTool(final String name,
                             final String description,
                             final Map<String, Object> inputSchema,
                             final Function<Map<String, Object>, Object> handler) {
        tools.put(name, new ToolRegistration(name, description, inputSchema, handler));
        notifyToolsListChanged();
    }

    public void removeTool(final String name) {
        tools.remove(name);
        notifyToolsListChanged();
    }

    public List<Map<String, Object>> getTools() {
        final List<Map<String, Object>> toolList = new ArrayList<>();
        for (final ToolRegistration t : tools.values()) {
            toolList.add(Map.of("name", t.name, "description", t.description, "inputSchema", t.inputSchema));
        }
        return toolList;
    }

    private void notifyToolsListChanged() {
        if (notificationSender != null) {
            // Notification: { "jsonrpc": "2.0", "method": "notifications/tools/list_changed" }
            final String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/tools/list_changed\"}";
            notificationSender.accept(notification);
        }
    }

    /**
     * Processes an incoming JSON-RPC message string.
     *
     * @param jsonBody The raw JSON string received from the transport.
     * @return A JSON string response (success, error, or null if no response needed).
     */
    public String handleMessage(final String jsonBody) {
        addLog(McpLogEntry.Type.REQUEST, jsonBody);
        JsonRpc.Request req;
        try {
            req = gson.fromJson(jsonBody, JsonRpc.Request.class);
        } catch (final Exception e) {
            final var error = error(null, -32700, "Parse error");
            addLog(McpLogEntry.Type.RESPONSE, error);
            return error;
        }

        try {
            String response;
            switch (req.method) {
                // HANDSHAKE: Client says "Hello"
                case "initialize":
                    response = handleInitialize(req);
                    break;

                // HANDSHAKE: Client says "I got your hello"
                case "notifications/initialized":
                    response = null; // No response allowed for notifications
                    break;

                // DISCOVERY: Client asks "What can you do?"
                case "tools/list":
                    response = handleListTools(req);
                    break;

                // EXECUTION: Client says "Do this"
                case "tools/call":
                    response = handleToolCall(req);
                    break;

                case "ping":
                    response = success(req.id, Map.of());
                    break;

                default:
                    // If it's a notification (no ID), ignore it. If request, send error.
                    response = req.id == null ? null : error(req.id, -32601, "Method not found: " + req.method);
                    break;
            }
            if (response != null) {
                addLog(McpLogEntry.Type.RESPONSE, response);
            }
            return response;
        } catch (final Exception e) {
            final var error = error(req.id, -32000, "Server error: " + e.getMessage());
            addLog(McpLogEntry.Type.RESPONSE, error);
            return error;
        }
    }

    // --- Handlers ---

    private String handleInitialize(final JsonRpc.Request req) {
        // We declare what we support (only Tools, no Resources/Prompts yet)
        final var capabilities = Map.of("tools", Map.of("listChanged", true), // We can notify if tools change
                "resources", Map.of("listChanged", false), "prompts", Map.of("listChanged", false));

        final var result = Map.of("protocolVersion", LATEST_PROTOCOL_VERSION, "capabilities", capabilities,
                "serverInfo", Map.of("name", "OSGi.fx", "version", "1.0.0"));
        return success(req.id, result);
    }

    private String handleListTools(final JsonRpc.Request req) {
        final List<Map<String, Object>> toolList = new ArrayList<>();
        for (final ToolRegistration t : tools.values()) {
            toolList.add(Map.of("name", t.name, "description", t.description, "inputSchema", t.inputSchema));
        }
        return success(req.id, Map.of("tools", toolList));
    }

    @SuppressWarnings("unchecked")
    private String handleToolCall(final JsonRpc.Request req) {
        final String              name = (String) req.params.get("name");
        final Map<String, Object> args = (Map<String, Object>) req.params.get("arguments");

        final ToolRegistration tool = tools.get(name);
        if (tool == null) {
            return error(req.id, -32601, "Tool not found");
        }

        try {
            final Object result = tool.handler.apply(args);

            String text;
            if (result instanceof String string) {
                text = string;
            } else if (result instanceof byte[] bytes) {
                text = Base64.getEncoder().encodeToString(bytes);
            } else {
                text = gson.toJson(result);
            }

            return success(req.id, Map.of("content", List.of(Map.of("type", "text", "text", text)), "isError", false));
        } catch (final Exception e) {
            // Application error (e.g., bundle not found) is still a valid protocol response
            return success(req.id, Map.of("content",
                    List.of(Map.of("type", "text", "text", "Error: " + e.getMessage())), "isError", true));
        }
    }

    // --- Helpers (Same as before) ---
    private String success(final Object id, final Object result) {
        final var res = new JsonRpc.Response();
        res.id     = id;
        res.result = result;
        return gson.toJson(res);
    }

    private String error(final Object id, final int code, final String msg) {
        final var res = new JsonRpc.Response();
        res.id    = id;
        res.error = new JsonRpc.Error(code, msg);
        return gson.toJson(res);
    }

    // Internal Registration Class
    private static class ToolRegistration {
        String                                name;
        String                                description;
        Map<String, Object>                   inputSchema;
        Function<Map<String, Object>, Object> handler;

        ToolRegistration(final String name,
                         final String description,
                         final Map<String, Object> inputSchema,
                         final Function<Map<String, Object>, Object> handler) {
            this.name        = name;
            this.description = description;
            this.inputSchema = inputSchema;
            this.handler     = handler;
        }
    }
}