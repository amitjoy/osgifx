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

    // Tracks active SSE connections (useful for cleanup or future notifications)
    private final Map<Object, Boolean> activeConnections = new ConcurrentHashMap<>();

    @Activate
    void activate(final Configuration config) {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
        startServer(config);
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

            // SSE Endpoint (GET /sse) - Handshake
            if (("/sse".equals(path) || path.startsWith("/sse?")) && HTTPMethod.GET == req.getMethod()) {
                handleSseConnection(req, res);
                return;
            }

            // Message Endpoint (POST /messages OR POST /sse)
            final boolean isMessagePath = "/messages".equals(path) || path.startsWith("/messages?")
                    || "/sse".equals(path) || path.startsWith("/sse?");
            if (isMessagePath && HTTPMethod.POST == req.getMethod()) {
                handleMessage(req, res);
                return;
            }

            logger.atError().log("404 Not Found: %s %s", req.getMethod(), path);
            res.setStatus(404);
        };

        final HTTPListenerConfiguration listenerConfig = new HTTPListenerConfiguration(config.port());

        httpServer = new HTTPServer().withListener(listenerConfig).withHandler(handler);
        httpServer.start();

        logger.atInfo().log("MCP HTTP Server started at http://localhost:%d/sse", config.port());
    }

    private void stopServer() {
        if (httpServer != null) {
            httpServer.close();
            httpServer = null;
        }
        // Release any blocked threads waiting on SSE connections
        synchronized (activeConnections) {
            activeConnections.notifyAll();
        }
        logger.atInfo().log("MCP HTTP Server stopped");
    }

    private void handleSseConnection(final HTTPRequest req, final HTTPResponse res) {
        try {
            // Standard SSE Headers
            res.setHeader("Content-Type", "text/event-stream");
            res.setHeader("Cache-Control", "no-cache");
            res.setHeader("Connection", "keep-alive");
            res.setStatus(200);

            // CRITICAL: Send the 'endpoint' event immediately.
            // This tells the AI client: "I am ready. Send POST requests to /messages"
            // Therefore, generate a session ID to satisfy clients that require it.
            final String sessionId = UUID.randomUUID().toString();
            final String initEvent = "event: endpoint\ndata: /messages?sessionId=" + sessionId + "\n\n";
            res.getOutputStream().write(initEvent.getBytes(StandardCharsets.UTF_8));
            res.flush();

            // Keep connection open by blocking
            final Object connectionLock = new Object();
            activeConnections.put(connectionLock, Boolean.TRUE);

            try {
                synchronized (connectionLock) {
                    // Wait indefinitely until server stop or client disconnect
                    connectionLock.wait();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                activeConnections.remove(connectionLock);
            }
        } catch (final IOException e) {
            // Client likely disconnected, which is normal behavior
            logger.atDebug().log("SSE Client disconnected");
        }
    }

    private void handleMessage(final HTTPRequest req, final HTTPResponse res) {
        try {
            // Read the JSON body
            // Note: FusionAuth doesn't auto-parse body, so read all bytes
            final String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // Delegate to the MCP JSON-RPC Server
            final String responseJson = mcpProvider.getServer().handleMessage(body);

            // Send the response
            if (responseJson != null) {
                res.setHeader("Content-Type", "application/json");
                res.setStatus(200);
                res.getOutputStream().write(responseJson.getBytes(StandardCharsets.UTF_8));
            } else {
                // If it was a notification, return 204 No Content
                res.setStatus(204);
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Error handling MCP message");
            res.setStatus(500);
        }
    }
}