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
package com.osgifx.console.ui.heap;

import com.osgifx.console.agent.spi.LargePayloadHandler;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class HeapdumpOptionsDialogController {

    @FXML
    private Label heapSizeLabel;
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
    private final ObjectProperty<HeapdumpOptionsDialog.HeapdumpOption> selectedOptionProperty = new SimpleObjectProperty<>();

    public void initData(final HeapdumpOptionsDialog.HeapdumpStatistics stats) {
        heapSizeLabel.setText("~" + formatSize(stats.estimatedUncompressedSize));
        compressedSizeLabel.setText("~" + formatSize(stats.estimatedCompressedSize));

        createTransportLabel(stats);

        if (stats.estimatedUncompressedSize > 100_000_000L) {
            warningLabel.setText("⚠️ This heapdump is large. Please choose an appropriate option:");
            warningLabel.setManaged(true);
            warningLabel.setVisible(true);
        }

        createOptions(stats);
    }

    private void createTransportLabel(final HeapdumpOptionsDialog.HeapdumpStatistics stats) {
        final var label = new Label();
        label.setStyle("-fx-font-weight: bold;");

        if (stats.isMqttTransport) {
            final var mqttLimit = 250_000_000L;
            if (stats.estimatedCompressedSize > mqttLimit) {
                label.setText("MQTT (max 250 MB) ❌");
                label.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            } else {
                label.setText("MQTT (max 250 MB) ✓");
                label.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        } else {
            final var socketLimit = 200_000_000L;
            if (stats.estimatedCompressedSize > socketLimit) {
                label.setText("Socket (max 200 MB) ⚠️");
                label.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
            } else {
                label.setText("Socket (max 2 GB) ✓");
                label.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        }
        transportBox.getChildren().add(label);
    }

    private void createOptions(final HeapdumpOptionsDialog.HeapdumpStatistics stats) {
        if (stats.handler != null) {
            final var handlerOption = createHandlerOption(stats.handler);
            optionsContainer.getChildren().add(handlerOption);
        }

        if (stats.rpcAvailable) {
            final var rpcOption = createRpcOption(stats);
            optionsContainer.getChildren().add(rpcOption);
        }

        final var localOption = createLocalOption();
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

    private VBox createHandlerOption(final LargePayloadHandler handler) {
        final var box = new VBox(5);

        final var radio = new RadioButton("Use Handler: \"" + handler.getHandlerName() + "\"");
        radio.setToggleGroup(toggleGroup);
        radio.setUserData(HeapdumpOptionsDialog.HeapdumpOption.USE_HANDLER);
        radio.setStyle("-fx-font-weight: bold;");

        final var descLabel = new Label(handler.getDescription());
        descLabel.setStyle("-fx-text-fill: #6b7280;");
        descLabel.setWrapText(true);
        VBox.setMargin(descLabel, new Insets(0, 0, 0, 25));

        final var maxSizeLabel = new Label("Max size: " + formatSize(handler.getMaxPayloadSize()));
        maxSizeLabel.setStyle("-fx-text-fill: #6b7280;");
        VBox.setMargin(maxSizeLabel, new Insets(0, 0, 0, 25));

        box.getChildren().addAll(radio, descLabel, maxSizeLabel);
        return box;
    }

    private VBox createRpcOption(final HeapdumpOptionsDialog.HeapdumpStatistics stats) {
        final var box = new VBox(5);

        final var radio = new RadioButton("Use RPC Transport");
        radio.setToggleGroup(toggleGroup);
        radio.setUserData(HeapdumpOptionsDialog.HeapdumpOption.USE_RPC);
        radio.setStyle("-fx-font-weight: bold;");

        final var transportType = stats.isMqttTransport ? "MQTT" : "Socket";
        final var descLabel     = new Label("Transfer via " + transportType + " ("
                + formatSize(stats.estimatedCompressedSize) + " compressed)");
        descLabel.setStyle("-fx-text-fill: #6b7280;");
        descLabel.setWrapText(true);
        VBox.setMargin(descLabel, new Insets(0, 0, 0, 25));

        final var recommendLabel = new Label("Recommended for heapdumps < 200 MB");
        recommendLabel.setStyle("-fx-text-fill: #6b7280;");
        VBox.setMargin(recommendLabel, new Insets(0, 0, 0, 25));

        box.getChildren().addAll(radio, descLabel, recommendLabel);
        return box;
    }

    private VBox createLocalOption() {
        final var box = new VBox(5);

        final var radio = new RadioButton("Store Locally on Agent");
        radio.setToggleGroup(toggleGroup);
        radio.setUserData(HeapdumpOptionsDialog.HeapdumpOption.STORE_LOCALLY);
        radio.setStyle("-fx-font-weight: bold;");

        final var descLabel = new Label("You will be prompted to specify the directory path on the agent");
        descLabel.setStyle("-fx-text-fill: #6b7280;");
        VBox.setMargin(descLabel, new Insets(0, 0, 0, 25));

        final var infoLabel = new Label("Retrieve the file manually via SFTP/SCP");
        infoLabel.setStyle("-fx-text-fill: #6b7280;");
        VBox.setMargin(infoLabel, new Insets(0, 0, 0, 25));

        box.getChildren().addAll(radio, descLabel, infoLabel);
        return box;
    }

    private void updateSelectedOption() {
        final var selectedToggle = toggleGroup.getSelectedToggle();
        if (selectedToggle != null) {
            selectedOptionProperty.set((HeapdumpOptionsDialog.HeapdumpOption) selectedToggle.getUserData());
        }
    }

    private static String formatSize(final long bytes) {
        if (bytes == Long.MAX_VALUE) {
            return "Unlimited";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        final var exp = (int) (Math.log(bytes) / Math.log(1024));
        final var pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public ObjectProperty<HeapdumpOptionsDialog.HeapdumpOption> selectedOptionProperty() {
        return selectedOptionProperty;
    }

    public HeapdumpOptionsDialog.HeapdumpOption getSelectedOption() {
        return selectedOptionProperty.get();
    }
}
