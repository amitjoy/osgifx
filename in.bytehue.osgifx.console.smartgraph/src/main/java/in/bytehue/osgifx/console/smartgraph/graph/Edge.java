package in.bytehue.osgifx.console.smartgraph.graph;

/**
 * An edge connects two {@link Vertex} of type <code>V</code> and stores
 * an element of type <code>E</code>.
 *
 * The edge may be used in oriented and non-oriented graphs.
 *
 * @param <E> Type of value stored in the edge
 * @param <V> Type of value stored in the vertices that this edge connects.
 *
 * @see Graph
 * @see Digraph
 */
public interface Edge<E, V> {

    /**
     * Returns the element stored in the edge.
     *
     * @return stored element
     */
    E element();

    /**
     * Returns and array of size 2, with references for both vertices at the ends
     * of an edge.
     *
     * In a {@link Digraph} the reference at {@code vertices()[0]} must be that
     * of the <i>outbound vertex</i> and at {@code vertices()[1]} that of the <i>inbound</i>
     * vertex.
     *
     * @return an array of length 2, containing the vertices at both ends.
     */
    Vertex<V>[] vertices();

}
