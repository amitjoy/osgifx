/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.google.common.base.Functions;
import com.google.common.collect.Sets;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleInfoDTO;

public final class RuntimeBundleGraph {

    private final Map<String, XBundleDTO>          bundles;
    private final Graph<BundleVertex, DefaultEdge> providerGraph;
    private final Graph<BundleVertex, DefaultEdge> requirerGraph;

    public RuntimeBundleGraph(final List<XBundleDTO> bundles) {
        this.bundles  = processBundles(bundles);
        providerGraph = buildGraph(bundles, Strategy.PROVIDER);
        requirerGraph = buildGraph(bundles, Strategy.REQUIRER);
    }

    public Collection<GraphPath<BundleVertex, DefaultEdge>> getAllBundlesThatRequire(final Collection<XBundleDTO> bundles) {
        return getDirectedPaths(bundles, Strategy.PROVIDER);
    }

    public Collection<GraphPath<BundleVertex, DefaultEdge>> getAllBundlesThatAreRequiredBy(final Collection<XBundleDTO> bundles) {
        return getDirectedPaths(bundles, Strategy.REQUIRER);
    }

    public Collection<GraphPath<BundleVertex, DefaultEdge>> getDirectedPaths(final Collection<XBundleDTO> bundles,
            final Strategy strategy) {
        if (bundles.isEmpty()) {
            return List.of();
        }
        final Graph<BundleVertex, DefaultEdge> graph;
        if (strategy == Strategy.REQUIRER) {
            graph = requirerGraph;
        } else {
            graph = providerGraph;
        }
        final Set<BundleVertex> vertices = Sets.newHashSet();
        for (final XBundleDTO bundle : bundles) {
            final BundleVertex bundleVertex = new BundleVertex(bundle.symbolicName, bundle.id);
            if (graph.containsVertex(bundleVertex)) {
                final BundleVertex vertex = graph.vertexSet().stream().filter(bundleVertex::equals).findAny().orElse(null);
                vertices.add(vertex);
            }
        }
        final AllDirectedPaths<BundleVertex, DefaultEdge> paths = new AllDirectedPaths<>(graph);
        return paths.getAllPaths(vertices, graph.vertexSet(), true, null);
    }

    private Map<String, XBundleDTO> processBundles(final List<XBundleDTO> bundles) {
        return bundles.stream().collect(toMap(b -> b.symbolicName + ":" + b.id, Functions.identity()));
    }

    private Graph<BundleVertex, DefaultEdge> buildGraph(final List<XBundleDTO> bundles, final Strategy strategy) {
        final Graph<BundleVertex, DefaultEdge> graph            = new DefaultDirectedGraph<>(DefaultEdge.class);
        final Set<String>                      processedBundles = Sets.newHashSet();
        bundles.forEach(b -> prepareGraph(b, graph, strategy, processedBundles));
        return graph;
    }

    private void prepareGraph(final XBundleDTO bundle, final Graph<BundleVertex, DefaultEdge> graph, final Strategy strategy,
            final Set<String> processedBundles) {
        final List<XBundleInfoDTO> vertexBundles;
        if (strategy == Strategy.REQUIRER) {
            vertexBundles = bundle.wiredBundlesAsRequirer;
        } else {
            vertexBundles = bundle.wiredBundlesAsProvider;
        }
        final BundleVertex source = new BundleVertex(bundle.symbolicName, bundle.id);
        if (processedBundles.contains(source.toString())) {
            return;
        }
        final boolean isSourceVertexAdded = graph.addVertex(source);
        if (isSourceVertexAdded) {
            processedBundles.add(source.toString());
        }
        if (vertexBundles == null || vertexBundles.isEmpty()) {
            return;
        }
        for (final XBundleInfoDTO b : vertexBundles) {
            final BundleVertex target              = new BundleVertex(b.symbolicName, b.id);
            final boolean      isTargetVertexAdded = graph.addVertex(target);
            if (isTargetVertexAdded) {
                processedBundles.add(source.toString());
            }
            graph.addEdge(source, target);
            final XBundleDTO dto = bundles.get(target.toString());
            prepareGraph(dto, graph, strategy, processedBundles);
        }
    }

    private enum Strategy {
        PROVIDER,
        REQUIRER
    }

}
