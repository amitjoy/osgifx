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

import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * A shape of an arrow to be attached to a {@link SmartGraphEdge}.
 */
public class SmartArrow extends Path implements SmartStylableNode {

    /* Styling proxy */
    private final SmartStyleProxy styleProxy;

    /**
     * Constructor
     *
     * @param size determines the size of the arrow (side of the triangle in pixels)
     */
    public SmartArrow(final double size) {
        /* Create this arrow shape */
        getElements().add(new MoveTo(0, 0));
        getElements().add(new LineTo(-size, size));
        getElements().add(new MoveTo(0, 0));
        getElements().add(new LineTo(-size, -size));

        /* Add the corresponding css class */
        styleProxy = new SmartStyleProxy(this);
        styleProxy.addStyleClass("arrow");
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

}
