/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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

import com.osgifx.console.smartgraph.graph.Edge;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.shape.CubicCurve;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Concrete implementation of a curved edge. <br>
 * The edge binds its start point to the <code>outbound</code>
 * {@link SmartGraphVertexNode} center and its end point to the
 * <code>inbound</code> {@link SmartGraphVertexNode} center. As such, the curve
 * is updated automatically as the vertices move. <br>
 * Given there can be several curved edges connecting two vertices, when calling
 * the constructor
 * {@link #SmartGraphEdgeCurve(com.osgifx.console.smartgraph.graph.Edge, com.brunomnsilva.smartgraph.graphview.SmartGraphVertexNode, com.brunomnsilva.smartgraph.graphview.SmartGraphVertexNode, int) }
 * the <code>edgeIndex</code> can be specified as to create non-overlaping
 * curves.
 *
 * @param <E> Type stored in the underlying edge
 * @param <V> Type of connecting vertex
 */
public class SmartGraphEdgeCurve<E, V> extends CubicCurve implements SmartGraphEdgeBase<E, V> {

    private static final double MAX_EDGE_CURVE_ANGLE = 20;

    private final Edge<E, V> underlyingEdge;

    private final SmartGraphVertexNode<V> inbound;
    private final SmartGraphVertexNode<V> outbound;

    private SmartLabel attachedLabel = null;
    private SmartArrow attachedArrow = null;

    private double randomAngleFactor = 0;

    /* Styling proxy */
    private final SmartStyleProxy styleProxy;

    @SuppressWarnings("rawtypes")
    public SmartGraphEdgeCurve(final Edge<E, V> edge,
                               final SmartGraphVertexNode inbound,
                               final SmartGraphVertexNode outbound) {
        this(edge, inbound, outbound, 0);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public SmartGraphEdgeCurve(final Edge<E, V> edge,
                               final SmartGraphVertexNode inbound,
                               final SmartGraphVertexNode outbound,
                               final int edgeIndex) {
        this.inbound  = inbound;
        this.outbound = outbound;

        this.underlyingEdge = edge;

        styleProxy = new SmartStyleProxy(this);
        styleProxy.addStyleClass("edge");

        // bind start and end positions to vertices centers through properties
        startXProperty().bind(outbound.centerXProperty());
        startYProperty().bind(outbound.centerYProperty());
        endXProperty().bind(inbound.centerXProperty());
        endYProperty().bind(inbound.centerYProperty());

        // TODO: improve this solution taking into account even indices, etc.
        randomAngleFactor = edgeIndex == 0 ? 0 : 1.0 / edgeIndex; // Math.random();

        // update();
        enableListeners();
    }

    @Override
    public void setStyleClass(final String cssClass) {
        styleProxy.setStyleClass(cssClass);
    }

    @Override
    public void addStyleClass(final String cssClass) {
        styleProxy.addStyleClass(cssClass);
    }

    @Override
    public boolean removeStyleClass(final String cssClass) {
        return styleProxy.removeStyleClass(cssClass);
    }

    private void update() {
        if (inbound == outbound) {
            /* Make a loop using the control points proportional to the vertex radius */

            // TODO: take into account several "self-loops" with randomAngleFactor
            final var midpointX1 = outbound.getCenterX() - inbound.getRadius() * 5;
            final var midpointY1 = outbound.getCenterY() - inbound.getRadius() * 2;

            final var midpointX2 = outbound.getCenterX() + inbound.getRadius() * 5;
            final var midpointY2 = outbound.getCenterY() - inbound.getRadius() * 2;

            setControlX1(midpointX1);
            setControlY1(midpointY1);
            setControlX2(midpointX2);
            setControlY2(midpointY2);

        } else {
            /* Make a curved edge. The curve is proportional to the distance */
            final var midpointX = (outbound.getCenterX() + inbound.getCenterX()) / 2;
            final var midpointY = (outbound.getCenterY() + inbound.getCenterY()) / 2;

            var midpoint = new Point2D(midpointX, midpointY);

            final var startpoint = new Point2D(inbound.getCenterX(), inbound.getCenterY());
            final var endpoint   = new Point2D(outbound.getCenterX(), outbound.getCenterY());

            // TODO: improvement lower max_angle_placement according to distance between
            // vertices
            var angle = MAX_EDGE_CURVE_ANGLE;

            final var distance = startpoint.distance(endpoint);

            // TODO: remove "magic number" 1500 and provide a distance function for the
            // decreasing angle with distance
            angle = angle - distance / 1500 * angle;

            midpoint = UtilitiesPoint2D.rotate(midpoint, startpoint, -angle + randomAngleFactor * (angle - (-angle)));

            setControlX1(midpoint.getX());
            setControlY1(midpoint.getY());
            setControlX2(midpoint.getX());
            setControlY2(midpoint.getY());
        }

    }

    /*
     * With a curved edge we need to continuously update the control points. TODO:
     * Maybe we can achieve this solely with bindings.
     */
    private void enableListeners() {
        startXProperty().addListener((final ObservableValue<? extends Number> ov, final Number t, final Number t1) -> {
            update();
        });
        startYProperty().addListener((final ObservableValue<? extends Number> ov, final Number t, final Number t1) -> {
            update();
        });
        endXProperty().addListener((final ObservableValue<? extends Number> ov, final Number t, final Number t1) -> {
            update();
        });
        endYProperty().addListener((final ObservableValue<? extends Number> ov, final Number t, final Number t1) -> {
            update();
        });
    }

    @Override
    public void attachLabel(final SmartLabel label) {
        this.attachedLabel = label;
        label.xProperty().bind(controlX1Property().add(controlX2Property()).divide(2)
                .subtract(label.getLayoutBounds().getWidth() / 2));
        label.yProperty().bind(
                controlY1Property().add(controlY2Property()).divide(2).add(label.getLayoutBounds().getHeight() / 2));
    }

    @Override
    public SmartLabel getAttachedLabel() {
        return attachedLabel;
    }

    @Override
    public Edge<E, V> getUnderlyingEdge() {
        return underlyingEdge;
    }

    @Override
    public void attachArrow(final SmartArrow arrow) {
        this.attachedArrow = arrow;

        /* attach arrow to line's endpoint */
        arrow.translateXProperty().bind(endXProperty());
        arrow.translateYProperty().bind(endYProperty());

        /* rotate arrow around itself based on this line's angle */
        final var rotation = new Rotate();
        rotation.pivotXProperty().bind(translateXProperty());
        rotation.pivotYProperty().bind(translateYProperty());
        rotation.angleProperty().bind(UtilitiesBindings.toDegrees(UtilitiesBindings
                .atan2(endYProperty().subtract(controlY2Property()), endXProperty().subtract(controlX2Property()))));

        arrow.getTransforms().add(rotation);

        /* add translation transform to put the arrow touching the circle's bounds */
        final var t = new Translate(-outbound.getRadius(), 0);
        arrow.getTransforms().add(t);
    }

    @Override
    public SmartArrow getAttachedArrow() {
        return this.attachedArrow;
    }

    @Override
    public SmartStylableNode getStylableArrow() {
        return this.attachedArrow;
    }

    @Override
    public SmartStylableNode getStylableLabel() {
        return this.attachedLabel;
    }
}
