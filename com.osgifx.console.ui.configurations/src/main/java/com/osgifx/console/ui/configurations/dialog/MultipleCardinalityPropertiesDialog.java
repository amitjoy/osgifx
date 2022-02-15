/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.ui.configurations.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.List;

import javax.inject.Inject;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.util.fx.FxDialog;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class MultipleCardinalityPropertiesDialog extends Dialog<String> {

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    private ThreadSynchronize          threadSync;
    private XAttributeDefType          targetType;
    private final List<PropertiesForm> entries = Lists.newArrayList();

    public void init(final String key, final XAttributeDefType targetType) {
        this.targetType = targetType;
        final DialogPane dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Key - " + key + System.lineSeparator() + "Type - " + targetType);
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/configuration.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final Label lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final VBox content = new VBox(10);

        content.getChildren().add(lbMessage);
        addFieldPair(content);

        dialogPane.setContent(content);

        final ButtonType finishButtonType = new ButtonType("Finish", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(finishButtonType);

        final Button finishButton = (Button) dialogPane.lookupButton(finishButtonType);
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
        setResultConverter(dialogButton -> {
            final ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            try {
                return data == ButtonData.OK_DONE ? getInput() : null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Configuration values cannot be converted");
            }
            return null;
        });
    }

    private String getInput() {
        final List<String> properties = Lists.newArrayList();
        for (final PropertiesForm form : entries) {
            final String value = form.txtValue.getText();
            if (Strings.isNullOrEmpty(value)) {
                continue;
            }
            Object convertedValue;
            try {
                convertedValue = getParsed(targetType, value);
            } catch (final Exception e) {
                threadSync.asyncExec(() -> FxDialog.showErrorDialog("Value Conversion Error",
                        value + " cannot be converted as " + targetType, getClass().getClassLoader()));
                return null;
            }
            if (convertedValue != null) {
                properties.add(convertedValue.toString());
            }
        }
        return String.join(",", properties);
    }

    private class PropertiesForm extends HBox {

        private final CustomTextField txtValue;

        private final Button btnAddField;
        private final Button btnRemoveField;

        public PropertiesForm(final VBox parent) {
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);

            final String valueCaption = "Value";

            txtValue = (CustomTextField) TextFields.createClearableTextField();
            txtValue.setLeft(new ImageView(getClass().getResource("/graphic/icons/kv.png").toExternalForm()));

            txtValue.setPromptText(valueCaption);

            btnAddField    = new Button();
            btnRemoveField = new Button();

            btnAddField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.PLUS));
            btnRemoveField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));

            btnAddField.setOnAction(e -> addFieldPair(parent));
            btnRemoveField.setOnAction(e -> removeFieldPair(parent, this));

            getChildren().addAll(txtValue, btnAddField, btnRemoveField);

            entries.add(this);
        }
    }

    private void addFieldPair(final VBox content) {
        content.getChildren().add(new PropertiesForm(content));
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    private void removeFieldPair(final VBox content, final PropertiesForm form) {
        if (content.getChildren().size() > 2) {
            content.getChildren().remove(form);
            getDialogPane().getScene().getWindow().sizeToScene();
        }
        entries.remove(form);
    }

    private Object getParsed(final XAttributeDefType type, final String value) {
        return switch (type) {
            case STRING_ARRAY, STRING_LIST -> value;
            case INTEGER_ARRAY, INTEGER_LIST -> Integer.parseInt(value);
            case BOOLEAN_ARRAY, BOOLEAN_LIST -> Boolean.parseBoolean(value);
            case DOUBLE_ARRAY, DOUBLE_LIST -> Double.parseDouble(value);
            case FLOAT_ARRAY, FLOAT_LIST -> Float.parseFloat(value);
            case CHAR_ARRAY, CHAR_LIST -> value.charAt(0);
            case LONG_ARRAY, LONG_LIST -> Long.parseLong(value);
            default -> throw new IllegalArgumentException("Non-multiple Cardinality Type");
        };
    }

}
