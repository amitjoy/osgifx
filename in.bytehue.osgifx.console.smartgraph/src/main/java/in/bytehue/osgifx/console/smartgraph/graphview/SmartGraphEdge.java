package in.bytehue.osgifx.console.smartgraph.graphview;

import in.bytehue.osgifx.console.smartgraph.graph.Edge;
import in.bytehue.osgifx.console.smartgraph.graph.Vertex;

/**
 * A graph edge visually connects two {@link Vertex} of type <code>V</code>.
 * <br>
 * Concrete edge implementations used by {@link SmartGraphPanel} should
 * implement this interface as this type is the only one exposed to the user.
 *
 * @param <E> Type stored in the underlying edge
 * @param <V> Type of connecting vertex
 *
 * @see Vertex
 * @see SmartGraphPanel
 */
public interface SmartGraphEdge<E, V> extends SmartStylableNode {

    /**
     * Returns the underlying (stored reference) graph edge.
     *
     * @return edge reference
     *
     * @see SmartGraphPanel
     */
    Edge<E, V> getUnderlyingEdge();

    /**
     * Returns the attached arrow of the edge, for styling purposes.
     *
     * The arrows are only used with directed graphs.
     *
     * @return arrow reference; null if does not exist.
     */
    SmartStylableNode getStylableArrow();

    /**
     * Returns the label node for further styling.
     *
     * @return the label node.
     */
    SmartStylableNode getStylableLabel();
}
