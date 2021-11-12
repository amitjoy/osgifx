package in.bytehue.osgifx.console.ui.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.google.common.collect.Sets;

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

    public List<GraphPath<BundleVertex, DefaultEdge>> getTransitiveDependenciesOf(final Collection<XBundleDTO> bundles) {
        if (bundles.isEmpty()) {
            return Collections.emptyList();
        }
        final Set<BundleVertex> vertices = Sets.newHashSet();
        for (final XBundleDTO bundle : bundles) {
            final BundleVertex bundleVertex = new BundleVertex(bundle.symbolicName, bundle.id);
            if (graph.containsVertex(bundleVertex)) {
                final BundleVertex vertex = graph.vertexSet().stream().filter(bundleVertex::equals).findAny().orElse(null);
                vertices.add(vertex);
            }
        }
        final Set<BundleVertex> targetVertices = Sets.newHashSet(graph.vertexSet());
        targetVertices.removeIf(vertex -> exists(bundles, vertex));
        final AllDirectedPaths<BundleVertex, DefaultEdge> paths = new AllDirectedPaths<>(graph);
        return paths.getAllPaths(vertices, graph.vertexSet(), true, null);
    }

    private boolean exists(final Collection<XBundleDTO> bundles, final BundleVertex vertex) {
        for (final XBundleDTO dto : bundles) {
            if (dto.symbolicName.equals(vertex.symbolicName) && dto.id == vertex.id) {
                return true;
            }
        }
        return false;
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
