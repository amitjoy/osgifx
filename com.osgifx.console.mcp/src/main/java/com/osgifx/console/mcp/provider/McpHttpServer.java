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

import static com.osgifx.console.mcp.provider.McpHttpServer.CONDITION_ID_VALUE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.condition.Condition.CONDITION_ID;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.osgifx.console.mcp.provider.McpHttpServer.Configuration;

import io.fusionauth.http.HTTPMethod;
import io.fusionauth.http.server.HTTPHandler;
import io.fusionauth.http.server.HTTPListenerConfiguration;
import io.fusionauth.http.server.HTTPRequest;
import io.fusionauth.http.server.HTTPResponse;
import io.fusionauth.http.server.HTTPServer;

@Designate(ocd = Configuration.class)
@Component(service = McpHttpServer.class, immediate = true)
@SatisfyingConditionTarget("(" + CONDITION_ID + "=" + CONDITION_ID_VALUE + ")")
public class McpHttpServer {

    public static final String CONDITION_ID_VALUE = "osgi.fx.mcp";

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
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
        startServer(config);
        // Register notification sender to broadcast to all SSE clients
        mcpProvider.getServer().setNotificationSender(this::broadcast);
    }

    @Deactivate
    void deactivate() {
        stopServer();
    }

    @SuppressWarnings("resource")
    private void startServer(final Configuration config) {
        if (httpServer != null) {
            return;
        }

        final HTTPHandler handler = (req, res) -> {
            final String path = req.getPath();
            logger.atInfo().log("Incoming Request: %s %s", req.getMethod(), path);

            // Streamable HTTP Endpoint (GET/POST /mcp)
            if ("/mcp".equals(path) || path.startsWith("/mcp?")) {
                // Handle Preflight OPTIONS request for CORS (if needed later)
                // For now, implementing standard interactions.

                // POST: Message Handling (Stateless)
                if (HTTPMethod.POST == req.getMethod()) {
                    handleMessage(req, res);
                    return;
                }

                // GET: Optional SSE Streaming
                if (HTTPMethod.GET == req.getMethod() && "text/event-stream".equals(req.getHeader("Accept"))) {
                    handleSseConnection(req, res);
                    return;
                }
            }

            logger.atError().log("404 Not Found: %s %s", req.getMethod(), path);
            res.setStatus(404);
        };

        final HTTPListenerConfiguration listenerConfig = new HTTPListenerConfiguration(config.port());

        httpServer = new HTTPServer().withListener(listenerConfig).withHandler(handler);
        httpServer.start();

        logger.atInfo().log("MCP HTTP Server started at http://localhost:%d/mcp", config.port());
    }

    private void stopServer() {
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
                // We cannot remove the client here easily as it's managed by the handler thread lock.
                // The handler thread will likely exit if writing fails eventually.
            }
        });
    }

    private void handleSseConnection(final HTTPRequest req, final HTTPResponse res) {
        try {
            // Log Streamable HTTP headers for debugging/context
            String sessionId   = req.getHeader("Mcp-Session-Id");
            String lastEventId = req.getHeader("Last-Event-ID");
            logger.atDebug().log("SSE Connection Request - Session: %s, Last-Event-ID: %s", sessionId, lastEventId);

            // Standard SSE Headers
            res.setHeader("Content-Type", "text/event-stream");
            res.setHeader("Cache-Control", "no-cache");
            res.setHeader("Connection", "keep-alive");

            // Reflect Protocol Version if requested
            String protocolVersion = req.getHeader("MCP-Protocol-Version");
            if (protocolVersion != null) {
                res.setHeader("MCP-Protocol-Version", protocolVersion); // Or negotiate supported version
            }

            // If Session ID is missing, we could generate one (optional in new spec for stateless,
            // but good for tracking if we add state later).
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
                // In Streamable HTTP, we might want to return this, but SSE is a stream.
                // We can't easily validly set a response header for the *stream* if headers are already flushed?
                // But strictly, we haven't flushed yet.
                res.setHeader("Mcp-Session-Id", sessionId);
            }

            res.setStatus(200);
            res.flush(); // Send headers

            // In Streamable HTTP, we don't need to send the 'endpoint' handshake event.
            // We just keep the connection open for any potential server-initiated notifications.

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

    private void handleMessage(final HTTPRequest req, final HTTPResponse res) {
        try {
            // Log session context
            String sessionId = req.getHeader("Mcp-Session-Id");
            logger.atDebug().log("MCP Message - Session: %s", sessionId);

            // Read the JSON body
            final String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Generate Session ID if missing (especially on 'initialize')
            if (sessionId == null && body.contains("\"method\":\"initialize\"")) { // Simple check, ideally parse JSON
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

                // Reflect Protocol Version if requested
                String protocolVersion = req.getHeader("MCP-Protocol-Version");
                if (protocolVersion != null) {
                    res.setHeader("MCP-Protocol-Version", protocolVersion);
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