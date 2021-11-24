package in.bytehue.osgifx.console.ui.graph;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import in.bytehue.osgifx.console.smartgraph.graph.Edge;
import in.bytehue.osgifx.console.smartgraph.graph.Graph;
import in.bytehue.osgifx.console.smartgraph.graph.Vertex;

public final class GraphHelper {

    private GraphHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static <V> org.jgrapht.Graph<V, DefaultEdge> toJGraphT(final Graph<V, String> smartGraph) {
        final org.jgrapht.Graph<V, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        smartGraph.vertices().forEach(v -> graph.addVertex(v.element()));
        for (final Edge<String, V> edge : smartGraph.edges()) {
            final Vertex<V>[] vertices = edge.vertices();
            if (vertices[0] != null && vertices[1].element() != null) {
                graph.addEdge(vertices[0].element(), vertices[1].element());
            }
        }
        return graph;
    }

    public static String generateDotFileName(final String prefix) {
        final String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
        return "OSGi.fx_" + prefix + "_" + timeStamp + ".dot";
    }

}
