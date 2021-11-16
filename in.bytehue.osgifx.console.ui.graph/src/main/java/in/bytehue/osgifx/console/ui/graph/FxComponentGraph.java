package in.bytehue.osgifx.console.ui.graph;

import java.util.Collection;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;

import in.bytehue.osgifx.console.smartgraph.graph.DigraphEdgeList;
import in.bytehue.osgifx.console.smartgraph.graph.Edge;
import in.bytehue.osgifx.console.smartgraph.graph.Graph;
import in.bytehue.osgifx.console.smartgraph.graph.Vertex;

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
            final ComponentVertex source = jgraph.getEdgeSource(edge);
            final ComponentVertex target = jgraph.getEdgeTarget(edge);
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
                final ComponentVertex source = path.getGraph().getEdgeSource(edge);
                final ComponentVertex target = path.getGraph().getEdgeTarget(edge);
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
        boolean isEdgeFound = false;
        for (final Edge<String, ComponentVertex> edge : edges) {
            boolean isEdgeSourceFound = false;
            boolean isEdgeTargetFound = false;
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
