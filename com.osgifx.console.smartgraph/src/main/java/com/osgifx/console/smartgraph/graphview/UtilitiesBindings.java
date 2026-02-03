/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.smartgraph.graphview;

import static javafx.beans.binding.Bindings.createDoubleBinding;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;

/**
 * Some {@link Math} operations implemented as bindings.
 */
public class UtilitiesBindings {

    private UtilitiesBindings() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    /**
     * Binding for {@link java.lang.Math#atan2(double, double)}
     *
     * @param y the ordinate coordinate
     * @param x the abscissa coordinate
     * @return the <i>theta</i> component of the point (<i>r</i>,&nbsp;<i>theta</i>)
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
     * @return the measurement of the angle {@code angrad} in degrees.
     */
    public static DoubleBinding toDegrees(final ObservableDoubleValue angrad) {
        return createDoubleBinding(() -> Math.toDegrees(angrad.get()), angrad);
    }
}
