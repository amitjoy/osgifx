/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
package com.osgifx.console.ui.roles.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.List;
import java.util.Map;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.ui.roles.helper.RolesHelper;
import com.osgifx.console.util.fx.FxDialog;
import com.osgifx.console.util.fx.PeekablePasswordField;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class PropertiesConfigurationDialog extends Dialog<String> {

    public enum ConfigurationType {
        PROPERTIES,
        CREDENTIALS
    }

    private ConfigurationType          type;
    private final List<PropertiesForm> entries = Lists.newArrayList();

    public void init(final ConfigurationType type, final Map<String, Object> properties) {
        this.type = type;
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Role " + type.name().toLowerCase());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(getClass().getResource("/graphic/images/configuration.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);

        content.getChildren().add(lbMessage);
        if (!properties.isEmpty()) {
            properties.forEach((k, v) -> addFieldPair(content, k, v.toString()));
        } else {
            addFieldPair(content);
        }

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
        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            try {
                return data == ButtonData.OK_DONE ? getInput() : null;
            } catch (final Exception e) {
                return null;
            }
        });
    }

    private String getInput() {
        final Map<String, String> props = Maps.newHashMap();
        for (final PropertiesForm form : entries) {
            final var key   = form.txtKey.getText();
            final var value = ((TextField) form.txtValue).getText();
            if (!key.isBlank() && !value.isBlank()) {
                props.put(key, value);
            }
        }
        return RolesHelper.mapToString(props);
    }

    private class PropertiesForm extends HBox {

        private final Button btnAddField;
        private final Button btnRemoveField;

        private final CustomTextField txtKey;
        private Node                  txtValue;

        public PropertiesForm(final VBox parent, final String propKey, final String propValue) {
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);

            txtKey = (CustomTextField) TextFields.createClearableTextField();
            txtKey.setLeft(new ImageView(getClass().getResource("/graphic/icons/kv.png").toExternalForm()));

            txtKey.setPromptText("Key");
            txtKey.setText(propKey != null ? propKey : "");

            if (type == ConfigurationType.PROPERTIES) {
                txtValue = TextFields.createClearableTextField();
                ((CustomTextField) txtValue)
                        .setLeft(new ImageView(getClass().getResource("/graphic/icons/kv.png").toExternalForm()));
                ((CustomTextField) txtValue).setPromptText("Value");
                ((CustomTextField) txtValue).setText(propValue != null ? propValue : "");
            } else {
                txtValue = new PeekablePasswordField();
                ((PeekablePasswordField) txtValue).setPromptText("Value");
                ((PeekablePasswordField) txtValue).setText(propValue != null ? propValue : "");
            }

            btnAddField    = new Button();
            btnRemoveField = new Button();

            btnAddField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.PLUS));
            btnRemoveField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));

            btnAddField.setOnAction(e -> addFieldPair(parent));
            btnRemoveField.setOnAction(e -> removeFieldPair(parent, this));

            getChildren().addAll(txtKey, txtValue, btnAddField, btnRemoveField);

            entries.add(this);
        }

        private void removeFieldPair(final VBox content, final PropertiesForm form) {
            if (content.getChildren().size() > 2) {
                content.getChildren().remove(form);
                getDialogPane().getScene().getWindow().sizeToScene();
                entries.remove(form);
            }
        }

    }

    private void addFieldPair(final VBox parent) {
        addFieldPair(parent, null, null);
    }

    private void addFieldPair(final VBox content, final String propKey, final String propValue) {
        content.getChildren().add(new PropertiesForm(content, propKey, propValue));
        getDialogPane().getScene().getWindow().sizeToScene();
    }

}
