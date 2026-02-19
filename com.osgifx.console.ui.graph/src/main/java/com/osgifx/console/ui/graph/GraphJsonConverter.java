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

import java.util.function.Function;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import com.google.common.collect.Lists;

/**
 * Converts a JGraphT {@link Graph} into a JSON string compatible with the
 * D3.js-based graph visualization page ({@code graph.html}).
 */
public final class GraphJsonConverter {

    private GraphJsonConverter() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    /**
     * Converts a JGraphT directed graph into a JSON array of nodes and edges.
     * <p>
     * Output format:
     * 
     * <pre>
     * [
     *   { "data": { "id": "...", "label": "..." } },                    // nodes
     *   { "data": { "id": "e0", "source": "...", "target": "..." } }    // edges
     * ]
     * </pre>
     *
     * @param graph the JGraphT graph
     * @param idMapper maps a vertex to a unique ID string
     * @param labelMapper maps a vertex to a display label
     * @param <V> vertex type
     * @return JSON string of graph elements
     */
    public static <V> String toJson(final Graph<V, DefaultEdge> graph,
                                    final Function<V, String> idMapper,
                                    final Function<V, String> labelMapper) {
        final var elements  = Lists.<String> newArrayList();
        var       edgeIndex = 0;

        // Add nodes
        for (final V vertex : graph.vertexSet()) {
            final var id    = escapeJson(idMapper.apply(vertex));
            final var label = escapeJson(labelMapper.apply(vertex));
            elements.add("{ \"data\": { \"id\": \"" + id + "\", \"label\": \"" + label + "\" } }");
        }

        // Add edges
        for (final DefaultEdge edge : graph.edgeSet()) {
            final var source = escapeJson(idMapper.apply(graph.getEdgeSource(edge)));
            final var target = escapeJson(idMapper.apply(graph.getEdgeTarget(edge)));
            elements.add("{ \"data\": { \"id\": \"e" + edgeIndex++ + "\", \"source\": \"" + source
                    + "\", \"target\": \"" + target + "\" } }");
        }

        return "[\n" + String.join(",\n", elements) + "\n]";
    }

    private static String escapeJson(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
                "\\t");
    }

}
