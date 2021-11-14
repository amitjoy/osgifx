package in.bytehue.osgifx.console.smartgraph.graphview;

import java.util.Collection;
import java.util.Random;

import in.bytehue.osgifx.console.smartgraph.graph.Graph;

/**
 * Scatters the vertices randomly.
 *
 * @see SmartPlacementStrategy
 */
public class SmartRandomPlacementStrategy implements SmartPlacementStrategy {

    @Override
    public <V, E> void place(final double width, final double height, final Graph<V, E> theGraph,
            final Collection<? extends SmartGraphVertex<V>> vertices) {

        final Random rand = new Random();

        for (final SmartGraphVertex<V> vertex : vertices) {

            final double x = rand.nextDouble() * width;
            final double y = rand.nextDouble() * height;

            vertex.setPosition(x, y);

        }
    }

}
