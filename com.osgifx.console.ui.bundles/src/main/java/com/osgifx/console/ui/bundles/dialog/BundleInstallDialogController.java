/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.ui.bundles.dialog;

import java.io.File;

import javax.inject.Inject;

import org.controlsfx.control.ToggleSwitch;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public final class BundleInstallDialogController {

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    private ThreadSynchronize          threadSync;
    @FXML
    private Button                     fileChooser;
    @FXML
    private ToggleSwitch               startBundleToggle;
    @FXML
    private GridPane                   installBundleDialogPane;
    @FXML
    private TextField                  startLevel;
    private File                       bundle;
    private final ObjectProperty<File> bundleProperty = new SimpleObjectProperty<>();

    private static final int DEFAULT_START_LEVEL = 10;

    @FXML
    public void initialize() {
        registerDragAndDropSupport();
        registerNumberValidationListener();
        logger.atDebug().log("FXML controller has been initialized");
    }

    @FXML
    private void chooseBundle(final ActionEvent event) {
        logger.atInfo().log("FXML controller 'chooseBundle(..)' event has been invoked");
        final var bundleChooser = new FileChooser();
        bundleChooser.getExtensionFilters().add(new ExtensionFilter("JAR Files", "*.jar"));
        bundle = bundleChooser.showOpenDialog(null);
        if (bundle != null) {
            bundleProperty.set(bundle);
            fileChooser.setText(bundle.getName());
            fileChooser.setTooltip(new Tooltip(bundle.getName()));
        }
    }

    public BundleInstallDTO getInstallDTO() {
        if (bundle == null) {
            return null;
        }
        final var sl    = startLevel.getText();
        var       level = DEFAULT_START_LEVEL;
        try {
            level = Integer.parseInt(sl);
        } catch (final Exception e) {
            logger.atError().withException(e).log(
                    "Start level value cannot be parsed. Fall back to default start level - %s", DEFAULT_START_LEVEL);
        }
        return new BundleInstallDTO(bundle, startBundleToggle.isSelected(), level);
    }

    private void registerDragAndDropSupport() {
        installBundleDialogPane.setOnDragOver(event -> {
            final var db         = event.getDragboard();
            final var isAccepted = db.getFiles().get(0).getName().toLowerCase().endsWith(".jar");
            if (db.hasFiles() && isAccepted) {
                installBundleDialogPane.setStyle("-fx-background-color: #C6C6C6");
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        installBundleDialogPane.setOnDragDropped(event -> {
            final var db      = event.getDragboard();
            var       success = false;
            if (db.hasFiles()) {
                success = true;
                // only get the first file from the list
                final var file = db.getFiles().get(0);
                threadSync.asyncExec(() -> {
                    fileChooser.setText(file.getName());
                    bundle = file;
                    bundleProperty.set(bundle);
                    fileChooser.setTooltip(new Tooltip(bundle.getName()));
                });
            }
            event.setDropCompleted(success);
            event.consume();
        });
        logger.atInfo().log("Registered drag and drop support");
    }

    private void registerNumberValidationListener() {
        startLevel.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                startLevel.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    public ObjectProperty<File> bundleProperty() {
        return bundleProperty;
    }

}
