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
package com.osgifx.console.application.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import javax.inject.Inject;

import org.controlsfx.control.textfield.CustomPasswordField;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

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
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

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
        final var hostnameCaption           = "Hostname";
        final var portCaption               = "Port (between 1 to 65536)";
        final var timeoutCaption            = "Timeout in millis";
        final var trustStoreCaption         = "JKS Truststore Location";
        final var trustStorePasswordCaption = "JKS Truststore Password";

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
            validationSupport.registerValidator(hostname,
                    Validator.createEmptyValidator(String.format(requiredFormat, hostnameCaption)));
            validationSupport.registerValidator(port,
                    Validator.createEmptyValidator(String.format(requiredFormat, portCaption)));
            validationSupport.registerValidator(port, Validator.createPredicateValidator(value -> {
                try {
                    final var parsedPort = Integer.parseInt(value.toString());
                    return parsedPort > 0 && parsedPort < 65536;
                } catch (final Exception e) {
                    return false;
                }
            }, String.format(requiredPortFormat, portCaption)));
            validationSupport.registerValidator(timeout,
                    Validator.createEmptyValidator(String.format(requiredFormat, timeoutCaption)));
            validationSupport.registerValidator(timeout, Validator.createPredicateValidator(value -> {
                try {
                    Integer.parseInt(value.toString());
                    return true;
                } catch (final Exception e) {
                    return false;
                }
            }, String.format(requiredNumberFormat, timeoutCaption)));
        });
        setResultConverter(dialogButton -> {
            try {
                return dialogButton == saveButtonType
                        ? new ConnectionSettingDTO(hostname.getText(), Integer.parseInt(port.getText()),
                                Integer.parseInt(timeout.getText()), trustStore.getAccessibleText(),
                                trustStorePassword.getText())
                        : null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Connection settings cannot be added due to validation problem");
                throw e;
            }
        });
    }

}
