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
package com.osgifx.console.ui.batchinstall.dialog;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.controlsfx.control.ListSelectionView;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.ui.batchinstall.dialog.BatchInstallDialog.ArtifactDTO;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;

public final class BatchInstallDialogController {

    @Log
    @Inject
    private FluentLogger                   logger;
    @FXML
    private ListSelectionView<ArtifactDTO> artifactsList;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
        artifactsList.setCellFactory(_ -> new ListCell<>() {
            @Override
            public void updateItem(final ArtifactDTO artifact, final boolean empty) {
                super.updateItem(artifact, empty);

                if (artifact == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(artifact.file().getName());
                    setGraphic(new ImageView(this.getClass().getResource(
                            artifact.isConfiguration() ? "/graphic/images/config.png" : "/graphic/images/bundle.png")
                            .toString()));
                }
            }
        });
    }

    public void initArtifacts(final File directory) {
        List<ArtifactDTO> artifacts;
        if (directory.exists()) {
            final var files = FileUtils.listFiles(directory, new String[] { "jar", "json" }, false);
            artifacts = files.stream().map(this::createArtifact).toList();
        } else {
            artifacts = List.of();
        }
        artifactsList.getSourceItems().clear();
        artifactsList.getTargetItems().clear();
        artifactsList.getSourceItems().addAll(artifacts);
    }

    private ArtifactDTO createArtifact(final File file) {
        return new ArtifactDTO(file, file.getName().endsWith(".json"));
    }

    public List<ArtifactDTO> getSelectedArtifacts() {
        return artifactsList.getTargetItems();
    }

    public ObjectProperty<ObservableList<ArtifactDTO>> targetItemsProperty() {
        return artifactsList.targetItemsProperty();
    }

}
