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
package com.osgifx.console.ui.configurations.dialog;

import static com.osgifx.console.agent.dto.XAttributeDefType.BOOLEAN;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.fx.core.Triple;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.util.converter.ValueConverter;
import com.osgifx.console.util.fx.FxDialog;
import com.osgifx.console.util.fx.MultipleCardinalityPropertiesDialog;
import com.osgifx.console.util.fx.PeekablePasswordField;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class ConfigurationCreateDialog extends Dialog<ConfigurationDTO> {

    @Log
    @Inject
    private FluentLogger         logger;
    private final ValueConverter converter = new ValueConverter();

    private final Map<PropertiesForm, Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>>> configurationEntries = Maps
            .newHashMap();

    public void init() {
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Create New Configuration for OSGi Configuration Admin");
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/configuration.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final var txtPid = (CustomTextField) TextFields.createClearableTextField();
        txtPid.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));

        final var txtFactoryPid = (CustomTextField) TextFields.createClearableTextField();
        txtFactoryPid.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);

        content.getChildren().add(lbMessage);
        content.getChildren().add(txtPid);
        content.getChildren().add(txtFactoryPid);
        addFieldPair(content);

        dialogPane.setContent(content);

        final var createButtonType = new ButtonType("Create", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(createButtonType);

        final var createButton = (Button) dialogPane.lookupButton(createButtonType);
        createButton.setOnAction(actionEvent -> {
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
        final var pidCaption        = "Configuration PID";
        final var factoryPidCaption = "Factory PID";

        txtPid.setPromptText(pidCaption);
        txtFactoryPid.setPromptText(factoryPidCaption);

        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            try {
                if (data == ButtonData.OK_DONE) {
                    return getInput(txtPid, txtFactoryPid);
                }
                return null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Configuration values cannot be converted");
                Throwables.throwIfInstanceOf(e, RuntimeException.class);
                return null;
            }
        });
    }

    private ConfigurationDTO getInput(final CustomTextField txtPid, final CustomTextField txtFactoryPid) throws Exception {
        final List<ConfigValue> properties = Lists.newArrayList();
        for (final Entry<PropertiesForm, Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>>> entry : configurationEntries
                .entrySet()) {
            final var value       = entry.getValue();
            final var configKey   = value.value1.get();
            final var configValue = value.value2.get();
            var       configType  = value.value3.get();
            if (Strings.isNullOrEmpty(configKey) || Strings.isNullOrEmpty(configValue)) {
                continue;
            }
            if (configType == null) {
                configType = XAttributeDefType.STRING;
            }
            final var convertedValue = converter.convert(configValue, configType);
            properties.add(ConfigValue.create(configKey, convertedValue, configType));
        }
        return new ConfigurationDTO(txtPid.getText(), txtFactoryPid.getText(), properties);
    }

    private class PropertiesForm extends HBox {

        private final CustomTextField txtKey;
        private Node                  node;

        private final Button btnAddField;
        private final Button btnRemoveField;

        public PropertiesForm(final VBox parent) {
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);

            final var keyCaption = "Key";

            txtKey = (CustomTextField) TextFields.createClearableTextField();
            txtKey.setLeft(new ImageView(getClass().getResource("/graphic/icons/kv.png").toExternalForm()));

            txtKey.setPromptText(keyCaption);

            btnAddField    = new Button();
            btnRemoveField = new Button();

            final ObservableList<XAttributeDefType> options  = FXCollections.observableArrayList(XAttributeDefType.values());
            final var                               comboBox = new ComboBox<>(options);

            final var type = comboBox.getValue();
            node = getFieldByType(type == null ? XAttributeDefType.STRING : type);

            comboBox.getSelectionModel().selectedItemProperty().addListener((opt, oldValue, newValue) -> {
                final Class<?> clazz   = XAttributeDefType.clazz(newValue);
                final var      newNode = getFieldByType(newValue);
                newNode.setOnMouseClicked(e -> {
                    // multiple cardinality
                    if (clazz == null) {
                        final var dialog = new MultipleCardinalityPropertiesDialog();
                        final var key    = txtKey.getText();
                        final var field  = (TextField) node;

                        dialog.init(key, newValue, field.getText(), getClass().getClassLoader());
                        final var entries = dialog.showAndWait();
                        entries.ifPresent(field::setText);
                    }
                });
                node = newNode;
                final var children = getChildren();
                if (!children.isEmpty()) {
                    children.set(1, newNode);
                }
            });

            comboBox.getSelectionModel().select(0); // default STRING type

            btnAddField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.PLUS));
            btnRemoveField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));

            btnAddField.setOnAction(e -> addFieldPair(parent));
            btnRemoveField.setOnAction(e -> removeFieldPair(parent, this));

            getChildren().addAll(txtKey, node, comboBox, btnAddField, btnRemoveField);

            final var tuple = new Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>>(txtKey::getText,
                    () -> getValue(node), comboBox::getValue);
            configurationEntries.put(this, tuple);
        }

        private Node getFieldByType(final XAttributeDefType type) {
            final CustomTextField txtField;
            if (type != BOOLEAN) {
                txtField = (CustomTextField) TextFields.createClearableTextField();
                txtField.setLeft(new ImageView(getClass().getResource("/graphic/icons/kv.png").toExternalForm()));
            } else {
                txtField = null;
            }
            switch (type) {
            case LONG, INTEGER:
                final var captionAsInt = switch (type) {
                case LONG -> "Long Number";
                case INTEGER -> "Integer Number";
                default -> null;
                };
                txtField.setPromptText(captionAsInt);
                txtField.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
                    if (!newValue.matches("\\d*")) {
                        txtField.setText(newValue.replaceAll("[^\\d]", ""));
                    }
                });
                break;
            case BOOLEAN:
                return new ToggleSwitch();
            case DOUBLE, FLOAT:
                final var captionAsDouble = "Decimal Number";
                txtField.setPromptText(captionAsDouble);
                final var pattern = Pattern.compile("\\d*|\\d+\\.\\d*");
                final TextFormatter<?> doubleFormatter = new TextFormatter<>(
                        (UnaryOperator<TextFormatter.Change>) change -> (pattern.matcher(change.getControlNewText()).matches() ? change
                                : null));
                txtField.setTextFormatter(doubleFormatter);
                break;
            case CHAR:
                final var valueCaptionAsChar = "Character Value";
                txtField.setPromptText(valueCaptionAsChar);
                final TextFormatter<?> charFormatter = new TextFormatter<>(
                        (UnaryOperator<TextFormatter.Change>) change -> (change.getControlNewText().length() == 1 ? change : null));
                txtField.setTextFormatter(charFormatter);
                break;
            case STRING:
                final var valueCaptionAsStr = "String Value";
                txtField.setPromptText(valueCaptionAsStr);
                break;
            case PASSWORD:
                final var valueCaptionAsPwd = "Password Value";
                final var pwdField = new PeekablePasswordField();
                pwdField.setPromptText(valueCaptionAsPwd);
                return pwdField;
            case STRING_ARRAY, STRING_LIST, INTEGER_ARRAY, INTEGER_LIST, BOOLEAN_ARRAY, BOOLEAN_LIST, DOUBLE_ARRAY, DOUBLE_LIST,
                    FLOAT_ARRAY, FLOAT_LIST, CHAR_ARRAY, CHAR_LIST, LONG_ARRAY, LONG_LIST:
                final var valueCaptionAsMultipleCardinality = "Multiple Cardinality Value";
                txtField.setPromptText(valueCaptionAsMultipleCardinality);
                break;
            }
            return txtField;
        }
    }

    private void addFieldPair(final VBox content) {
        content.getChildren().add(new PropertiesForm(content));
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    private void removeFieldPair(final VBox content, final PropertiesForm form) {
        if (content.getChildren().size() > 4) {
            content.getChildren().remove(form);
            getDialogPane().getScene().getWindow().sizeToScene();
        }
        configurationEntries.remove(form);
    }

    private String getValue(final Node node) {
        if (node instanceof final TextField tf) {
            return tf.getText();
        }
        if (node instanceof final ToggleSwitch ts) {
            return String.valueOf(ts.isSelected());
        }
        return null;
    }

}
