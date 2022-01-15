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

import com.osgifx.console.smartgraph.graph.Edge;

import javafx.scene.shape.Line;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Implementation of a straight line edge.
 *
 * @param <E> Type stored in the underlying edge
 * @param <V> Type of connecting vertex
 */
public class SmartGraphEdgeLine<E, V> extends Line implements SmartGraphEdgeBase<E, V> {

    private final Edge<E, V> underlyingEdge;

    @SuppressWarnings({ "rawtypes", "unused" })
    private final SmartGraphVertexNode inbound;
    @SuppressWarnings("rawtypes")
    private final SmartGraphVertexNode outbound;

    private SmartLabel attachedLabel = null;
    private SmartArrow attachedArrow = null;

    /* Styling proxy */
    private final SmartStyleProxy styleProxy;

    @SuppressWarnings("rawtypes")
    public SmartGraphEdgeLine(final Edge<E, V> edge, final SmartGraphVertexNode inbound, final SmartGraphVertexNode outbound) {
        if (inbound == null || outbound == null) {
            throw new IllegalArgumentException("Cannot connect null vertices.");
        }

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

    @Override
    public void attachLabel(final SmartLabel label) {
        this.attachedLabel = label;
        label.xProperty().bind(startXProperty().add(endXProperty()).divide(2).subtract(label.getLayoutBounds().getWidth() / 2));
        label.yProperty().bind(startYProperty().add(endYProperty()).divide(2).add(label.getLayoutBounds().getHeight() / 1.5));
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
        final Rotate rotation = new Rotate();
        rotation.pivotXProperty().bind(translateXProperty());
        rotation.pivotYProperty().bind(translateYProperty());
        rotation.angleProperty().bind(UtilitiesBindings
                .toDegrees(UtilitiesBindings.atan2(endYProperty().subtract(startYProperty()), endXProperty().subtract(startXProperty()))));

        arrow.getTransforms().add(rotation);

        /* add translation transform to put the arrow touching the circle's bounds */
        final Translate t = new Translate(-outbound.getRadius(), 0);
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
