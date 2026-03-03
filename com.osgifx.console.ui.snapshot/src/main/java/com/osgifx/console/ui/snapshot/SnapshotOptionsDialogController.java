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
package com.osgifx.console.ui.snapshot;

import com.osgifx.console.util.fx.OptionsDialogHelper;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class SnapshotOptionsDialogController {

    @FXML
    private Label bundleCountLabel;
    @FXML
    private Label serviceCountLabel;
    @FXML
    private Label estimatedSizeLabel;
    @FXML
    private Label compressedSizeLabel;
    @FXML
    private HBox  transportBox;
    @FXML
    private Label warningLabel;
    @FXML
    private VBox  optionsContainer;
    @FXML
    private VBox  infoBox;

    private final ToggleGroup                                          toggleGroup            = new ToggleGroup();
    private final ObjectProperty<SnapshotOptionsDialog.SnapshotOption> selectedOptionProperty = new SimpleObjectProperty<>();

    public void initData(final SnapshotOptionsDialog.SnapshotStatistics stats) {
        bundleCountLabel.setText(String.format("%,d", stats.bundleCount));
        serviceCountLabel.setText(String.format("%,d", stats.serviceCount));
        estimatedSizeLabel
                .setText("~" + OptionsDialogHelper.formatSize(stats.estimatedUncompressedSize) + " uncompressed");
        compressedSizeLabel.setText("~" + OptionsDialogHelper.formatSize(stats.estimatedCompressedSize));

        transportBox.getChildren()
                .add(OptionsDialogHelper.createTransportLabel(stats.isMqttTransport, stats.estimatedCompressedSize));
        OptionsDialogHelper.setupWarningLabel(warningLabel, stats.estimatedUncompressedSize, "snapshot");

        createOptions(stats);
    }

    private void createOptions(final SnapshotOptionsDialog.SnapshotStatistics stats) {
        if (stats.handler != null) {
            final var handlerOption = OptionsDialogHelper.createHandlerOption(toggleGroup,
                    SnapshotOptionsDialog.SnapshotOption.USE_HANDLER, stats.handler);
            optionsContainer.getChildren().add(handlerOption);
        }

        if (stats.rpcAvailable) {
            final var rpcOption = OptionsDialogHelper.createRpcOption(toggleGroup,
                    SnapshotOptionsDialog.SnapshotOption.USE_RPC, stats.isMqttTransport, stats.estimatedCompressedSize,
                    "snapshot");
            optionsContainer.getChildren().add(rpcOption);
        }

        final var localOption = OptionsDialogHelper.createLocalOption(toggleGroup,
                SnapshotOptionsDialog.SnapshotOption.STORE_LOCALLY);
        optionsContainer.getChildren().add(localOption);

        if (stats.handler == null) {
            infoBox.setManaged(true);
            infoBox.setVisible(true);
        }

        if (!toggleGroup.getToggles().isEmpty()) {
            toggleGroup.selectToggle(toggleGroup.getToggles().get(0));
            updateSelectedOption();
        }

        toggleGroup.selectedToggleProperty().addListener((_, _, _) -> updateSelectedOption());
    }

    private void updateSelectedOption() {
        final var selectedToggle = toggleGroup.getSelectedToggle();
        if (selectedToggle != null) {
            selectedOptionProperty.set((SnapshotOptionsDialog.SnapshotOption) selectedToggle.getUserData());
        }
    }

    public ObjectProperty<SnapshotOptionsDialog.SnapshotOption> selectedOptionProperty() {
        return selectedOptionProperty;
    }

    public SnapshotOptionsDialog.SnapshotOption getSelectedOption() {
        return selectedOptionProperty.get();
    }
}
