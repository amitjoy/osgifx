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

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.osgifx.console.mcp.FxMcpServer;
import com.osgifx.console.mcp.provider.McpHttpServer.Configuration;
import com.osgifx.console.mcp.server.McpProtocol;

import io.fusionauth.http.HTTPMethod;
import io.fusionauth.http.server.HTTPHandler;
import io.fusionauth.http.server.HTTPListenerConfiguration;
import io.fusionauth.http.server.HTTPRequest;
import io.fusionauth.http.server.HTTPResponse;
import io.fusionauth.http.server.HTTPServer;

@Designate(ocd = Configuration.class)
@Component(service = FxMcpServer.class)
public class McpHttpServer implements FxMcpServer {

    @ObjectClassDefinition(name = "MCP HTTP Server Configuration", description = "Configuration for the MCP HTTP Server")
    public @interface Configuration {
        @AttributeDefinition(name = "Port", description = "The port to listen on", required = false)
        int port() default 8080;
    }

    @Reference(cardinality = MANDATORY, policy = STATIC)
    private McpServerProvider mcpProvider;

    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;
    private int           port;

    private HTTPServer httpServer;

    // Tracks active SSE connections with their associated locks and conditions
    private final Map<ConnectionContext, HTTPResponse> activeSseConnections = new ConcurrentHashMap<>();
    private final Lock                                 sseConnectionsLock   = new ReentrantLock();

    // Helper class to hold lock and condition for each SSE connection
    private static class ConnectionContext {
        final Lock      lock      = new ReentrantLock();
        final Condition condition = lock.newCondition();
    }

    @Activate
    void activate(final Configuration config) {
        port   = config.port();
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
        // Register notification sender to broadcast to all SSE clients
        mcpProvider.getServer().setNotificationSender(this::broadcast);
    }

    @Deactivate
    void deactivate() throws Exception {
        stop();
    }

    @SuppressWarnings("resource")
    @Override
    public void start() throws Exception {
        if (httpServer != null) {
            return;
        }

        // Port availability check because FusionAuth's HTTPServer swallows bind exceptions natively.
        try (var _ = new ServerSocket(port)) {
            // Port is available, closed immediately.
        } catch (final Exception e) {
            throw new RuntimeException("Port " + port + " is already in use or unavailable", e);
        }

        final HTTPHandler handler = (req, res) -> {
            final String path = req.getPath();
            logger.atInfo().log("Incoming Request: %s %s", req.getMethod(), path);

            // Streamable HTTP Endpoint (Legacy OSGi.fx implementation)
            if ("/mcp".equals(path) || path.startsWith("/mcp?")) {
                if (HTTPMethod.POST == req.getMethod()) {
                    handleMessage(req, res, req.getHeader("Mcp-Session-Id"));
                    return;
                }
                if (HTTPMethod.GET == req.getMethod() && "text/event-stream".equals(req.getHeader("Accept"))) {
                    handleSseConnection(req, res, false); // false = streamable HTTP mode
                    return;
                }
            }

            // Traditional SSE Endpoint (Expected by Claude Code, Cursor, etc.)
            if ("/sse".equals(path) || path.startsWith("/sse?")) {
                if (HTTPMethod.GET == req.getMethod() && "text/event-stream".equals(req.getHeader("Accept"))) {
                    handleSseConnection(req, res, true); // true = traditional SSE mode
                    return;
                }
            }

            // Traditional Message Endpoint
            if ("/messages".equals(path) || path.startsWith("/messages?")) {
                if (HTTPMethod.POST == req.getMethod()) {
                    // Extract session ID from query parameters
                    final Map<String, List<String>> params = req.getURLParameters();
                    String                          sessionId;
                    if (params != null && params.containsKey("sessionId")) {
                        final List<String> sessionIds = params.get("sessionId");
                        sessionId = (sessionIds == null || sessionIds.isEmpty()) ? null : sessionIds.get(0);
                        if (sessionId != null && sessionId.contains("[")) {
                            // FusionAuth might wrap params in arrays depending on parsing
                            sessionId = sessionId.replaceAll("[\\[\\]]", "");
                        }
                    } else {
                        sessionId = null;
                    }
                    handleMessage(req, res, sessionId);
                    return;
                }
            }

            logger.atError().log("404 Not Found: %s %s", req.getMethod(), path);
            res.setStatus(404);
        };

        final HTTPListenerConfiguration listenerConfig = new HTTPListenerConfiguration(port);

        httpServer = new HTTPServer().withListener(listenerConfig).withHandler(handler);
        httpServer.start();

        logger.atInfo().log("MCP HTTP Server started at http://localhost:%d/mcp", port);
    }

