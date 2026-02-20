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
package com.osgifx.console.ui.graph;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import org.osgi.framework.FrameworkUtil;

import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * A JavaFX component that wraps a {@link WebView} running a D3.js + dagre
 * SVG-based graph visualization. Capable of rendering 1,000+ nodes without
 * blocking the UI thread.
 * <p>
 * All graph data is injected via {@link WebEngine#executeScript(String)}.
 */
public final class WebGraphView extends BorderPane {

    private final WebView                 webView;
    private final WebEngine               webEngine;
    private final CompletableFuture<Void> readyFuture = new CompletableFuture<>();

    public WebGraphView() {
        webView   = new WebView();
        webView.setContextMenuEnabled(false);
        webEngine = webView.getEngine();
        setCenter(webView);

        // Track page load state
        webEngine.getLoadWorker().stateProperty().addListener((_, _, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                readyFuture.complete(null);
            } else if (newState == Worker.State.FAILED) {
                readyFuture.completeExceptionally(new RuntimeException("Failed to load graph page"));
            }
        });

        // Load the HTML page from bundle resources
        final var url = resolveGraphHtmlUrl();
        webEngine.load(url);
    }

    /**
     * Loads graph data into the D3.js visualization.
     *
     * @param elementsJson JSON array of nodes and edges
     */
    public void loadGraph(final String elementsJson) {
        executeWhenReady("loadGraph('" + escapeForJS(elementsJson) + "')");
    }

    /**
     * Switches the graph layout algorithm.
     *
     * @param layoutName one of: dagre, breadthfirst, cose, circle
     */
    public void setLayout(final String layoutName) {
        executeWhenReady("setLayout('" + layoutName + "')");
    }

    /** Zooms in on the graph center. */
    public void zoomIn() {
        executeWhenReady("zoomIn()");
    }

    /** Zooms out from the graph center. */
    public void zoomOut() {
        executeWhenReady("zoomOut()");
    }

    /** Fits the entire graph to the visible viewport. */
    public void fitToScreen() {
        executeWhenReady("fitGraph()");
    }

    /**
     * Highlights a specific node by its ID and centers the view on it.
     *
     * @param nodeId the graph node ID to highlight
     */
    public void highlightNode(final String nodeId) {
        executeWhenReady("highlightNodeById('" + escapeForJS(nodeId) + "')");
    }

    /**
     * Returns a Base64-encoded SVG of the current graph.
     *
     * @return SVG data as Base64 string, or empty string if no graph
     */
    public String exportPNG() {
        if (readyFuture.isDone()) {
            final var result = webEngine.executeScript("getGraphPNG()");
            return result != null ? result.toString() : "";
        }
        return "";
    }

    /**
     * Marks specific nodes as "selected" (e.g. highlighted in a special color).
     *
     * @param nodeIds the Cytoscape node IDs to mark
     */
    public void markSelectedNodes(final java.util.Collection<String> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            executeWhenReady("markSelectedNodes('[]')");
            return;
        }
        final var jsonArray = new StringBuilder("[");
        var first = true;
        for (final var id : nodeIds) {
            if (!first) jsonArray.append(",");
            jsonArray.append("\"").append(escapeForJS(id)).append("\"");
            first = false;
        }
        jsonArray.append("]");
        executeWhenReady("markSelectedNodes('" + escapeForJS(jsonArray.toString()) + "')");
    }

    private void executeWhenReady(final String script) {
        if (readyFuture.isDone()) {
            webEngine.executeScript(script);
        } else {
            readyFuture.thenRun(() -> webEngine.executeScript(script));
        }
    }

    private String resolveGraphHtmlUrl() {
        final var bundle = FrameworkUtil.getBundle(getClass());
        if (bundle != null) {
            final URL entry = bundle.getEntry("web/graph.html");
            if (entry != null) {
                return entry.toExternalForm();
            }
        }
        // Fallback for development / non-OSGi environments
        final var resource = getClass().getResource("/web/graph.html");
        if (resource != null) {
            return resource.toExternalForm();
        }
        throw new IllegalStateException("Cannot find web/graph.html in bundle or classpath");
    }

    private static String escapeForJS(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r");
    }

}
