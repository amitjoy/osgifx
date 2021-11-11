/*
 * The MIT License
 *
 * Copyright 2019 brunomnsilva.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.brunomnsilva.smartgraph.graphview;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.brunomnsilva.smartgraph.graph.Graph;

import javafx.geometry.Point2D;

/**
 * Places vertices around a circle, ordered by the underlying
 * vertices {@code element.toString() value}.
 *
 * @see SmartPlacementStrategy
 *
 * @author brunomnsilva
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

    protected <V> Collection<SmartGraphVertex<V>> sort(final Collection<? extends SmartGraphVertex<V>> vertices) {
        return vertices.stream().sorted(Comparator.comparing(v -> v.getUnderlyingVertex().element().toString().toLowerCase()))
                .collect(Collectors.toList());
    }
}
