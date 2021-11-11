/*
 * The MIT License
 *
 * Copyright 2019 brunomnsilva@gmail.com.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.brunomnsilva.smartgraph.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ADT Graph implementation that stores a collection of edges (and vertices) and
 * where each edge contains the references for the vertices it connects.
 * <br>
 * Does not allow duplicates of stored elements through <b>equals</b> criteria.
 *
 * @param <V> Type of element stored at a vertex
 * @param <E> Type of element stored at an edge
 *
 * @author brunomnsilva
 */
public class GraphEdgeList<V, E> implements Graph<V, E> {

    /*
     * inner classes are defined at the end of the class, so are the auxiliary methods
     */
    private final Map<V, Vertex<V>>  vertices;
    private final Map<E, Edge<E, V>> edges;

    /**
     * Creates a empty graph.
     */
    public GraphEdgeList() {
        this.vertices = new HashMap<>();
        this.edges    = new HashMap<>();
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
    public Collection<Vertex<V>> vertices() {
        final List<Vertex<V>> list = new ArrayList<>(vertices.values());
        return list;
    }

    @Override
    public Collection<Edge<E, V>> edges() {
        final List<Edge<E, V>> list = new ArrayList<>(edges.values());
        return list;
    }

    @Override
    public Collection<Edge<E, V>> incidentEdges(final Vertex<V> v) throws InvalidVertexException {

        checkVertex(v);

        final List<Edge<E, V>> incidentEdges = new ArrayList<>();
        for (final Edge<E, V> edge : edges.values()) {

            if (((MyEdge) edge).contains(v)) {
                /* edge.vertices()[0] == v || edge.vertices()[1] == v */
                incidentEdges.add(edge);
            }

        }

        return incidentEdges;
    }

    @Override
    public Vertex<V> opposite(final Vertex<V> v, final Edge<E, V> e) throws InvalidVertexException, InvalidEdgeException {
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
    public synchronized boolean areAdjacent(final Vertex<V> u, final Vertex<V> v) throws InvalidVertexException {
        // we allow loops, so we do not check if u == v
        checkVertex(v);
        checkVertex(u);

        /* find and edge that contains both u and v */
        for (final Edge<E, V> edge : edges.values()) {
            if (((MyEdge) edge).contains(u) && ((MyEdge) edge).contains(v)) {
                return true;
            }
        }
        return false;
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
    public synchronized Edge<E, V> insertEdge(final Vertex<V> u, final Vertex<V> v, final E edgeElement)
            throws InvalidVertexException, InvalidEdgeException {

        if (existsEdgeWith(edgeElement)) {
            throw new InvalidEdgeException("There's already an edge with this element.");
        }

        final MyVertex outVertex = checkVertex(u);
        final MyVertex inVertex  = checkVertex(v);

        final MyEdge newEdge = new MyEdge(edgeElement, outVertex, inVertex);

        edges.put(edgeElement, newEdge);

        return newEdge;

    }

    @Override
    public synchronized Edge<E, V> insertEdge(final V vElement1, final V vElement2, final E edgeElement)
            throws InvalidVertexException, InvalidEdgeException {

        if (existsEdgeWith(edgeElement)) {
            throw new InvalidEdgeException("There's already an edge with this element.");
        }

        if (!existsVertexWith(vElement1)) {
            throw new InvalidVertexException("No vertex contains " + vElement1);
        }
        if (!existsVertexWith(vElement2)) {
            throw new InvalidVertexException("No vertex contains " + vElement2);
        }

        final MyVertex outVertex = vertexOf(vElement1);
        final MyVertex inVertex  = vertexOf(vElement2);

        final MyEdge newEdge = new MyEdge(edgeElement, outVertex, inVertex);

        edges.put(edgeElement, newEdge);

        return newEdge;

    }

    @Override
    public synchronized V removeVertex(final Vertex<V> v) throws InvalidVertexException {
        checkVertex(v);

        final V element = v.element();

        // remove incident edges
        final Iterable<Edge<E, V>> incidentEdges = incidentEdges(v);
        for (final Edge<E, V> edge : incidentEdges) {
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

    class MyVertex implements Vertex<V> {

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

    class MyEdge implements Edge<E, V> {

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

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public Vertex<V>[] vertices() {
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
