package in.bytehue.osgifx.console.ui.graph;

import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;

public final class RuntimeGraph {

    private final Graph<BundleVertex, DefaultEdge> graph;

    public RuntimeGraph(final List<XBundleDTO> bundles) {
        graph = buildGraph(bundles);
    }

    public Graph<BundleVertex, DefaultEdge> getGraph() {
        return graph;
    }

    public List<GraphPath<BundleVertex, DefaultEdge>> getAllPaths(final Set<BundleVertex> sourceVertices,
            final Set<BundleVertex> targetVertices) {
        final AllDirectedPaths<BundleVertex, DefaultEdge> paths = new AllDirectedPaths<>(graph);
        return paths.getAllPaths(sourceVertices, targetVertices, false, null);
    }

    private Graph<BundleVertex, DefaultEdge> buildGraph(final List<XBundleDTO> bundles) {
        final Graph<BundleVertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        bundles.forEach(b -> prepareGraph(b, graph));
        return graph;
    }

    private void prepareGraph(final XBundleDTO bundle, final Graph<BundleVertex, DefaultEdge> graph) {
        final List<XBundleInfoDTO> requiredBundles = bundle.wiredBundlesAsRequirer;
        final BundleVertex         source          = new BundleVertex(bundle.symbolicName, bundle.id);
        graph.addVertex(source);
        if (requiredBundles == null || requiredBundles.isEmpty()) {
            return;
        }
        for (final XBundleInfoDTO b : requiredBundles) {
            final BundleVertex target = new BundleVertex(b.symbolicName, b.id);
            graph.addVertex(target);
            graph.addEdge(source, target);

            final XBundleDTO dto = new XBundleDTO();
            dto.id           = b.id;
            dto.symbolicName = b.symbolicName;

            prepareGraph(dto, graph);
        }
    }

}
