/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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

import javafx.scene.shape.Shape;

/**
 * This class acts as a proxy for styling of nodes.
 *
 * It essentially groups all the logic, avoiding code duplicate.
 *
 * Classes that have this behavior can delegate the method calls to an instance
 * of this class.
 */
public class SmartStyleProxy implements SmartStylableNode {

    private final Shape client;

    public SmartStyleProxy(final Shape client) {
        this.client = client;
    }

    @Override
    public void setStyle(final String css) {
        client.setStyle(css);
    }

    @Override
    public void setStyleClass(final String cssClass) {
        client.getStyleClass().clear();
        client.setStyle(null);
        client.getStyleClass().add(cssClass);
    }

    @Override
    public void addStyleClass(final String cssClass) {
        client.getStyleClass().add(cssClass);
    }

    @Override
    public boolean removeStyleClass(final String cssClass) {
        return client.getStyleClass().remove(cssClass);
    }

}
