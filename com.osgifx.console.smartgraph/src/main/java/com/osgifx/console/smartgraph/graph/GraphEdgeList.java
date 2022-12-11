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
package com.osgifx.console.smartgraph.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * ADT Graph implementation that stores a collection of edges (and vertices) and
 * where each edge contains the references for the vertices it connects. <br>
 * Does not allow duplicates of stored elements through <b>equals</b> criteria.
 *
 * @param <V> Type of element stored at a vertex
 * @param <E> Type of element stored at an edge
 */
public class GraphEdgeList<V, E> implements Graph<V, E> {

    /*
     * inner classes are defined at the end of the class, so are the auxiliary
     * methods
     */
    private final Map<V, Vertex<V>>  vertices;
    private final Map<E, Edge<E, V>> edges;

    /**
     * Creates a empty graph.
     */
    public GraphEdgeList() {
        this.vertices = Maps.newHashMap();
        this.edges    = Maps.newHashMap();
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
        return Lists.newArrayList(vertices.values());
    }

    @Override
    public Collection<Edge<E, V>> edges() {
        return Lists.newArrayList(edges.values());
    }

    @Override
    public Collection<Edge<E, V>> incidentEdges(final Vertex<V> v) throws InvalidVertexException {

        checkVertex(v);

        final List<Edge<E, V>> incidentEdges = Lists.newArrayList();
        for (final Edge<E, V> edge : edges.values()) {

            if (((MyEdge) edge).contains(v)) {
                /* edge.vertices()[0] == v || edge.vertices()[1] == v */
                incidentEdges.add(edge);
            }

        }

        return incidentEdges;
    }

    @Override
    public Vertex<V> opposite(final Vertex<V> v,
                              final Edge<E, V> e) throws InvalidVertexException, InvalidEdgeException {
        checkVertex(v);
        final var edge = checkEdge(e);

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

        final var newVertex = new MyVertex(vElement);

        vertices.put(vElement, newVertex);

        return newVertex;
    }

    @Override
    public synchronized Edge<E, V> insertEdge(final Vertex<V> u,
                                              final Vertex<V> v,
                                              final E edgeElement) throws InvalidVertexException, InvalidEdgeException {

        if (existsEdgeWith(edgeElement)) {
            throw new InvalidEdgeException("There's already an edge with this element.");
        }

        final var outVertex = checkVertex(u);
        final var inVertex  = checkVertex(v);

        final var newEdge = new MyEdge(edgeElement, outVertex, inVertex);

        edges.put(edgeElement, newEdge);

        return newEdge;

    }

    @Override
    public synchronized Edge<E, V> insertEdge(final V vElement1,
                                              final V vElement2,
                                              final E edgeElement) throws InvalidVertexException, InvalidEdgeException {

        if (existsEdgeWith(edgeElement)) {
            throw new InvalidEdgeException("There's already an edge with this element.");
        }

        if (!existsVertexWith(vElement1)) {
            throw new InvalidVertexException("No vertex contains " + vElement1);
        }
        if (!existsVertexWith(vElement2)) {
            throw new InvalidVertexException("No vertex contains " + vElement2);
        }

        final var outVertex = vertexOf(vElement1);
        final var inVertex  = vertexOf(vElement2);

        final var newEdge = new MyEdge(edgeElement, outVertex, inVertex);

        edges.put(edgeElement, newEdge);

        return newEdge;

    }

    @Override
    public synchronized V removeVertex(final Vertex<V> v) throws InvalidVertexException {
        checkVertex(v);

        final var element = v.element();

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

        final var element = e.element();
        edges.remove(e.element());

        return element;
    }

    @Override
    public V replace(final Vertex<V> v, final V newElement) throws InvalidVertexException {
        if (existsVertexWith(newElement)) {
            throw new InvalidVertexException("There's already a vertex with this element.");
        }

        final var vertex = checkVertex(v);

        final var oldElement = vertex.element;
        vertex.element = newElement;

        return oldElement;
    }

    @Override
    public E replace(final Edge<E, V> e, final E newElement) throws InvalidEdgeException {
        if (existsEdgeWith(newElement)) {
            throw new InvalidEdgeException("There's already an edge with this element.");
        }

        final var edge = checkEdge(e);

        final var oldElement = edge.element;
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
        final var sb = new StringBuilder(String.format("Graph with %d vertices and %d edges:%n", numVertices(),
                numEdges()));

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

        @SuppressWarnings("unchecked")
        @Override
        public Vertex<V>[] vertices() {
            final var vertices = new Vertex[2];
            vertices[0] = vertexOutbound;
            vertices[1] = vertexInbound;

            return vertices;
        }

        @Override
        public String toString() {
            return "Edge{{" + element + "}, vertexOutbound=" + vertexOutbound.toString() + ", vertexInbound="
                    + vertexInbound.toString() + '}';
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
