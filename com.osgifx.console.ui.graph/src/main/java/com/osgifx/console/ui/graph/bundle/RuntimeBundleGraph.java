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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.osgifx.console.ui.graph.GraphEdge;

@Creatable
public final class RuntimeBundleGraph {

    @Inject
    private DataProvider dataProvider;

    private Graph<BundleVertex, DefaultEdge> graph;
    private Graph<BundleVertex, GraphEdge>   serviceGraph;

    @PostConstruct
    public void init() {
        final var bundles = dataProvider.bundles();
        graph        = buildGraph(bundles);
        serviceGraph = buildServiceGraph(bundles);
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

    public Graph<BundleVertex, GraphEdge> getAllBundlesThatUseServicesFrom(final Collection<XBundleDTO> bundles,
                                                                           final boolean isTransitive) {
        final var vertices = getVertices(bundles, new EdgeReversedGraph<>(serviceGraph), isTransitive);
        return new AsSubgraph<>(serviceGraph, vertices);
    }

    public Graph<BundleVertex, GraphEdge> getAllBundlesThatProvideServicesTo(final Collection<XBundleDTO> bundles,
                                                                             final boolean isTransitive) {
        final var vertices = getVertices(bundles, serviceGraph, isTransitive);
        return new AsSubgraph<>(serviceGraph, vertices);
    }

    private <E extends DefaultEdge> Set<BundleVertex> getVertices(final Collection<XBundleDTO> bundles,
                                                                  final Graph<BundleVertex, E> graph,
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
                    for (final E edge : graph.outgoingEdgesOf(vertex)) {
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

    private Graph<BundleVertex, GraphEdge> buildServiceGraph(final List<XBundleDTO> bundles) {
        final Graph<BundleVertex, GraphEdge> graph            = new DefaultDirectedGraph<>(GraphEdge.class);
        final Map<Long, BundleVertex>        serviceProviders = new HashMap<>();

        // 1. Create vertices and map service IDs to providers
        bundles.forEach(b -> {
            final var vertex = new BundleVertex(b.symbolicName, b.id);
            graph.addVertex(vertex);
            if (b.registeredServices != null) {
                b.registeredServices.forEach(s -> serviceProviders.put(s.id, vertex));
            }
        });

        // 2. Create edges from consumers to providers
        bundles.forEach(consumer -> {
            if (consumer.usedServices != null) {
                final var consumerVertex = new BundleVertex(consumer.symbolicName, consumer.id);
                // Collect all services consumed by this bundle from specific providers
                final Map<BundleVertex, List<String>> edgesToCreate = new HashMap<>();

                consumer.usedServices.forEach(s -> {
                    final var providerVertex = serviceProviders.get(s.id);
                    // Avoid self-references to prevent graph loops
                    if (providerVertex != null && !providerVertex.equals(consumerVertex)) {
                        edgesToCreate.computeIfAbsent(providerVertex, _ -> new ArrayList<>()).add(s.objectClass);
                    }
                });

                // Create aggregated edges
                edgesToCreate.forEach((provider, classes) -> {
                    final var label = classes.stream().distinct().sorted().collect(Collectors.joining(", "));
                    final var edge  = new GraphEdge(label);
                    graph.addEdge(consumerVertex, provider, edge);
                });
            }
        });
        return graph;
    }

}
