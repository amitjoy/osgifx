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
package com.osgifx.console.smartgraph.containers;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

public class ContentResizerPane extends Pane {

    private final Node           content;
    private final DoubleProperty resizeFActor = new SimpleDoubleProperty(1);

    public ContentResizerPane(final Node content) {
        this.content = content;

        getChildren().add(content);

        final Scale scale = new Scale(1, 1);
        content.getTransforms().add(scale);

        resizeFActor.addListener((final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) -> {
            scale.setX(newValue.doubleValue());
            scale.setY(newValue.doubleValue());
            requestLayout();
        });
    }

    @Override
    protected void layoutChildren() {
        final Pos    pos           = Pos.TOP_LEFT;
        final double width         = getWidth();
        final double height        = getHeight();
        final double top           = getInsets().getTop();
        final double right         = getInsets().getRight();
        final double left          = getInsets().getLeft();
        final double bottom        = getInsets().getBottom();
        final double contentWidth  = (width - left - right) / resizeFActor.get();
        final double contentHeight = (height - top - bottom) / resizeFActor.get();
        layoutInArea(content, left, top, contentWidth, contentHeight, 0, null, pos.getHpos(), pos.getVpos());
    }

    public final Double getResizeFactor() {
        return resizeFActor.get();
    }

    public final void setResizeFactor(final Double resizeFactor) {
        resizeFActor.set(resizeFactor);
    }

    public final DoubleProperty resizeFactorProperty() {
        return resizeFActor;
    }
}
