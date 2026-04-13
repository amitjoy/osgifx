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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class McpJsonRpcServerTest {

    private McpJsonRpcServer server;
    private Gson             gson;

    @Before
    public void setUp() {
        gson   = new Gson();
        server = new McpJsonRpcServer(gson);
    }

    // ---- Initialize ----

    @Test
    public void testInitializeReturnsNegotiatedVersion() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"2025-03-26\"}}";
        final String response = server.handleMessage(request);

        final JsonObject json   = JsonParser.parseString(response).getAsJsonObject();
        final JsonObject result = json.getAsJsonObject("result");

        assertEquals("2.0", json.get("jsonrpc").getAsString());
        assertEquals("2025-03-26", result.get("protocolVersion").getAsString());
    }

    @Test
    public void testInitializeFallsBackForUnknownVersion() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"1.0.0\"}}";
        final String response = server.handleMessage(request);

        final JsonObject result = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");

        assertEquals("2025-11-25", result.get("protocolVersion").getAsString());
    }

    @Test
    public void testInitializeDeclaresOnlyToolsCapability() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"2025-03-26\"}}";
        final String response = server.handleMessage(request);

        final JsonObject result       = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");
        final JsonObject capabilities = result.getAsJsonObject("capabilities");

        assertTrue(capabilities.has("tools"));
        assertFalse("resources should not be declared", capabilities.has("resources"));
        assertFalse("prompts should not be declared", capabilities.has("prompts"));
    }

    @Test
    public void testInitializeContainsServerInfo() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"2025-03-26\"}}";
        final String response = server.handleMessage(request);

        final JsonObject result     = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");
        final JsonObject serverInfo = result.getAsJsonObject("serverInfo");

        assertEquals("OSGi.fx", serverInfo.get("name").getAsString());
    }

    @Test
    public void testInitializeContainsInstructions() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"2025-03-26\"}}";
        final String response = server.handleMessage(request);

        final JsonObject result = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");

        assertTrue(result.has("instructions"));
        assertFalse(result.get("instructions").getAsString().isEmpty());
    }

    // ---- Notifications ----

    @Test
    public void testInitializedNotificationReturnsNull() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";
        final String response = server.handleMessage(request);

        assertNull(response);
    }

    // ---- Ping ----

    @Test
    public void testPingReturnsEmptyResult() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}";
        final String response = server.handleMessage(request);

        final JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertNotNull(json.get("result"));
    }

    // ---- Tools List ----

    @Test
    public void testToolsListReturnsEmptyWhenNoTools() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}";
        final String response = server.handleMessage(request);

        final JsonObject result = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");

        assertEquals(0, result.getAsJsonArray("tools").size());
    }

    @Test
    public void testToolsListReturnsRegisteredTool() {
        server.registerTool("test_tool", "A test tool",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()), _ -> "ok");

        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}";
        final String response = server.handleMessage(request);

        final JsonObject result = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");

        assertEquals(1, result.getAsJsonArray("tools").size());
        assertEquals("test_tool", result.getAsJsonArray("tools").get(0).getAsJsonObject().get("name").getAsString());
    }

    @Test
    public void testToolsListPagination() {
        // Register more than page size tools (page size is 50)
        for (int i = 0; i < 60; i++) {
            server.registerTool("tool_" + i, "Tool " + i,
                    Map.of("type", "object", "properties", Map.of(), "required", List.of()), _ -> "ok");
        }

        // First page
        final String     request1  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}";
        final String     response1 = server.handleMessage(request1);
        final JsonObject result1   = JsonParser.parseString(response1).getAsJsonObject().getAsJsonObject("result");

        assertEquals(50, result1.getAsJsonArray("tools").size());
        assertTrue("Should have nextCursor", result1.has("nextCursor"));
        assertEquals("50", result1.get("nextCursor").getAsString());

        // Second page
        final String     request2  = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\","
                + "\"params\":{\"cursor\":\"50\"}}";
        final String     response2 = server.handleMessage(request2);
        final JsonObject result2   = JsonParser.parseString(response2).getAsJsonObject().getAsJsonObject("result");

        assertEquals(10, result2.getAsJsonArray("tools").size());
        assertFalse("Should not have nextCursor on last page", result2.has("nextCursor"));
    }

    @Test
    public void testToolsListInvalidCursorReturnsError() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\","
                + "\"params\":{\"cursor\":\"not-a-number\"}}";
        final String response = server.handleMessage(request);

        final JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertNotNull("Should return error for invalid cursor", json.get("error"));
    }

    // ---- Tool Call ----

    @Test
    public void testToolCallExecutesTool() {
        server.registerTool("echo", "Echoes input",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()),
                args -> "hello " + args.get("name"));

        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"echo\",\"arguments\":{\"name\":\"world\"}}}";
        final String response = server.handleMessage(request);

        final JsonObject result = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");

        assertFalse(result.get("isError").getAsBoolean());
        assertTrue(result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString()
                .contains("hello world"));
    }

    @Test
    public void testToolCallNotFound() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"nonexistent\",\"arguments\":{}}}";
        final String response = server.handleMessage(request);

        final JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertNotNull(json.get("error"));
        assertEquals(-32601, json.getAsJsonObject("error").get("code").getAsInt());
    }

    @Test
    public void testToolCallExecutionErrorReturnsIsErrorTrue() {
        server.registerTool("fail", "Always fails",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()), _ -> {
                    throw new RuntimeException("intentional failure");
                });

        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"fail\",\"arguments\":{}}}";
        final String response = server.handleMessage(request);

        final JsonObject result = JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");

        assertTrue(result.get("isError").getAsBoolean());
        assertTrue(result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString()
                .contains("intentional failure"));
    }

    // ---- Error Handling ----

    @Test
    public void testUnknownMethodReturnsError() {
        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"unknown/method\"}";
        final String response = server.handleMessage(request);

        final JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals(-32601, json.getAsJsonObject("error").get("code").getAsInt());
    }

    @Test
    public void testUnknownNotificationMethodReturnsNull() {
        // Notification = no ID
        final String request  = "{\"jsonrpc\":\"2.0\",\"method\":\"unknown/notification\"}";
        final String response = server.handleMessage(request);

        assertNull(response);
    }

    @Test
    public void testInvalidJsonReturnsParseError() {
        final String response = server.handleMessage("not valid json{{{");

        final JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertEquals(-32700, json.getAsJsonObject("error").get("code").getAsInt());
    }

    // ---- Batch Support ----

    @Test
    public void testBatchRequestProcessesMultipleRequests() {
        final String batch = "[" + "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"},"
                + "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}" + "]";

        final String response = server.handleMessage(batch);

        // Response should be a JSON array
        assertTrue(response.startsWith("["));
        final var array = JsonParser.parseString(response).getAsJsonArray();
        assertEquals(2, array.size());
    }

    @Test
    public void testBatchWithNotificationsFiltersNullResponses() {
        final String batch = "[" + "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"},"
                + "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}" + "]";

        final String response = server.handleMessage(batch);

        final var array = JsonParser.parseString(response).getAsJsonArray();
        assertEquals("Only the ping should have a response", 1, array.size());
    }

    @Test
    public void testBatchAllNotificationsReturnsNull() {
        final String batch = "[" + "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"},"
                + "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}" + "]";

        final String response = server.handleMessage(batch);

        assertNull("All notifications should return null", response);
    }

    // ---- Tool Removal ----

    @Test
    public void testRemoveToolMakesItUnavailable() {
        server.registerTool("temp", "A temporary tool",
                Map.of("type", "object", "properties", Map.of(), "required", List.of()), _ -> "ok");
        server.removeTool("temp");

        final String request  = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"temp\",\"arguments\":{}}}";
        final String response = server.handleMessage(request);

        final JsonObject json = JsonParser.parseString(response).getAsJsonObject();

        assertNotNull("Removed tool should return error", json.get("error"));
    }

    // ---- Logging ----

    @Test
    public void testHandleMessageLogsRequestsAndResponses() {
        server.handleMessage("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}");

        final var logs = server.getLogs();

        assertEquals(2, logs.size());
        assertEquals(McpLogEntry.Type.REQUEST, logs.get(0).getType());
        assertEquals(McpLogEntry.Type.RESPONSE, logs.get(1).getType());
    }
}
