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

        // Add Local Vertex
        final RsaVertex localVertex = new RsaVertex(localFrameworkUUID == null ? "Local Framework"
                : localFrameworkUUID);
        graph.addVertex(localVertex);

        remoteServices.forEach(dto -> {
            final String    remoteUUID   = dto.frameworkUUID != null ? dto.frameworkUUID : "Unknown Framework";
            final RsaVertex remoteVertex = new RsaVertex(remoteUUID);
            graph.addVertex(remoteVertex);

            final String label = dto.objectClass != null ? String.join(", ", dto.objectClass) : "Service";

            if (dto.direction == RemoteServiceDirection.EXPORT) {
                graph.addEdge(localVertex, remoteVertex, new GraphEdge(label));
            } else if (dto.direction == RemoteServiceDirection.IMPORT) {
                graph.addEdge(remoteVertex, localVertex, new GraphEdge(label));
            }
        });

        return graph;
    }

}
