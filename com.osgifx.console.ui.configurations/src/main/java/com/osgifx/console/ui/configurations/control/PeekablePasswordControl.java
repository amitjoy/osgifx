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
package com.osgifx.console.ui.configurations.control;

import com.dlsc.formsfx.model.structure.PasswordField;
import com.dlsc.formsfx.view.controls.SimpleControl;
import com.osgifx.console.util.fx.PeekablePasswordField;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

public final class PeekablePasswordControl extends SimpleControl<PasswordField> {

    /**
     * This StackPane is needed for achieving the readonly effect by putting the
     * readOnlyLabel over the editableField on the change of the visibleProperty.
     */
    protected StackPane stack;

    /**
     * The fieldLabel is the container that displays the label property of the
     * field. - The editableField allows users to modify the field's value. - The
     * readOnlyLabel displays the field's value if it is not editable.
     */
    protected PeekablePasswordField editableField;
    protected Label                 readOnlyLabel;
    protected Label                 fieldLabel;

    /*
     * Translates characters found in user input into '*'
     */
    protected StringBinding obfuscatedUserInputBinding;

    @Override
    public void initializeParts() {
        super.initializeParts();

        getStyleClass().add("simple-password-control");

        stack = new StackPane();

        editableField = new PeekablePasswordField();
        editableField.setText(field.getValue());

        readOnlyLabel = new Label(obfuscate(field.getValue()));
        fieldLabel    = new Label(field.labelProperty().getValue());
        editableField.setPromptText(field.placeholderProperty().getValue());
    }

    @Override
    public void layoutParts() {
        super.layoutParts();

        readOnlyLabel.getStyleClass().add("read-only-label");
        readOnlyLabel.setPrefHeight(26);

        stack.getChildren().addAll(editableField, readOnlyLabel);
        stack.setAlignment(Pos.CENTER_LEFT);

        final var labelDescription = field.getLabelDescription();
        final var valueDescription = field.getValueDescription();

        final var columns = field.getSpan();

        if (columns < 3) {
            var rowIndex = 0;
            add(fieldLabel, 0, rowIndex, columns, 1);
            rowIndex++;
            if (labelDescription != null) {
                GridPane.setValignment(labelDescription, VPos.TOP);
                add(labelDescription, 0, rowIndex, columns, 1);
                rowIndex++;
            }
            add(stack, 0, rowIndex, columns, 1);
            rowIndex++;
            if (valueDescription != null) {
                GridPane.setValignment(valueDescription, VPos.TOP);
                add(valueDescription, 0, rowIndex, columns, 1);
            }
        } else {
            add(fieldLabel, 0, 0, 2, 1);
            if (labelDescription != null) {
                GridPane.setValignment(labelDescription, VPos.TOP);
                add(labelDescription, 0, 1, 2, 1);
            }
            add(stack, 2, 0, columns - 2, 1);
            if (valueDescription != null) {
                GridPane.setValignment(valueDescription, VPos.TOP);
                add(valueDescription, 2, 1, columns - 2, 1);
            }
        }
    }

    @Override
    public void setupBindings() {
        super.setupBindings();

        editableField.visibleProperty().bind(field.editableProperty());
        readOnlyLabel.visibleProperty().bind(field.editableProperty().not());

        editableField.textProperty().bindBidirectional(field.userInputProperty());
        obfuscatedUserInputBinding = Bindings.createStringBinding(() -> obfuscate(field.getUserInput()),
                field.userInputProperty());
        readOnlyLabel.textProperty().bind(obfuscatedUserInputBinding);
        fieldLabel.textProperty().bind(field.labelProperty());
        editableField.promptTextProperty().bind(field.placeholderProperty());
        editableField.managedProperty().bind(editableField.visibleProperty());
    }

    @Override
    public void setupValueChangedListeners() {
        super.setupValueChangedListeners();
        field.errorMessagesProperty().addListener((observable, oldValue, newValue) -> toggleTooltip(editableField));
        editableField.focusedProperty().addListener((observable, oldValue, newValue) -> toggleTooltip(editableField));
    }

    protected String obfuscate(final String input) {
        if (input == null) {
            return "";
        }
        final var length = input.length();
        final var b      = new StringBuilder();
        for (var i = 0; i < length; i++) {
            b.append('*');
        }
        return b.toString();
    }
}