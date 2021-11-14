package in.bytehue.osgifx.console.smartgraph.graphview;

import static javafx.beans.binding.Bindings.createDoubleBinding;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;

/**
 * Some {@link Math} operations implemented as bindings.
 */
public class UtilitiesBindings {

    /**
     * Binding for {@link java.lang.Math#atan2(double, double)}
     *
     * @param y the ordinate coordinate
     * @param x the abscissa coordinate
     * @return the <i>theta</i> component of the point
     *         (<i>r</i>,&nbsp;<i>theta</i>)
     *         in polar coordinates that corresponds to the point
     *         (<i>x</i>,&nbsp;<i>y</i>) in Cartesian coordinates.
     */
    public static DoubleBinding atan2(final ObservableDoubleValue y, final ObservableDoubleValue x) {
        return createDoubleBinding(() -> Math.atan2(y.get(), x.get()), y, x);
    }

    /**
     * Binding for {@link java.lang.Math#toDegrees(double)}
     *
     * @param angrad an angle, in radians
     * @return the measurement of the angle {@code angrad}
     *         in degrees.
     */
    public static DoubleBinding toDegrees(final ObservableDoubleValue angrad) {
        return createDoubleBinding(() -> Math.toDegrees(angrad.get()), angrad);
    }
}
