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
package com.osgifx.console.ui.graph.bundle;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_BUNDLES_TOPIC;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import com.google.common.collect.Sets;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.data.provider.DataProvider;

@Creatable
public final class RuntimeBundleGraph {

    @Inject
    private DataProvider dataProvider;

    private Graph<BundleVertex, DefaultEdge> graph;

    @PostConstruct
    public void init() {
        final var bundles = dataProvider.bundles();
        graph = buildGraph(bundles);
    }

    @Inject
    @Optional
    private void onUnderlyingDataUpdate(@EventTopic(DATA_RETRIEVED_BUNDLES_TOPIC) final String data) {
        init();
    }

    public Graph<BundleVertex, DefaultEdge> getAllBundlesThatRequire(final Collection<XBundleDTO> bundles,
                                                                     final boolean isTransitive) {
        final var vertices = getVertices(bundles, new EdgeReversedGraph<>(graph), isTransitive);
        return new AsSubgraph<>(graph, vertices);
    }

    public Graph<BundleVertex, DefaultEdge> getAllBundlesThatAreRequiredBy(final Collection<XBundleDTO> bundles,
                                                                           final boolean isTransitive) {
        final var vertices = getVertices(bundles, graph, isTransitive);
        return new AsSubgraph<>(graph, vertices);
    }

    private Set<BundleVertex> getVertices(final Collection<XBundleDTO> bundles,
                                          final Graph<BundleVertex, DefaultEdge> graph,
                                          final boolean isTransitive) {
        final Set<BundleVertex> vertices = Sets.newHashSet();
        for (final XBundleDTO bundle : bundles) {
            final var vertex = new BundleVertex(bundle.symbolicName, bundle.id);
            if (graph.containsVertex(vertex)) {
                vertices.add(vertex);
                if (isTransitive) {
                    new BreadthFirstIterator<>(graph, vertex).forEachRemaining(vertices::add);
                } else {
                    // Direct neighbors only
                    for (final DefaultEdge edge : graph.outgoingEdgesOf(vertex)) {
                        vertices.add(graph.getEdgeTarget(edge));
                    }
                }
            }
        }
        return vertices;
    }

    private Graph<BundleVertex, DefaultEdge> buildGraph(final List<XBundleDTO> bundles) {
        final Graph<BundleVertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        bundles.forEach(b -> {
            final var source = new BundleVertex(b.symbolicName, b.id);
            graph.addVertex(source);
            if (b.wiredBundlesAsRequirer != null) {
                b.wiredBundlesAsRequirer.forEach(d -> {
                    final var target = new BundleVertex(d.symbolicName, d.id);
                    graph.addVertex(target);
                    graph.addEdge(source, target);
                });
            }
        });
        return graph;
    }

}
