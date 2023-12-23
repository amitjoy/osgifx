/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.osgifx.console.smartgraph.graph.Edge;
import com.osgifx.console.smartgraph.graph.Graph;

public final class GraphHelper {

    private GraphHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static <V> org.jgrapht.Graph<V, DefaultEdge> toJGraphT(final Graph<V, String> smartGraph) {
        final org.jgrapht.Graph<V, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        smartGraph.vertices().forEach(v -> graph.addVertex(v.element()));
        for (final Edge<String, V> edge : smartGraph.edges()) {
            final var vertices = edge.vertices();
            if (vertices[0] != null && vertices[1].element() != null) {
                graph.addEdge(vertices[0].element(), vertices[1].element());
            }
        }
        return graph;
    }

    public static String generateDotFileName(final String prefix) {
        final var timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
        return "OSGi.fx_" + prefix + "_" + timeStamp + ".dot";
    }

}
