package in.bytehue.osgifx.console.smartgraph.graph;

/**
 * A vertex contains an element of type <code>V</code> and is used both in
 * graphs and digraphs.
 *
 * @param <V> Type of value stored in the vertex.
 *
 * @see Graph
 * @see Digraph
 */
public interface Vertex<V> {

    /**
     * Returns the element stored in the vertex.
     *
     * @return stored element
     */
    V element();
}
