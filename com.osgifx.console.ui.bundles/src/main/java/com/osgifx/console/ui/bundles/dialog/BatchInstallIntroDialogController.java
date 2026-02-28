/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;

public final class BatchInstallIntroDialogController {

    @Log
    @Inject
    private FluentLogger logger;
    @FXML
    private Button       directoryChooserBtn;

    private final ObjectProperty<File> directoryProperty = new SimpleObjectProperty<>();

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    @FXML
    public void chooseDirectory(final ActionEvent event) {
        logger.atInfo().log("Choosing directory for batch install");
        final var directoryChooser = new DirectoryChooser();
        final var location         = directoryChooser.showDialog(null);
        directoryProperty.set(location);
    }

    public ObjectProperty<File> directoryProperty() {
        return directoryProperty;
    }

    public File getDirectory() {
        return directoryProperty.get();
    }
}
