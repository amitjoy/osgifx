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
package com.osgifx.console.ui.overview;

import static com.google.common.base.Verify.verify;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static org.controlsfx.validation.Validator.createPredicateValidator;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.osgifx.console.util.fx.FxDialog;

import jakarta.inject.Inject;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class ViewRefreshDelayDialog extends Dialog<Double> {

    @Log
    @Inject
    private FluentLogger logger;

    private final ValidationSupport validationSupport = new ValidationSupport();

    public void init(final long refreshDelay) {
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Refresh Delay in Seconds");
        dialogPane.getStylesheets().add(getClass().getClassLoader().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(
                new ImageView(getClass().getClassLoader().getResource("/graphic/images/delay.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content    = new VBox(10);
        final var delayField = (CustomTextField) TextFields.createClearableTextField();

        final var nameCaption          = "Refresh Delay";
        final var requiredFormat       = "'%s' is required";
        final var requiredNumberFormat = "'%s' should be a valid integer number";

        delayField.setText(String.valueOf(refreshDelay));
        delayField.setLeft(new ImageView(getClass().getResource("/graphic/icons/delay.png").toExternalForm()));
        delayField.setPromptText("Delay in seconds");
        validationSupport.registerValidator(delayField,
                Validator.createEmptyValidator(String.format(requiredFormat, nameCaption)));
        validationSupport.registerValidator(delayField, createPredicateValidator(
                value -> Ints.tryParse(value.toString()) != null, String.format(requiredNumberFormat, nameCaption)));

        content.getChildren().addAll(delayField);
        dialogPane.setContent(content);

        final var finishButtonType = new ButtonType("Finish", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(finishButtonType);

        final var finishButton = (Button) dialogPane.lookupButton(finishButtonType);
        finishButton.setOnAction(actionEvent -> {
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
        finishButton.disableProperty().bind(validationSupport.invalidProperty());
        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            if (data == ButtonData.CANCEL_CLOSE) {
                return null;
            }
            try {
                if (data == ButtonData.OK_DONE) {
                    verify(!validationSupport.isInvalid(), "Entry validation failed");
                    return Doubles.tryParse(delayField.getText());
                }
                return null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Invalid refresh delay");
                throw e;
            }
        });
    }

}
