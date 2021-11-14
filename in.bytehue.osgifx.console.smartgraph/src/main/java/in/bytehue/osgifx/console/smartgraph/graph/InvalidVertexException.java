package in.bytehue.osgifx.console.smartgraph.graph;

/**
 * Error when using an invalid vertex in calls of methods in {@link Graph}
 * and {@link Digraph} implementations.
 *
 * @see Graph
 * @see Digraph
 */
public class InvalidVertexException extends RuntimeException {

    private static final long serialVersionUID = 991130343030333770L;

    public InvalidVertexException() {
        super("The vertex is invalid or does not belong to this graph.");
    }

    public InvalidVertexException(final String string) {
        super(string);
    }

}
