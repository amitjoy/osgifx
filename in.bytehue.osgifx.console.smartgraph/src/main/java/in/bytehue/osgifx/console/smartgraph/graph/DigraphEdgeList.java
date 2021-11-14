package in.bytehue.osgifx.console.smartgraph.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a digraph that adheres to the {@link Digraph} interface.
 * <br>
 * Does not allow duplicates of stored elements through <b>equals</b> criteria.
 * <br>
 *
 * @param <V> Type of element stored at a vertex
 * @param <E> Type of element stored at an edge
 */
public class DigraphEdgeList<V, E> implements Digraph<V, E> {

    /*
     * inner classes are defined at the end of the class, so are the auxiliary methods
     */
    private final Map<V, Vertex<V>>  vertices;
    private final Map<E, Edge<E, V>> edges;

    public DigraphEdgeList() {
        this.vertices = new HashMap<>();
        this.edges    = new HashMap<>();
    }

    @Override
    public synchronized Collection<Edge<E, V>> incidentEdges(final Vertex<V> inbound) throws InvalidVertexException {
        checkVertex(inbound);

        final List<Edge<E, V>> incidentEdges = new ArrayList<>();
        for (final Edge<E, V> edge : edges.values()) {

            if (((MyEdge) edge).getInbound() == inbound) {
                incidentEdges.add(edge);
            }
        }
        return incidentEdges;
    }

    @Override
    public synchronized Collection<Edge<E, V>> outboundEdges(final Vertex<V> outbound) throws InvalidVertexException {
        checkVertex(outbound);

        final List<Edge<E, V>> outboundEdges = new ArrayList<>();
        for (final Edge<E, V> edge : edges.values()) {

            if (((MyEdge) edge).getOutbound() == outbound) {
                outboundEdges.add(edge);
            }
        }
        return outboundEdges;
    }

