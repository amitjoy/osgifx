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
package com.osgifx.console.application.dialog;

import static com.google.common.base.Verify.verify;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.scene.control.ButtonType.CANCEL;
import static org.controlsfx.validation.Validator.createEmptyValidator;
import static org.controlsfx.validation.Validator.createPredicateValidator;

import javax.inject.Inject;

import org.controlsfx.control.textfield.CustomPasswordField;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;
import org.controlsfx.validation.ValidationSupport;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.primitives.Ints;
import com.osgifx.console.util.fx.FxDialog;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.StageStyle;

public final class ConnectionDialog extends Dialog<ConnectionSettingDTO> {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private ThreadSynchronize threadSync;

    public void init() {
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);

        dialogPane.setHeaderText("Add Connection Settings");
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(
                new ImageView(this.getClass().getResource("/graphic/images/connection-setting.png").toString()));
        dialogPane.getButtonTypes().addAll(CANCEL);

        final var name = (CustomTextField) TextFields.createClearableTextField();
        name.setLeft(new ImageView(getClass().getResource("/graphic/icons/name.png").toExternalForm()));

        final var hostname = (CustomTextField) TextFields.createClearableTextField();
        hostname.setLeft(new ImageView(getClass().getResource("/graphic/icons/hostname.png").toExternalForm()));

        final var port = (CustomTextField) TextFields.createClearableTextField();
        port.setLeft(new ImageView(getClass().getResource("/graphic/icons/port.png").toExternalForm()));

        final var timeout = (CustomTextField) TextFields.createClearableTextField();
        timeout.setLeft(new ImageView(getClass().getResource("/graphic/icons/timeout.png").toExternalForm()));

        final var trustStore = (CustomTextField) TextFields.createClearableTextField();
        trustStore.setLeft(new ImageView(getClass().getResource("/graphic/icons/truststore.png").toExternalForm()));

        final var trustStorePassword = (CustomPasswordField) TextFields.createClearablePasswordField();
        trustStorePassword
                .setLeft(new ImageView(getClass().getResource("/graphic/icons/truststore.png").toExternalForm()));

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);

        content.getChildren().add(lbMessage);
        content.getChildren().add(name);
        content.getChildren().add(hostname);
        content.getChildren().add(port);
        content.getChildren().add(timeout);
        content.getChildren().add(trustStore);
        content.getChildren().add(trustStorePassword);

        dialogPane.setContent(content);

        final var saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType);

        final var loginButton = (Button) dialogPane.lookupButton(saveButtonType);
        loginButton.setOnAction(actionEvent -> {
            try {
                lbMessage.setVisible(false);
                lbMessage.setManaged(false);
                hide();
            } catch (final Exception ex) {
                lbMessage.setVisible(true);
                lbMessage.setManaged(true);
                lbMessage.setText(ex.getMessage());
                FxDialog.showExceptionDialog(ex, getClass().getClassLoader());
            }
        });
        final var nameCaption               = "Name";
        final var hostnameCaption           = "Host";
        final var portCaption               = "Port (between 1 to 65536)";
        final var timeoutCaption            = "Timeout in millis";
        final var trustStoreCaption         = "Truststore Location";
        final var trustStorePasswordCaption = "Truststore Password";

        name.setPromptText(nameCaption);
        hostname.setPromptText(hostnameCaption);
        port.setPromptText(portCaption);
        timeout.setPromptText(timeoutCaption);
        trustStore.setPromptText(trustStoreCaption);
        trustStorePassword.setPromptText(trustStorePasswordCaption);

        trustStore.setEditable(false);
        trustStore.setOnMouseClicked(event -> {
            final var trustStoreChooser = new FileChooser();
            trustStoreChooser.getExtensionFilters()
                    .add(new ExtensionFilter("Java Keystore Files (.keystore, .jks)", "*.keystore", "*.jks"));
            final var jksTrustStore = trustStoreChooser.showOpenDialog(null);
            if (jksTrustStore != null) {
                trustStore.setText(jksTrustStore.getName());
                trustStore.setAccessibleText(jksTrustStore.getAbsolutePath());
            }
        });

        final var validationSupport = new ValidationSupport();
        threadSync.asyncExec(() -> {
            final var requiredFormat       = "'%s' is required";
            final var requiredPortFormat   = "'%s' should be a valid port number";
            final var requiredNumberFormat = "'%s' should be a valid integer number";

            validationSupport.registerValidator(name, createEmptyValidator(String.format(requiredFormat, nameCaption)));
            validationSupport.registerValidator(hostname,
                    createEmptyValidator(String.format(requiredFormat, hostnameCaption)));
            validationSupport.registerValidator(port, createEmptyValidator(String.format(requiredFormat, portCaption)));
            validationSupport.registerValidator(port, createPredicateValidator(value -> {
                final var parsedPort = Ints.tryParse(value.toString());
                return parsedPort != null && parsedPort > 0 && parsedPort < 65536;
            }, String.format(requiredPortFormat, portCaption)));
            validationSupport.registerValidator(timeout,
                    createEmptyValidator(String.format(requiredFormat, timeoutCaption)));
            validationSupport.registerValidator(timeout,
                    createPredicateValidator(value -> Ints.tryParse(value.toString()) != null,
                            String.format(requiredNumberFormat, timeoutCaption)));
        });
        final var saveBtn = (Button) dialogPane.lookupButton(saveButtonType);
        saveBtn.disableProperty().bind(validationSupport.invalidProperty());

        setResultConverter(dialogButton -> {
            if (dialogButton == CANCEL) {
                return null;
            }
            final var p = Ints.tryParse(port.getText());
            final var t = Ints.tryParse(timeout.getText());

            verify(p != null && t != null, "Port and host formats are not compliant");

            return new ConnectionSettingDTO(name.getText(), hostname.getText(), p, t, trustStore.getAccessibleText(),
                                            trustStorePassword.getText());
        });
    }

}
