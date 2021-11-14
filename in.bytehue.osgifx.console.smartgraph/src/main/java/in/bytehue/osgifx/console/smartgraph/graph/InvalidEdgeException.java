package in.bytehue.osgifx.console.smartgraph.graph;

/**
 * Error when using an invalid edge in calls of methods in {@link Graph}
 * and {@link Digraph} implementations.
 *
 * @see Graph
 * @see Digraph
 */
public class InvalidEdgeException extends RuntimeException {

    private static final long serialVersionUID = 6725706014265791454L;

    public InvalidEdgeException() {
        super("The edge is invalid or does not belong to this graph.");
    }

    public InvalidEdgeException(final String string) {
        super(string);
    }

}
