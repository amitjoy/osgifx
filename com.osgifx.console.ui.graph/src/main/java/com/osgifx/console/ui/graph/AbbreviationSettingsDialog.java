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
package com.osgifx.console.ui.graph;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

/**
 * A dialog that allows users to manage prefix abbreviation rules for graph
 * labels. Each rule maps a prefix to a replacement string.
 */
public final class AbbreviationSettingsDialog extends Dialog<List<AbbreviationRule>> {

    private static final class RuleModel {
        private final StringProperty prefix;
        private final StringProperty replacement;

        RuleModel(final String prefix, final String replacement) {
            this.prefix      = new SimpleStringProperty(prefix);
            this.replacement = new SimpleStringProperty(replacement);
        }

        StringProperty prefixProperty() {
            return prefix;
        }

        StringProperty replacementProperty() {
            return replacement;
        }

        AbbreviationRule toRule() {
            return new AbbreviationRule(prefix.get(), replacement.get());
        }
    }

    private final ObservableList<RuleModel> rulesData = FXCollections.observableArrayList();

    public AbbreviationSettingsDialog(final List<AbbreviationRule> existingRules) {
        if (existingRules != null) {
            existingRules.forEach(r -> rulesData.add(new RuleModel(r.prefix(), r.replacement())));
        }
        init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        setTitle("Graph Label Abbreviation Settings");
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());

        // --- Table ---
        final var table = new TableView<>(rulesData);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(300);
        table.setPrefWidth(500);

        final var prefixCol = new TableColumn<RuleModel, String>("Prefix");
        prefixCol.setCellValueFactory(cell -> cell.getValue().prefixProperty());
        prefixCol.setCellFactory(_ -> new EditingCell());
        prefixCol.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).prefixProperty()
                .set(t.getNewValue()));

        final var replacementCol = new TableColumn<RuleModel, String>("Replacement");
        replacementCol.setCellValueFactory(cell -> cell.getValue().replacementProperty());
        replacementCol.setCellFactory(_ -> new EditingCell());
        replacementCol.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow())
                .replacementProperty().set(t.getNewValue()));

        table.getColumns().addAll(prefixCol, replacementCol);

        // --- Buttons ---
        final var addBtn = new Button("Add");
        addBtn.setOnAction(_ -> {
            final var newRule = new RuleModel("", "");
            rulesData.add(newRule);
            table.getSelectionModel().select(newRule);
            // Focus and start editing the new row immediately
            table.edit(rulesData.size() - 1, prefixCol);
        });

        final var removeBtn = new Button("Remove");
        removeBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        removeBtn.setOnAction(_ -> {
            final var selectedIndex = table.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                rulesData.remove(selectedIndex);
            }
        });

        final var buttonBar = new HBox(5, addBtn, removeBtn);
        buttonBar.setPadding(new Insets(5, 0, 0, 0));

        // --- Hint label ---
        final var hint = new Label("Click a cell to edit. Changes are saved automatically when you click away.");
        hint.setStyle("-fx-font-size: 11; -fx-text-fill: gray;");
        hint.setWrapText(true);

        // --- Layout ---
        final var content = new VBox(8, table, buttonBar, hint);
        content.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        dialogPane.setContent(content);
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                // Filter out empty prefixes before returning
                return rulesData.stream().map(RuleModel::toRule)
                        .filter(r -> r.prefix() != null && !r.prefix().trim().isEmpty()).toList();
            }
            return null;
        });
    }

    /**
     * A custom TableCell that commits the value on focus loss, preventing data loss
     * when clicking between cells or out of the table.
     */
    private static class EditingCell extends TableCell<RuleModel, String> {
        private TextField textField;

        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
            // Commit on focus loss
            textField.focusedProperty().addListener((_, _, newVal) -> {
                if (!newVal && isEditing()) {
                    commitEdit(textField.getText());
                }
            });
            // Handle keyboard navigation
            textField.setOnKeyPressed(t -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(textField.getText());
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }
    }

}