    @Override
    public boolean areAdjacent(final Vertex<V> outbound, final Vertex<V> inbound) throws InvalidVertexException {
        // we allow loops, so we do not check if outbound == inbound
        checkVertex(outbound);
        checkVertex(inbound);

        /* find and edge that goes outbound ---> inbound */
        for (final Edge<E, V> edge : edges.values()) {
            if (((MyEdge) edge).getOutbound() == outbound && ((MyEdge) edge).getInbound() == inbound) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized Edge<E, V> insertEdge(final Vertex<V> outbound, final Vertex<V> inbound, final E edgeElement)
            throws InvalidVertexException, InvalidEdgeException {
        if (existsEdgeWith(edgeElement)) {
            throw new InvalidEdgeException("There's already an edge with this element.");
        }

        final MyVertex outVertex = checkVertex(outbound);
        final MyVertex inVertex  = checkVertex(inbound);

        final MyEdge newEdge = new MyEdge(edgeElement, outVertex, inVertex);

        edges.put(edgeElement, newEdge);

        return newEdge;
    }

    @Override
    public synchronized Edge<E, V> insertEdge(final V outboundElement, final V inboundElement, final E edgeElement)
            throws InvalidVertexException, InvalidEdgeException {
        if (existsEdgeWith(edgeElement)) {
            throw new InvalidEdgeException("There's already an edge with this element.");
        }

        if (!existsVertexWith(outboundElement)) {
            throw new InvalidVertexException("No vertex contains " + outboundElement);
        }
        if (!existsVertexWith(inboundElement)) {
            throw new InvalidVertexException("No vertex contains " + inboundElement);
        }

        final MyVertex outVertex = vertexOf(outboundElement);
        final MyVertex inVertex  = vertexOf(inboundElement);

        final MyEdge newEdge = new MyEdge(edgeElement, outVertex, inVertex);

        edges.put(edgeElement, newEdge);

        return newEdge;
    }

    @Override
    public int numVertices() {
        return vertices.size();
    }

    @Override
    public int numEdges() {
        return edges.size();
    }

    @Override
    public synchronized Collection<Vertex<V>> vertices() {
        final List<Vertex<V>> list = new ArrayList<>();
        vertices.values().forEach(v -> {
            list.add(v);
        });
        return list;
    }

    @Override
    public synchronized Collection<Edge<E, V>> edges() {
        final List<Edge<E, V>> list = new ArrayList<>();
        edges.values().forEach(e -> {
            list.add(e);
        });
        return list;
    }

    @Override
    public synchronized Vertex<V> opposite(final Vertex<V> v, final Edge<E, V> e) throws InvalidVertexException, InvalidEdgeException {
        checkVertex(v);
        final MyEdge edge = checkEdge(e);

        if (!edge.contains(v)) {
            return null; /* this edge does not connect vertex v */
        }

        if (edge.vertices()[0] == v) {
            return edge.vertices()[1];
        }
        return edge.vertices()[0];

    }

    @Override
    public synchronized Vertex<V> insertVertex(final V vElement) throws InvalidVertexException {
        if (existsVertexWith(vElement)) {
            throw new InvalidVertexException("There's already a vertex with this element.");
        }

        final MyVertex newVertex = new MyVertex(vElement);

        vertices.put(vElement, newVertex);

        return newVertex;
    }

    @Override
    public synchronized V removeVertex(final Vertex<V> v) throws InvalidVertexException {
        checkVertex(v);

        final V element = v.element();

        // remove incident edges
        final Collection<Edge<E, V>> inOutEdges = incidentEdges(v);
        inOutEdges.addAll(outboundEdges(v));

        for (final Edge<E, V> edge : inOutEdges) {
            edges.remove(edge.element());
        }

        vertices.remove(v.element());

        return element;
    }

    @Override
    public synchronized E removeEdge(final Edge<E, V> e) throws InvalidEdgeException {
        checkEdge(e);

        final E element = e.element();
        edges.remove(e.element());

        return element;
    }

    @Override
    public V replace(final Vertex<V> v, final V newElement) throws InvalidVertexException {
        if (existsVertexWith(newElement)) {
            throw new InvalidVertexException("There's already a vertex with this element.");
        }

        final MyVertex vertex = checkVertex(v);

        final V oldElement = vertex.element;
        vertex.element = newElement;

        return oldElement;
    }

    @Override
    public E replace(final Edge<E, V> e, final E newElement) throws InvalidEdgeException {
        if (existsEdgeWith(newElement)) {
            throw new InvalidEdgeException("There's already an edge with this element.");
        }

        final MyEdge edge = checkEdge(e);

        final E oldElement = edge.element;
        edge.element = newElement;

        return oldElement;
    }

    private MyVertex vertexOf(final V vElement) {
        for (final Vertex<V> v : vertices.values()) {
            if (v.element().equals(vElement)) {
                return (MyVertex) v;
            }
        }
        return null;
    }

    private boolean existsVertexWith(final V vElement) {
        return vertices.containsKey(vElement);
    }

    private boolean existsEdgeWith(final E edgeElement) {
        return edges.containsKey(edgeElement);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(String.format("Graph with %d vertices and %d edges:\n", numVertices(), numEdges()));

        sb.append("--- Vertices: \n");
        for (final Vertex<V> v : vertices.values()) {
            sb.append("\t").append(v.toString()).append("\n");
        }
        sb.append("\n--- Edges: \n");
        for (final Edge<E, V> e : edges.values()) {
            sb.append("\t").append(e.toString()).append("\n");
        }
        return sb.toString();
    }

    private class MyVertex implements Vertex<V> {

        V element;

        public MyVertex(final V element) {
            this.element = element;
        }

        @Override
        public V element() {
            return this.element;
        }

        @Override
        public String toString() {
            return "Vertex{" + element + '}';
        }
    }

    private class MyEdge implements Edge<E, V> {

        E         element;
        Vertex<V> vertexOutbound;
        Vertex<V> vertexInbound;

        public MyEdge(final E element, final Vertex<V> vertexOutbound, final Vertex<V> vertexInbound) {
            this.element        = element;
            this.vertexOutbound = vertexOutbound;
            this.vertexInbound  = vertexInbound;
        }

        @Override
        public E element() {
            return this.element;
        }

        public boolean contains(final Vertex<V> v) {
            return vertexOutbound == v || vertexInbound == v;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Vertex<V>[] vertices() {
            @SuppressWarnings("rawtypes")
            final Vertex[] vertices = new Vertex[2];
            vertices[0] = vertexOutbound;
            vertices[1] = vertexInbound;

            return vertices;
        }

        @Override
        public String toString() {
            return "Edge{{" + element + "}, vertexOutbound=" + vertexOutbound.toString() + ", vertexInbound=" + vertexInbound.toString()
                    + '}';
        }

        public Vertex<V> getOutbound() {
            return vertexOutbound;
        }

        public Vertex<V> getInbound() {
            return vertexInbound;
        }
    }

    /**
     * Checks whether a given vertex is valid and belongs to this graph
     *
     * @param v
     * @return
     * @throws InvalidVertexException
     */
    private MyVertex checkVertex(final Vertex<V> v) throws InvalidVertexException {
        if (v == null) {
            throw new InvalidVertexException("Null vertex.");
        }

        MyVertex vertex;
        try {
            vertex = (MyVertex) v;
        } catch (final ClassCastException e) {
            throw new InvalidVertexException("Not a vertex.");
        }

        if (!vertices.containsKey(vertex.element)) {
            throw new InvalidVertexException("Vertex does not belong to this graph.");
        }

        return vertex;
    }

    private MyEdge checkEdge(final Edge<E, V> e) throws InvalidEdgeException {
        if (e == null) {
            throw new InvalidEdgeException("Null edge.");
        }

        MyEdge edge;
        try {
            edge = (MyEdge) e;
        } catch (final ClassCastException ex) {
            throw new InvalidVertexException("Not an adge.");
        }

        if (!edges.containsKey(edge.element)) {
            throw new InvalidEdgeException("Edge does not belong to this graph.");
        }

        return edge;
    }

}
