package in.bytehue.osgifx.console.ui.graph;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
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

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;

public final class RuntimeGraph {

    private final Set<String>                      processedBundles;
    private final Map<String, XBundleDTO>          bundles;
    private final Graph<BundleVertex, DefaultEdge> graph;

    public RuntimeGraph(final List<XBundleDTO> bundles) {
        processedBundles = Sets.newHashSet();
        this.bundles     = processBundles(bundles);
        graph            = buildGraph(bundles);
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
        final AllDirectedPaths<BundleVertex, DefaultEdge> paths = new AllDirectedPaths<>(graph);
        return paths.getAllPaths(vertices, graph.vertexSet(), true, null);
    }

    private Map<String, XBundleDTO> processBundles(final List<XBundleDTO> bundles) {
        return bundles.stream().collect(toMap(b -> b.symbolicName + ":" + b.id, Functions.identity()));
    }

    private Graph<BundleVertex, DefaultEdge> buildGraph(final List<XBundleDTO> bundles) {
        final Graph<BundleVertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        bundles.forEach(b -> prepareGraph(b, graph));
        return graph;
    }

    private void prepareGraph(final XBundleDTO bundle, final Graph<BundleVertex, DefaultEdge> graph) {
        final List<XBundleInfoDTO> requiredBundles = bundle.wiredBundlesAsRequirer;
        final BundleVertex         source          = new BundleVertex(bundle.symbolicName, bundle.id);
        if (processedBundles.contains(source.toString())) {
            return;
        }
        final boolean isSourceVertexAdded = graph.addVertex(source);
        if (isSourceVertexAdded) {
            processedBundles.add(source.toString());
        }
        if (requiredBundles == null || requiredBundles.isEmpty()) {
            return;
        }
        for (final XBundleInfoDTO b : requiredBundles) {
            final BundleVertex target              = new BundleVertex(b.symbolicName, b.id);
            final boolean      isTargetVertexAdded = graph.addVertex(target);
            if (isTargetVertexAdded) {
                processedBundles.add(source.toString());
            }
            graph.addEdge(source, target);
            final XBundleDTO dto = bundles.get(target.toString());
            prepareGraph(dto, graph);
        }
    }

}
