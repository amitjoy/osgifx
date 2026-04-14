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
import java.util.concurrent.atomic.AtomicLong;
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

    // Tracks active SSE connections by session ID
    private final Map<String, ConnectionContext> activeSseConnections = new ConcurrentHashMap<>();
    private final Lock                           sseConnectionsLock   = new ReentrantLock();
    private final AtomicLong                     eventIdCounter       = new AtomicLong(0);

    // Helper class to hold response, lock, and condition for each SSE connection
    private static class ConnectionContext {
        final HTTPResponse res;
        final Lock         lock      = new ReentrantLock();
        final Condition    condition = lock.newCondition();

        ConnectionContext(HTTPResponse res) {
            this.res = res;
        }
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

            // Origin validation to prevent DNS rebinding attacks (spec MUST)
            final String origin = req.getHeader("Origin");
            if (origin != null && !isOriginAllowed(origin)) {
                logger.atWarning().log("Rejected request from untrusted origin: %s", origin);
                res.setStatus(403);
                return;
            }

            // CORS Headers
            res.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
            res.setHeader("Access-Control-Allow-Headers",
                    "Content-Type, Authorization, Accept, MCP-Protocol-Version, Mcp-Session-Id, MCP-Session-Id, Last-Event-ID");
            res.setHeader("Access-Control-Expose-Headers", "MCP-Protocol-Version, Mcp-Session-Id, MCP-Session-Id");

            if (HTTPMethod.OPTIONS == req.getMethod()) {
                res.setStatus(200);
                return;
            }

            // Session termination via DELETE (spec: server MAY return 405)
            if (HTTPMethod.DELETE == req.getMethod()) {
                res.setStatus(405);
                return;
            }

            // Streamable HTTP Endpoint (Legacy OSGi.fx implementation)
            if ("/mcp".equals(path) || path.startsWith("/mcp?")) {
                if (HTTPMethod.POST == req.getMethod()) {
                    handleMessage(req, res, req.getHeader("Mcp-Session-Id"), false);
                    return;
                }
                if (HTTPMethod.GET == req.getMethod() && acceptsEventStream(req)) {
                    handleSseConnection(req, res, false); // false = streamable HTTP mode
                    return;
                }
            }

            // Traditional SSE Endpoint (Expected by Claude Code, Cursor, etc.)
            // No Accept header check — /sse is a dedicated SSE endpoint by convention
            if ("/sse".equals(path) || path.startsWith("/sse?")) {
                if (HTTPMethod.GET == req.getMethod()) {
                    handleSseConnection(req, res, true); // true = traditional SSE mode
                    return;
                }
            }

            // Traditional Message Endpoint
            if ("/messages".equals(path) || path.startsWith("/messages?")) {
                if (HTTPMethod.POST == req.getMethod()) {
                    // Extract session ID from query parameters
                    final Map<String, List<String>> params    = req.getURLParameters();
                    String                          sessionId = null;
                    if (params != null) {
                        // Support both 'sessionId' and 'session_id' for robustness
                        List<String> values = params.get("sessionId");
                        if (values == null) {
                            values = params.get("session_id");
                        }
                        if (values != null && !values.isEmpty()) {
                            sessionId = values.get(0).replaceAll("[\\[\\]]", "");
                        }
                    }
                    handleMessage(req, res, sessionId, true);
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
            activeSseConnections.values().forEach(ctx -> {
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
        final long   eventId = eventIdCounter.incrementAndGet();
        final String sseData = "id: " + eventId + "\nevent: message\ndata: " + message + "\n\n";
        final byte[] bytes   = sseData.getBytes(StandardCharsets.UTF_8);

        // Iterate safely over active connections
        activeSseConnections.values().forEach(ctx -> {
            try {
                // Note: Writing to the output stream from a different thread while the handlers
                // are blocked might require the underlying server to support concurrent IO.
                // FusionAuth HTTP server's response output stream should be thread-safe for writing.
                ctx.res.getOutputStream().write(bytes);
                ctx.res.flush();
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
            final String requestedVersion  = req.getHeader("MCP-Protocol-Version");
            final String negotiatedVersion = McpProtocol.negotiateVersion(requestedVersion);
            res.setHeader("MCP-Protocol-Version", negotiatedVersion);

            // Generate session ID if missing
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
            }
            res.setHeader("Mcp-Session-Id", sessionId);

            res.setStatus(200);
            res.flush(); // Send headers

            // Traditional SSE requires an endpoint event immediately
            if (isTraditionalMode) {
                // Use relative URI for the endpoint to support proxied/containerized clients properly
                final String endpointUri = String.format("/messages?sessionId=%s", sessionId);

                final long   eventId       = eventIdCounter.incrementAndGet();
                final String endpointEvent = "id: " + eventId + "\nevent: endpoint\ndata: " + endpointUri + "\n\n";
                res.getOutputStream().write(endpointEvent.getBytes(StandardCharsets.UTF_8));
                res.flush();
                logger.atDebug().log("Emitted traditional SSE endpoint: %s", endpointUri);
            }

            // Keep connection open by blocking
            final ConnectionContext ctx = new ConnectionContext(res);
            activeSseConnections.put(sessionId, ctx);

            try {
                ctx.lock.lock();
                try {
                    // Wait indefinitely until server stop or client disconnect
                    ctx.condition.await();
                } finally {
                    ctx.lock.unlock();
                }
            } catch (final InterruptedException _) {
                Thread.currentThread().interrupt();
            } finally {
                activeSseConnections.remove(sessionId);
            }
        } catch (final IOException e) {
            // Client likely disconnected, which is normal behavior
            logger.atDebug().log("SSE Client disconnected");
        }
    }

    private void handleMessage(final HTTPRequest req,
                               final HTTPResponse res,
                               String sessionId,
                               boolean isTraditionalMode) {
        try {
            // Log session context
            logger.atDebug().log("MCP Message - Session: %s", sessionId);

            // Read the JSON body
            final String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Validate MCP-Protocol-Version header for non-initialize requests
            final String protocolVersion = req.getHeader("MCP-Protocol-Version");
            if (protocolVersion != null && !McpProtocol.SUPPORTED_VERSIONS.contains(protocolVersion)
                    && !body.contains("\"method\":\"initialize\"")) {
                res.setHeader("Content-Type", "application/json");
                res.setStatus(400);
                res.getOutputStream()
                        .write(("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32600,"
                                + "\"message\":\"Unsupported MCP-Protocol-Version\"}}")
                                        .getBytes(StandardCharsets.UTF_8));
                return;
            }

            // Generate Session ID on initialize if somehow missing
            if (sessionId == null && body.contains("\"method\":\"initialize\"")) {
                sessionId = UUID.randomUUID().toString();
                logger.atDebug().log("Generated new session ID for initialization: %s", sessionId);
            }

            // Delegate to the MCP JSON-RPC Server
            final String responseJson = mcpProvider.getServer().handleMessage(body);

            // Send the response
            if (isTraditionalMode) {
                if (responseJson != null) {
                    final ConnectionContext ctx = activeSseConnections.get(sessionId);
                    if (ctx != null) {
                        final long   eventId = eventIdCounter.incrementAndGet();
                        final String sseData = "id: " + eventId + "\nevent: message\ndata: " + responseJson + "\n\n";
                        try {
                            ctx.res.getOutputStream().write(sseData.getBytes(StandardCharsets.UTF_8));
                            ctx.res.flush();
                        } catch (final IOException e) {
                            logger.atWarning().withException(e).log("Failed to write to SSE client for session: %s",
                                    sessionId);
                        }
                    } else {
                        logger.atWarning().log("SSE Connection not found for session: %s", sessionId);
                    }
                }
                res.setStatus(202);
            } else {
                if (responseJson != null) {
                    res.setHeader("Content-Type", "application/json");
                    // Reflect session ID
                    if (sessionId != null) {
                        res.setHeader("Mcp-Session-Id", sessionId);
                    }

                    // Negotiate Protocol Version at the transport level
                    final String requestedVersion  = req.getHeader("MCP-Protocol-Version");
                    final String negotiatedVersion = McpProtocol.negotiateVersion(requestedVersion);
                    res.setHeader("MCP-Protocol-Version", negotiatedVersion);

                    res.setStatus(200);
                    res.getOutputStream().write(responseJson.getBytes(StandardCharsets.UTF_8));
                } else {
                    // If it was a notification, return 202 Accepted (per spec)
                    res.setStatus(202);
                }
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Error handling MCP message");
            res.setStatus(500);
        }
    }

    private boolean isOriginAllowed(final String origin) {
        if (origin == null) {
            return true;
        }
        return origin.startsWith("http://localhost") || origin.startsWith("https://localhost")
                || origin.startsWith("http://127.0.0.1") || origin.startsWith("https://127.0.0.1")
                || origin.startsWith("http://[::1]") || origin.startsWith("https://[::1]") || "null".equals(origin);
    }

    private boolean acceptsEventStream(final HTTPRequest req) {
        final String accept = req.getHeader("Accept");
        return accept != null && accept.contains("text/event-stream");
    }
}