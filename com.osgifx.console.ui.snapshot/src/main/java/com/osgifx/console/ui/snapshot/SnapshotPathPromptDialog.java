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

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.scene.control.ButtonType.CANCEL;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class SnapshotPathPromptDialog extends Dialog<String> {

    public void init(final String defaultPath, final String title, final String description) {
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText(title);
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/snapshot.png").toString()));
        dialogPane.getButtonTypes().addAll(CANCEL);

        final var pathField = (CustomTextField) TextFields.createClearableTextField();
        pathField.setPromptText("Enter full file path on agent (e.g., {env:TEMP}/snapshot-2024.json)");
        pathField.setText(defaultPath);
        pathField.setPrefWidth(500);

        final var descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #6b7280;");

        final var content = new VBox(10);
        content.getChildren().addAll(descLabel, pathField);

        dialogPane.setContent(content);

        final var okButtonType = new ButtonType("OK", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(okButtonType);

        final var okButton = (Button) dialogPane.lookupButton(okButtonType);
        okButton.disableProperty().bind(pathField.textProperty().isEmpty());

        setResultConverter(dialogButton -> {
            if (dialogButton == CANCEL) {
                return null;
            }
            return pathField.getText();
        });
    }

}