    @Override
    public void stop() throws Exception {
        if (httpServer != null) {
            httpServer.close();
            httpServer = null;
        }
        // Signal all waiting SSE connections to close
        sseConnectionsLock.lock();
        try {
            activeSseConnections.keySet().forEach(ctx -> {
                ctx.lock.lock();
                try {
                    ctx.condition.signalAll();
                } finally {
                    ctx.lock.unlock();
                }
            });
        } finally {
            sseConnectionsLock.unlock();
        }
        logger.atInfo().log("MCP HTTP Server stopped");
    }

    /**
     * Broadcasts a message to all active SSE connections.
     *
     * @param message The JSON-RPC message to send.
     */
    private void broadcast(final String message) {
        // Format as SSE event: name 'message'
        final String sseData = "event: message\ndata: " + message + "\n\n";
        final byte[] bytes   = sseData.getBytes(StandardCharsets.UTF_8);

        // Iterate safely over active connections
        activeSseConnections.values().forEach(res -> {
            try {
                // Note: Writing to the output stream from a different thread while the handlers
                // are blocked might require the underlying server to support concurrent IO.
                // FusionAuth HTTP server's response output stream should be thread-safe for writing.
                res.getOutputStream().write(bytes);
                res.flush();
            } catch (final IOException e) {
                logger.atWarning().withException(e).log("Failed to broadcast to SSE client");
                // Client managed by handler thread lock; will exit on write failure
            }
        });
    }

    private void handleSseConnection(final HTTPRequest req, final HTTPResponse res, final boolean isTraditionalMode) {
        try {
            // Log Streamable HTTP headers for debugging/context
            String       sessionId   = req.getHeader("Mcp-Session-Id");
            final String lastEventId = req.getHeader("Last-Event-ID");
            logger.atDebug().log("SSE Connection Request - Session: %s, Last-Event-ID: %s", sessionId, lastEventId);

            // Standard SSE Headers
            res.setHeader("Content-Type", "text/event-stream");
            res.setHeader("Cache-Control", "no-cache");
            res.setHeader("Connection", "keep-alive");

            // Negotiate Protocol Version at the transport level
            final String requestedVersion = req.getHeader("MCP-Protocol-Version");
            if (requestedVersion != null) {
                final String negotiatedVersion = McpProtocol.negotiateVersion(requestedVersion);
                res.setHeader("MCP-Protocol-Version", negotiatedVersion);
            }

            // Generate session ID if missing
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
            }

            // Streamable HTTP requires header reflection
            if (!isTraditionalMode) {
                res.setHeader("Mcp-Session-Id", sessionId);
            }

            res.setStatus(200);
            res.flush(); // Send headers

            // Traditional SSE requires an endpoint event immediately
            if (isTraditionalMode) {
                final String endpointUri   = "/messages?sessionId=" + sessionId;
                final String endpointEvent = "event: endpoint\ndata: " + endpointUri + "\n\n";
                res.getOutputStream().write(endpointEvent.getBytes(StandardCharsets.UTF_8));
                res.flush();
                logger.atDebug().log("Emitted traditional SSE endpoint: %s", endpointUri);
            }

            // Keep connection open by blocking
            final ConnectionContext ctx = new ConnectionContext();
            activeSseConnections.put(ctx, res);

            try {
                ctx.lock.lock();
                try {
                    // Wait indefinitely until server stop or client disconnect
                    ctx.condition.await();
                } finally {
                    ctx.lock.unlock();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activeSseConnections.remove(ctx);
            }
        } catch (final IOException e) {
            // Client likely disconnected, which is normal behavior
            logger.atDebug().log("SSE Client disconnected");
        }
    }

    private void handleMessage(final HTTPRequest req, final HTTPResponse res, String sessionId) {
        try {
            // Log session context
            logger.atDebug().log("MCP Message - Session: %s", sessionId);

            // Read the JSON body
            final String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Generate Session ID on initialize if somehow missing
            if (sessionId == null && body.contains("\"method\":\"initialize\"")) {
                sessionId = UUID.randomUUID().toString();
                logger.atDebug().log("Generated new session ID for initialization: %s", sessionId);
            }

            // Delegate to the MCP JSON-RPC Server
            final String responseJson = mcpProvider.getServer().handleMessage(body);

            // Send the response
            if (responseJson != null) {
                res.setHeader("Content-Type", "application/json");
                // Reflect session ID if present/generated?
                // Spec says server SHOULD echo it if it generated it, but usually client sends it.
                if (sessionId != null) {
                    res.setHeader("Mcp-Session-Id", sessionId);
                }

                // Negotiate Protocol Version at the transport level
                final String requestedVersion = req.getHeader("MCP-Protocol-Version");
                if (requestedVersion != null) {
                    final String negotiatedVersion = McpProtocol.negotiateVersion(requestedVersion);
                    res.setHeader("MCP-Protocol-Version", negotiatedVersion);
                }

                res.setStatus(200);
                res.getOutputStream().write(responseJson.getBytes(StandardCharsets.UTF_8));
            } else {
                // If it was a notification, return 202 Accepted (per spec)
                res.setStatus(202);
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Error handling MCP message");
            res.setStatus(500);
        }
    }
}