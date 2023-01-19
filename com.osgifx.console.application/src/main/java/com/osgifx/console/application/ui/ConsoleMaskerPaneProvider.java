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
package com.osgifx.console.application.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import org.controlsfx.control.MaskerPane;

import com.osgifx.console.ui.ConsoleMaskerPane;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public final class ConsoleMaskerPaneProvider implements ConsoleMaskerPane {

    private final MaskerPane maskerPane = new MaskerPane();

    @Override
    public void addTo(final Pane pane) {
        checkNotNull(pane, "Specified 'pane' cannot be null");
        if (pane instanceof final BorderPane p) {
            p.setCenter(maskerPane);
        } else {
            pane.getChildren().add(maskerPane);
        }
    }

    @Override
    public void setVisible(final boolean isVisible) {
        maskerPane.setVisible(isVisible);
    }

    @Override
    public boolean isVisible() {
        return maskerPane.isVisible();
    }

    @Override
    public Node getMaskerPane() {
        return maskerPane;
    }

}
