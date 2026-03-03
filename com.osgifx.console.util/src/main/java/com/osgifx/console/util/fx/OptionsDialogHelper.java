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
package com.osgifx.console.util.fx;

import com.osgifx.console.agent.spi.LargePayloadHandler;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

public final class OptionsDialogHelper {

    private OptionsDialogHelper() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static String formatSize(final long bytes) {
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

    public static Label createTransportLabel(final boolean isMqttTransport, final long estimatedCompressedSize) {
        final var label = new Label();
        label.setStyle("-fx-font-weight: bold;");

        if (isMqttTransport) {
            final var mqttLimit = 250_000_000L;
            if (estimatedCompressedSize > mqttLimit) {
                label.setText("MQTT (max 250 MB) ❌");
                label.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            } else {
                label.setText("MQTT (max 250 MB) ✓");
                label.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        } else {
            final var socketLimit = 200_000_000L;
            if (estimatedCompressedSize > socketLimit) {
                label.setText("Socket (max 200 MB) ⚠️");
                label.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
            } else {
                label.setText("Socket (max 2 GB) ✓");
                label.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        }
        return label;
    }

    public static void setupWarningLabel(final Label warningLabel,
                                         final long estimatedUncompressedSize,
                                         final String type) {
        if (estimatedUncompressedSize > 100_000_000L) {
            warningLabel.setText("⚠️ This " + type + " is large. Please choose an appropriate option:");
            warningLabel.setManaged(true);
            warningLabel.setVisible(true);
        }
    }

    public static VBox createHandlerOption(final ToggleGroup toggleGroup,
                                           final Object userData,
                                           final LargePayloadHandler handler) {
        final var box = new VBox(5);

        final var radio = new RadioButton("Use Handler: \"" + handler.getHandlerName() + "\"");
        radio.setToggleGroup(toggleGroup);
        radio.setUserData(userData);
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

    public static VBox createRpcOption(final ToggleGroup toggleGroup,
                                       final Object userData,
                                       final boolean isMqttTransport,
                                       final long estimatedCompressedSize,
                                       final String type) {
        final var box = new VBox(5);

        final var radio = new RadioButton("Use RPC Transport");
        radio.setToggleGroup(toggleGroup);
        radio.setUserData(userData);
        radio.setStyle("-fx-font-weight: bold;");

        final var transportType = isMqttTransport ? "MQTT" : "Socket";
        final var descLabel     = new Label("Transfer via " + transportType + " (" + formatSize(estimatedCompressedSize)
                + " compressed)");
        descLabel.setStyle("-fx-text-fill: #6b7280;");
        descLabel.setWrapText(true);
        VBox.setMargin(descLabel, new Insets(0, 0, 0, 25));

        final var limit          = type.equals("snapshot") ? "100" : "200";
        final var recommendLabel = new Label("Recommended for " + type + "s < " + limit + " MB");
        recommendLabel.setStyle("-fx-text-fill: #6b7280;");
        VBox.setMargin(recommendLabel, new Insets(0, 0, 0, 25));

        box.getChildren().addAll(radio, descLabel, recommendLabel);
        return box;
    }

    public static VBox createLocalOption(final ToggleGroup toggleGroup, final Object userData) {
        final var box = new VBox(5);

        final var radio = new RadioButton("Store Locally on Agent");
        radio.setToggleGroup(toggleGroup);
        radio.setUserData(userData);
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

}
