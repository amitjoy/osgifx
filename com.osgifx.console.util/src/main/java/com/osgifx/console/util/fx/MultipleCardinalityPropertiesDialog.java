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
package com.osgifx.console.util.fx;

import static com.osgifx.console.agent.dto.XAttributeDefType.BOOLEAN;
import static com.osgifx.console.agent.dto.XAttributeDefType.BOOLEAN_ARRAY;
import static com.osgifx.console.agent.dto.XAttributeDefType.BOOLEAN_LIST;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.osgifx.console.agent.dto.XAttributeDefType;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class MultipleCardinalityPropertiesDialog extends Dialog<String> {

    private int                        unsignedCardinality;
    private ClassLoader                classLoader;
    private final List<PropertiesForm> entries = Lists.newArrayList();

    public void init(final String key,
                     final XAttributeDefType targetType,
                     final int unsignedCardinality,
                     final String textInput,
                     final ClassLoader classLoader) {
        this.classLoader         = classLoader;
        this.unsignedCardinality = unsignedCardinality;
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Multiple Cardinality [Key: " + key + "]");
        dialogPane.getStylesheets().add(classLoader.getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(classLoader.getResource("/graphic/images/configuration.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);

        content.getChildren().add(lbMessage);
        if (StringUtils.isNotBlank(textInput)) {
            Splitter.on(",").split(textInput.strip()).forEach(e -> addFieldPair(content, targetType, e));
        } else {
            addFieldPair(content, targetType);
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
        // @formatter:off
        return entries.stream()
                      .map(f -> getValue(f.node))
                      .filter(StringUtils::isNotBlank)
                      .collect(joining(","));
        // @formatter:on
    }

    private String getValue(final Node node) {
        if (node instanceof final CustomTextField c) {
            return c.getText();
        }
        if (node instanceof final ToggleSwitch s) {
            return String.valueOf(s.isSelected());
        }
        return null;
    }

    private class PropertiesForm extends HBox {

        private final Button btnAddField;
        private final Button btnRemoveField;

        private final Node node;

        public PropertiesForm(final VBox parent, final XAttributeDefType type, final String initValue) {
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);

            node = getFieldByType(type, initValue);

            btnAddField    = new Button();
            btnRemoveField = new Button();

            btnAddField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.PLUS));
            btnRemoveField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));

            btnAddField.setOnAction(e -> addFieldPair(parent, type));
            btnRemoveField.setOnAction(e -> removeFieldPair(parent, this));

            getChildren().addAll(node, btnAddField, btnRemoveField);

            entries.add(this);
        }

        private void removeFieldPair(final VBox content, final PropertiesForm form) {
            if (content.getChildren().size() > 2) {
                content.getChildren().remove(form);
                getDialogPane().getScene().getWindow().sizeToScene();
                entries.remove(form);
            }
        }

        private Node getFieldByType(final XAttributeDefType type, final String initValue) {
            final CustomTextField txtField;
            if (type != BOOLEAN || type != BOOLEAN_ARRAY || type != BOOLEAN_LIST) {
                txtField = (CustomTextField) TextFields.createClearableTextField();
                txtField.setLeft(new ImageView(classLoader.getResource("/graphic/icons/kv.png").toExternalForm()));
                if (initValue != null) {
                    txtField.setText(initValue);
                }
            } else {
                txtField = null;
            }
            if (txtField != null) {
                switch (type) {
                    case LONG_ARRAY, LONG_LIST, INTEGER_ARRAY, INTEGER_LIST:
                        final var valueCaptionAsInt = switch (type) {
                            case LONG_ARRAY, LONG_LIST -> "Long Number";
                            case INTEGER_ARRAY, INTEGER_LIST -> "Integer Number";
                            default -> "";
                        };
                        txtField.setPromptText(valueCaptionAsInt);
                        txtField.textProperty()
                                .addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
                                    if (!newValue.matches("\\d*")) {
                                        txtField.setText(newValue.replaceAll("[^\\d]", ""));
                                    }
                                });
                        break;
                    case BOOLEAN_ARRAY, BOOLEAN_LIST:
                        final var toggleSwitch = new ToggleSwitch();
                        toggleSwitch.setSelected(Boolean.parseBoolean(initValue));
                        return toggleSwitch;
                    case DOUBLE_ARRAY, DOUBLE_LIST, FLOAT_ARRAY, FLOAT_LIST:
                        final var valueCaptionAsDouble = "Decimal Number";
                        txtField.setPromptText(valueCaptionAsDouble);
                        final var pattern = Pattern.compile("\\d*|\\d+\\.\\d*");
                        final TextFormatter<?> doubleFormatter = new TextFormatter<>((UnaryOperator<TextFormatter.Change>) change -> (pattern
                                .matcher(change.getControlNewText()).matches() ? change : null));
                        txtField.setTextFormatter(doubleFormatter);
                        break;
                    case CHAR_ARRAY, CHAR_LIST:
                        final var valueCaptionAsChar = "Character Value";
                        txtField.setPromptText(valueCaptionAsChar);
                        final TextFormatter<?> charFormatter = new TextFormatter<>((UnaryOperator<TextFormatter.Change>) change -> (change
                                .getControlNewText().length() == 1 ? change : null));
                        txtField.setTextFormatter(charFormatter);
                        break;
                    case STRING_ARRAY, STRING_LIST:
                    default:
                        final var valueCaptionAsStr = "String Value";
                        txtField.setPromptText(valueCaptionAsStr);
                        break;
                }
            }
            return txtField;
        }
    }

    private void addFieldPair(final VBox content, final XAttributeDefType type) {
        addFieldPair(content, type, null);
    }

    private void addFieldPair(final VBox content, final XAttributeDefType type, final String initValue) {
        if (content.getChildren().size() <= unsignedCardinality) {
            content.getChildren().add(new PropertiesForm(content, type, initValue));
            getDialogPane().getScene().getWindow().sizeToScene();
        }
    }

}
