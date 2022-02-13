/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import java.util.Collection;
import java.util.Comparator;

import com.osgifx.console.smartgraph.graph.Graph;

import javafx.geometry.Point2D;

/**
 * Places vertices around a circle, ordered by the underlying
 * vertices {@code element.toString() value}.
 *
 * @see SmartPlacementStrategy
 */
public class SmartCircularSortedPlacementStrategy implements SmartPlacementStrategy {

    @Override
    public <V, E> void place(final double width, final double height, final Graph<V, E> theGraph,
            final Collection<? extends SmartGraphVertex<V>> vertices) {

        final Point2D center         = new Point2D(width / 2, height / 2);
        final int     N              = vertices.size();
        final double  angleIncrement = -360f / N;

        // place first vertex north position, others in clockwise manner
        boolean first = true;
        Point2D p     = null;
        for (final SmartGraphVertex<V> vertex : sort(vertices)) {
            if (first) {
                // verify smaller width and height.
                if (width > height) {
                    p = new Point2D(center.getX(), center.getY() - height / 2 + vertex.getRadius() * 2);
                } else {
                    p = new Point2D(center.getX(), center.getY() - width / 2 + vertex.getRadius() * 2);
                }
                first = false;
            } else {
                p = UtilitiesPoint2D.rotate(p, center, angleIncrement);
            }
            vertex.setPosition(p.getX(), p.getY());
        }
    }

    protected <V> Collection<? extends SmartGraphVertex<V>> sort(final Collection<? extends SmartGraphVertex<V>> vertices) {
        return vertices.stream().sorted(Comparator.comparing(v -> v.getUnderlyingVertex().element().toString().toLowerCase())).toList();
    }
}
