/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;

import com.osgifx.console.smartgraph.graph.DigraphEdgeList;
import com.osgifx.console.smartgraph.graph.Edge;
import com.osgifx.console.smartgraph.graph.Graph;
import com.osgifx.console.smartgraph.graph.Vertex;

public final class FxComponentGraph {

	Graph<ComponentVertex, String> graph;

	public FxComponentGraph(final Collection<GraphPath<ComponentVertex, DefaultEdge>> graphPaths) {
		graph = buildGraph(graphPaths);
	}

	public FxComponentGraph(final org.jgrapht.Graph<ComponentVertex, DefaultEdge> graph) {
		this.graph = buildGraph(graph);
	}

	public Graph<ComponentVertex, String> getGraph() {
		return graph;
	}

	private Graph<ComponentVertex, String> buildGraph(final org.jgrapht.Graph<ComponentVertex, DefaultEdge> jgraph) {
		final Graph<ComponentVertex, String> graph = new DigraphEdgeList<>();
		for (final DefaultEdge edge : jgraph.edgeSet()) {
			final var source = jgraph.getEdgeSource(edge);
			final var target = jgraph.getEdgeTarget(edge);
			if (graph.vertices().stream().noneMatch(v -> v.element().equals(source))) {
				graph.insertVertex(source);
			}
			if (graph.vertices().stream().noneMatch(v -> v.element().equals(target))) {
				graph.insertVertex(target);
			}
			if (!containsEdge(graph.edges(), source, target)) {
				graph.insertEdge(source, target, source + "->" + target);
			}
		}
		return graph;
	}

	private Graph<ComponentVertex, String> buildGraph(final Collection<GraphPath<ComponentVertex, DefaultEdge>> graphPaths) {
		final Graph<ComponentVertex, String> graph = new DigraphEdgeList<>();
		for (final GraphPath<ComponentVertex, DefaultEdge> path : graphPaths) {
			for (final DefaultEdge edge : path.getEdgeList()) {
				final var source = path.getGraph().getEdgeSource(edge);
				final var target = path.getGraph().getEdgeTarget(edge);
				if (graph.vertices().stream().noneMatch(v -> v.element().equals(source))) {
					graph.insertVertex(source);
				}
				if (graph.vertices().stream().noneMatch(v -> v.element().equals(target))) {
					graph.insertVertex(target);
				}
				if (!containsEdge(graph.edges(), source, target)) {
					graph.insertEdge(source, target, source + "->" + target);
				}
			}
		}
		return graph;
	}

	private boolean containsEdge(final Collection<Edge<String, ComponentVertex>> edges, final ComponentVertex source,
	        final ComponentVertex target) {
		var isEdgeFound = false;
		for (final Edge<String, ComponentVertex> edge : edges) {
			var isEdgeSourceFound = false;
			var isEdgeTargetFound = false;
			for (final Vertex<ComponentVertex> vertex : edge.vertices()) {
				if (isSameVertex(vertex, source)) {
					isEdgeSourceFound = true;
				}
			}
			for (final Vertex<ComponentVertex> vertex : edge.vertices()) {
				if (isSameVertex(vertex, target)) {
					isEdgeTargetFound = true;
				}
			}
			if (isEdgeSourceFound && isEdgeTargetFound) {
				isEdgeFound = true;
			}
		}
		return isEdgeFound;
	}

	private boolean isSameVertex(final Vertex<ComponentVertex> vertex, final ComponentVertex check) {
		return vertex.element().equals(check);
	}

}
