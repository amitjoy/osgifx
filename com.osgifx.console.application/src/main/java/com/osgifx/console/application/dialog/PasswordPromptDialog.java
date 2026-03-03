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
package com.osgifx.console.application.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.scene.control.ButtonType.CANCEL;

import org.controlsfx.control.textfield.CustomPasswordField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class PasswordPromptDialog extends Dialog<String> {

    public void init() {
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Password Required");
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/password.png").toString()));
        dialogPane.getButtonTypes().addAll(CANCEL);

        final var password = (CustomPasswordField) TextFields.createClearablePasswordField();
        password.setLeft(new ImageView(getClass().getResource("/graphic/icons/truststore.png").toExternalForm()));
        password.setPromptText("Enter Password");

        final var content = new VBox(10);
        content.getChildren().add(password);

        dialogPane.setContent(content);

        final var connectButtonType = new ButtonType("Connect", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(connectButtonType);

        final var connectButton = (Button) dialogPane.lookupButton(connectButtonType);
        connectButton.disableProperty().bind(password.textProperty().isEmpty());

        setResultConverter(dialogButton -> {
            if (dialogButton == CANCEL) {
                return null;
            }
            return password.getText();
        });
    }

}
