package com.osgifx.console.ui.graph.rsa;

import java.util.Collection;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;

import com.osgifx.console.agent.dto.RemoteServiceDirection;
import com.osgifx.console.agent.dto.XRemoteServiceDTO;
import com.osgifx.console.ui.graph.GraphEdge;

public final class RsaGraphGenerator {

    private RsaGraphGenerator() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static Graph<RsaVertex, GraphEdge> generateRsaGraph(final Collection<XRemoteServiceDTO> remoteServices,
                                                               final String localFrameworkUUID) {
        final Graph<RsaVertex, GraphEdge> graph = new DefaultDirectedGraph<>(GraphEdge.class);

        // Add Local Framework Vertex
        final RsaVertex localVertex = new RsaVertex(localFrameworkUUID == null ? "local" : localFrameworkUUID,
                                                    "Local Framework");
        graph.addVertex(localVertex);

        // Create one vertex per endpoint (not per framework)
        int index = 0;
        for (final XRemoteServiceDTO dto : remoteServices) {
            index++;

            // Extract service interface name
            final String fullInterface = dto.objectClass != null && !dto.objectClass.isEmpty() ? dto.objectClass.get(0)
                    : "UnknownService";
            // Create readable label: "com.example.Service"
            final String label = fullInterface;

            // Use endpoint ID or index as unique identifier
            final String    vertexId       = dto.id != null ? dto.id : "endpoint-" + index;
            final RsaVertex endpointVertex = new RsaVertex(vertexId, label);
            graph.addVertex(endpointVertex);

            // Create edge with framework info
            final String frameworkLabel = "Framework: "
                    + RsaVertex.shortenUUID(dto.frameworkUUID != null ? dto.frameworkUUID : "unknown");

            if (dto.direction == RemoteServiceDirection.EXPORT) {
                // EXPORT: Local -> Remote
                graph.addEdge(localVertex, endpointVertex, new GraphEdge(frameworkLabel));
            } else if (dto.direction == RemoteServiceDirection.IMPORT) {
                // IMPORT: Remote -> Local
                graph.addEdge(endpointVertex, localVertex, new GraphEdge(frameworkLabel));
            }
        }

        return graph;
    }

}
