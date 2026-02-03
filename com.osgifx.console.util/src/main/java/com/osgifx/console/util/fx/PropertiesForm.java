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
package com.osgifx.console.util.fx;

import static com.osgifx.console.agent.dto.XAttributeDefType.BOOLEAN;

import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.CustomPasswordField;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.fx.core.Triple;

import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.osgifx.console.agent.dto.XAttributeDefType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class PropertiesForm {

    private final DialogPane dialogPane;

    private final Map<FormContent, Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>>> entries = Maps
            .newHashMap();

    public PropertiesForm(final DialogPane dialogPane) {
        this.dialogPane = dialogPane;
    }

    public class FormContent extends HBox {

        private final CustomTextField txtKey;
        private Node                  node;

        private final Button btnAddField;
        private final Button btnRemoveField;

        public FormContent(final VBox parent) {
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);

            final var keyCaption = "Key";

            txtKey = (CustomTextField) TextFields.createClearableTextField();
            txtKey.setLeft(
                    new ImageView(PropertiesForm.class.getClassLoader().getResource("/kv.png").toExternalForm()));
            txtKey.setPromptText(keyCaption);

            btnAddField    = new Button();
            btnRemoveField = new Button();

            final ObservableList<XAttributeDefType> options  = FXCollections
                    .observableArrayList(XAttributeDefType.values());
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

                        dialog.init(key, newValue, Integer.MAX_VALUE, field.getText(), getClass().getClassLoader());
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
                                                                                                          () -> getValue(
                                                                                                                  node),
                                                                                                          comboBox::getValue);
            entries.put(this, tuple);
        }

        private Node getFieldByType(final XAttributeDefType type) {
            final CustomTextField txtField;
            if (type != BOOLEAN) {
                txtField = (CustomTextField) TextFields.createClearableTextField();
                txtField.setLeft(
                        new ImageView(PropertiesForm.class.getClassLoader().getResource("/kv.png").toExternalForm()));
            } else {
                txtField = null;
            }
            switch (type) {
                case LONG:
                    final var captionAsLong = "Long Number";
                    txtField.setPromptText(captionAsLong);
                    final TextFormatter<?> longFormatter = new TextFormatter<>((UnaryOperator<TextFormatter.Change>) change -> Longs
                            .tryParse(change.getControlNewText()) != null ? change : null);
                    txtField.setTextFormatter(longFormatter);
                    break;
                case INTEGER:
                    final var captionAsInteger = "Integer Number";
                    txtField.setPromptText(captionAsInteger);
                    final TextFormatter<?> integerFormatter = new TextFormatter<>((UnaryOperator<TextFormatter.Change>) change -> Ints
                            .tryParse(change.getControlNewText()) != null ? change : null);
                    txtField.setTextFormatter(integerFormatter);
                    break;
                case BOOLEAN:
                    return new ToggleSwitch();
                case DOUBLE:
                    final var captionAsDouble = "Decimal Number";
                    txtField.setPromptText(captionAsDouble);
                    final TextFormatter<?> doubleFormatter = new TextFormatter<>((UnaryOperator<TextFormatter.Change>) change -> Doubles
                            .tryParse(change.getControlNewText()) != null ? change : null);
                    txtField.setTextFormatter(doubleFormatter);
                    break;
                case FLOAT:
                    final var captionAsFloat = "Decimal Number";
                    txtField.setPromptText(captionAsFloat);
                    final TextFormatter<?> floatFormatter = new TextFormatter<>((UnaryOperator<TextFormatter.Change>) change -> Floats
                            .tryParse(change.getControlNewText()) != null ? change : null);
                    txtField.setTextFormatter(floatFormatter);
                    break;
                case CHAR:
                    final var captionAsChar = "Character Value";
                    txtField.setPromptText(captionAsChar);
                    final TextFormatter<?> charFormatter = new TextFormatter<>((UnaryOperator<TextFormatter.Change>) change -> (change
                            .getControlNewText().length() == 1 ? change : null));
                    txtField.setTextFormatter(charFormatter);
                    break;
                case STRING:
                    final var captionAsStr = "String Value";
                    txtField.setPromptText(captionAsStr);
                    break;
                case PASSWORD:
                    final var valueCaptionAsPwd = "Password Value";
                    final var pwdField = (CustomPasswordField) TextFields.createClearablePasswordField();
                    pwdField.setLeft(new ImageView(PropertiesForm.class.getClassLoader().getResource("/kv.png")
                            .toExternalForm()));
                    pwdField.setPromptText(valueCaptionAsPwd);
                    return pwdField;
                case STRING_ARRAY, STRING_LIST, INTEGER_ARRAY, INTEGER_LIST, BOOLEAN_ARRAY, BOOLEAN_LIST, DOUBLE_ARRAY, DOUBLE_LIST, FLOAT_ARRAY, FLOAT_LIST, CHAR_ARRAY, CHAR_LIST, LONG_ARRAY, LONG_LIST:
                    final var valueCaptionAsMultipleCardinality = "Multiple Cardinality Value";
                    txtField.setPromptText(valueCaptionAsMultipleCardinality);
                    break;
            }
            return txtField;
        }
    }

    public void addFieldPair(final VBox content) {
        content.getChildren().add(new FormContent(content));
        dialogPane.getScene().getWindow().sizeToScene();
    }

    private void removeFieldPair(final VBox content, final FormContent form) {
        if (content.getChildren().size() > 4) {
            content.getChildren().remove(form);
            dialogPane.getScene().getWindow().sizeToScene();
            entries.remove(form);
        }
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

    public Map<FormContent, Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>>> entries() {
        return entries;
    }

}