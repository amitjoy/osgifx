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
package com.osgifx.console.ui.logs;

import static org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK;

import org.controlsfx.control.SegmentedButton;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.di.FXMLBuilder;
import org.eclipse.fx.ui.di.FXMLLoader;
import org.eclipse.fx.ui.di.FXMLLoaderFactory;

import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;

@SuppressWarnings("deprecation")
public final class StarterFxController {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    @FXMLLoader
    private FXMLLoaderFactory loader;
    @FXML
    private BorderPane        mainPane;
    @FXML
    private ToggleButton      logsViewButton;
    @FXML
    private ToggleButton      configurationsViewButton;
    @FXML
    private SegmentedButton   logsActionTypeButton;

    @FXML
    public void initialize() {
        initLogsActionTypeButton();
        initButtons();
        showLogEvents();
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void initLogsActionTypeButton() {
        logsActionTypeButton.getStyleClass().add(STYLE_CLASS_DARK);
        logsActionTypeButton.getToggleGroup().selectedToggleProperty().addListener((obsVal, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });
    }

    private void initButtons() {
        logsViewButton.setOnMouseClicked(e -> showLogEvents());
        configurationsViewButton.setOnMouseClicked(e -> showConfigurations());
    }

    private void showLogEvents() {
        final var node = loadFXML("/fxml/tab-content-for-logs.fxml");
        mainPane.setCenter(node);
        logger.atDebug().log("Loaded log events");
    }

    private void showConfigurations() {
        final var node = loadFXML("/fxml/tab-content-for-configurations.fxml");
        mainPane.setCenter(node);
        logger.atDebug().log("Loaded log configurations");
    }

    private Node loadFXML(final String resourceName) {
        final FXMLBuilder<Node> builder = loader.loadBundleRelative(resourceName);
        try {
            return builder.load();
        } catch (final Exception e) {
            return null;
        }
    }

}
