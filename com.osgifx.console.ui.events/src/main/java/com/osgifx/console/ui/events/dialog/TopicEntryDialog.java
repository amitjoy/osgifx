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
package com.osgifx.console.ui.events.dialog;

import static com.google.common.base.Verify.verify;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static com.osgifx.console.util.fx.ConsoleFxHelper.validateTopic;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Lists;
import com.osgifx.console.util.fx.FxDialog;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class TopicEntryDialog extends Dialog<Set<String>> {

    @Log
    @Inject
    private FluentLogger logger;

    private final List<PropertiesForm> entries           = Lists.newArrayList();
    private final ValidationSupport    validationSupport = new ValidationSupport();

    public void init() {
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Receive Events from Topics");
        dialogPane.getStylesheets().add(getClass().getClassLoader().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(
                new ImageView(getClass().getClassLoader().getResource("/graphic/images/event-receive.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);
        addFieldPair(content);

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
                    verify(!validationSupport.isInvalid(), "Topic validation failed");
                    return getInput();
                }
                return null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Invalid topic");
                throw e;
            }
        });
    }

    private Set<String> getInput() {
        // @formatter:off
        return entries.stream()
                      .map(f -> f.textTopic.getText())
                      .filter(StringUtils::isNotBlank)
                      .collect(toSet());
        // @formatter:on
    }

    private class PropertiesForm extends HBox {

        private final Button btnAddField;
        private final Button btnRemoveField;

        private final CustomTextField textTopic;

        public PropertiesForm(final VBox parent) {
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);

            textTopic = (CustomTextField) TextFields.createClearableTextField();
            textTopic.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));
            textTopic.setPromptText("Event Topic");
            validationSupport.registerValidator(textTopic, Validator
                    .createPredicateValidator(value -> validateTopic(value.toString()), "Invalid Event Topic"));

            btnAddField    = new Button();
            btnRemoveField = new Button();

            btnAddField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.PLUS));
            btnRemoveField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));

            btnAddField.setOnAction(e -> addFieldPair(parent));
            btnRemoveField.setOnAction(e -> removeFieldPair(parent, this));

            getChildren().addAll(textTopic, btnAddField, btnRemoveField);
            entries.add(this);
        }

        private void removeFieldPair(final VBox content, final PropertiesForm form) {
            if (content.getChildren().size() > 1) {
                content.getChildren().remove(form);
                getDialogPane().getScene().getWindow().sizeToScene();
                validationSupport.deregisterValidator(textTopic);
                textTopic.getProperties().clear();
                entries.remove(form);
            }
        }
    }

    private void addFieldPair(final VBox content) {
        content.getChildren().add(new PropertiesForm(content));
        getDialogPane().getScene().getWindow().sizeToScene();
    }

}
