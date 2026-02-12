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

import java.util.Collection;
import java.util.HashSet;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;

import com.osgifx.console.smartgraph.graph.DigraphEdgeList;
import com.osgifx.console.smartgraph.graph.Graph;

public final class FxBundleGraph {

    Graph<BundleVertex, String> graph;

    public FxBundleGraph(final Collection<GraphPath<BundleVertex, DefaultEdge>> graphPaths) {
        graph = buildGraph(graphPaths);
    }

    public Graph<BundleVertex, String> getGraph() {
        return graph;
    }

    private Graph<BundleVertex, String> buildGraph(final Collection<GraphPath<BundleVertex, DefaultEdge>> graphPaths) {
        final Graph<BundleVertex, String> graph      = new DigraphEdgeList<>();
        final var                         addedEdges = new HashSet<String>();

        for (final GraphPath<BundleVertex, DefaultEdge> path : graphPaths) {
            for (final DefaultEdge edge : path.getEdgeList()) {
                final var source = path.getGraph().getEdgeSource(edge);
                final var target = path.getGraph().getEdgeTarget(edge);

                if (graph.vertices().stream().noneMatch(v -> v.element().equals(source))) {
                    graph.insertVertex(source);
                }
                if (graph.vertices().stream().noneMatch(v -> v.element().equals(target))) {
                    graph.insertVertex(target);
                }
                final var edgeId = source + "->" + target;
                if (!addedEdges.contains(edgeId)) {
                    graph.insertEdge(source, target, edgeId);
                    addedEdges.add(edgeId);
                }
            }
        }
        return graph;
    }

}
