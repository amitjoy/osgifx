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

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.scene.control.ButtonBar.ButtonData.OK_DONE;
import static org.osgi.service.event.EventConstants.BUNDLE_ID;
import static org.osgi.service.event.EventConstants.BUNDLE_SIGNER;
import static org.osgi.service.event.EventConstants.BUNDLE_SYMBOLICNAME;
import static org.osgi.service.event.EventConstants.BUNDLE_VERSION;
import static org.osgi.service.event.EventConstants.EVENT_TOPIC;
import static org.osgi.service.event.EventConstants.EXCEPTION;
import static org.osgi.service.event.EventConstants.EXCEPTION_CLASS;
import static org.osgi.service.event.EventConstants.EXCEPTION_MESSAGE;
import static org.osgi.service.event.EventConstants.MESSAGE;
import static org.osgi.service.event.EventConstants.SERVICE_ID;
import static org.osgi.service.event.EventConstants.SERVICE_OBJECTCLASS;
import static org.osgi.service.event.EventConstants.SERVICE_PID;
import static org.osgi.service.event.EventConstants.TIMESTAMP;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.controlsfx.control.SegmentedButton;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.fx.core.ThreadSynchronize;

import com.google.common.collect.Lists;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class EventFilterBuilderDialog extends Dialog<String> {

    @Inject
    private ThreadSynchronize threadSync;

    private final TextArea    previewArea = new TextArea();
    private final FilterGroup rootGroup   = new FilterGroup(null);

    public void init(final String existingFilter) {
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("OSGi Event Filter Builder");
        dialogPane.getStylesheets().add(getClass().getClassLoader().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(getClass().getResource("/graphic/images/filter.png").toExternalForm()));

        previewArea.setEditable(false);
        previewArea.setWrapText(true);
        previewArea.setPrefHeight(80);
        previewArea.setPromptText("Generated LDAP Filter Preview");

        final var content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(rootGroup, previewArea);

        dialogPane.setContent(content);

        final var applyButtonType = new ButtonType("Apply", OK_DONE);
        dialogPane.getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == OK_DONE ? rootGroup.toFilterPart().orElse("") : null;
        });

        if (existingFilter != null && !existingFilter.isBlank()) {
            parseFilter(existingFilter, rootGroup);
        } else {
            rootGroup.addCondition(); // Start with one condition if empty
        }
        updatePreview();

        threadSync.asyncExec(() -> dialogPane.getScene().getWindow().sizeToScene());
    }

    private void updatePreview() {
        previewArea.setText(rootGroup.toFilterPart().orElse(""));
    }

    private void styleButton(final Button button) {
        button.setPrefHeight(25);
        button.setMinHeight(25);
        button.setMaxHeight(25);
    }

    private interface FilterNode {
        Optional<String> toFilterPart();

        Node asNode();
    }

    private class FilterGroup extends VBox implements FilterNode {
        private final SegmentedButton  logicGroup;
        private final ToggleButton     btnAnd            = new ToggleButton("AND (&)");
        private final ToggleButton     btnOr             = new ToggleButton("OR (|)");
        private final ToggleButton     btnNot            = new ToggleButton("NOT (!)");
        private final VBox             childrenContainer = new VBox(10);
        private final List<FilterNode> children          = Lists.newArrayList();

        public FilterGroup(final FilterGroup parent) {
            setSpacing(10);
            setPadding(new Insets(10));

            // Boxed style to distinguish groups
            setStyle(
                    "-fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #fcfcfc; -fx-background-radius: 5;");

            btnAnd.setSelected(true);
            btnAnd.setPrefHeight(25);
            btnOr.setPrefHeight(25);
            btnNot.setPrefHeight(25);

            logicGroup = new SegmentedButton(btnAnd, btnOr, btnNot);
            logicGroup.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            logicGroup.setMinHeight(25);
            logicGroup.setMaxHeight(25);

            final var btnAddCondition = new Button("+Condition");
            btnAddCondition.setTooltip(new Tooltip("Add Condition to this group"));
            styleButton(btnAddCondition);
            btnAddCondition.setOnAction(_ -> addCondition());

            final var btnAddGroup = new Button("+Group");
            btnAddGroup.setTooltip(new Tooltip("Add Sub-Group to this group"));
            styleButton(btnAddGroup);
            btnAddGroup.setOnAction(_ -> addGroup());

            final var header = new HBox(10, logicGroup, btnAddCondition, btnAddGroup);
            header.setAlignment(Pos.CENTER_LEFT);

            if (parent != null) {
                final var btnRemove = new Button();
                btnRemove.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));
                styleButton(btnRemove);
                btnRemove.setOnAction(_ -> parent.removeChild(this));
                header.getChildren().add(btnRemove);

                // Indent subgroups to show hierarchy
                setMargin(this, new Insets(0, 0, 0, 20));
            }

            getChildren().addAll(header, childrenContainer);

            btnAnd.selectedProperty().addListener((_, _, _) -> updatePreview());
            btnOr.selectedProperty().addListener((_, _, _) -> updatePreview());
            btnNot.selectedProperty().addListener((_, _, _) -> updatePreview());
        }

        public FilterRow addCondition() {
            final var row = new FilterRow(this);
            children.add(row);
            childrenContainer.getChildren().add(row);
            threadSync.asyncExec(() -> getDialogPane().getScene().getWindow().sizeToScene());
            updatePreview();
            return row;
        }

        public FilterGroup addGroup() {
            final var group = new FilterGroup(this);
            children.add(group);
            childrenContainer.getChildren().add(group);
            threadSync.asyncExec(() -> getDialogPane().getScene().getWindow().sizeToScene());
            updatePreview();
            return group;
        }

        public void removeChild(final FilterNode child) {
            if (children.size() > 1 || child instanceof FilterGroup) {
                children.remove(child);
                childrenContainer.getChildren().remove(child.asNode());
                threadSync.asyncExec(() -> getDialogPane().getScene().getWindow().sizeToScene());
                updatePreview();
            }
        }

        public void setOperator(final String op) {
            if ("&".equals(op))
                btnAnd.setSelected(true);
            else if ("|".equals(op))
                btnOr.setSelected(true);
            else if ("!".equals(op))
                btnNot.setSelected(true);
        }

        @Override
        public Optional<String> toFilterPart() {
            final var parts = children.stream().map(FilterNode::toFilterPart).filter(Optional::isPresent)
                    .map(Optional::get).toList();
            if (parts.isEmpty()) {
                return Optional.empty();
            }

            if (btnNot.isSelected()) {
                // NOT must have exactly one operand
                return Optional.of("(!" + parts.get(0) + ")");
            }

            // Always wrap in operator even if only one child, for spec clarity in nested groups
            final var op = btnAnd.isSelected() ? "&" : "|";
            return Optional.of("(" + op + String.join("", parts) + ")");
        }

        @Override
        public Node asNode() {
            return this;
        }
    }

    private class FilterRow extends HBox implements FilterNode {
        private final ComboBox<String> itemCombo;
        private final ComboBox<String> operatorCombo;
        private final CustomTextField  valueField;

        public FilterRow(final FilterGroup parent) {
            setSpacing(5);
            setAlignment(Pos.CENTER_LEFT);

            itemCombo = new ComboBox<>(FXCollections.observableArrayList(EVENT_TOPIC, TIMESTAMP, MESSAGE, EXCEPTION,
                    EXCEPTION_CLASS, EXCEPTION_MESSAGE, BUNDLE_ID, BUNDLE_SYMBOLICNAME, BUNDLE_VERSION, BUNDLE_SIGNER,
                    SERVICE_ID, SERVICE_PID, SERVICE_OBJECTCLASS));
            itemCombo.setEditable(true);
            itemCombo.setPromptText("Property");
            itemCombo.setPrefWidth(150);
            itemCombo.setPrefHeight(25);

            operatorCombo = new ComboBox<>(FXCollections.observableArrayList("=", "~=", ">=", "<=", "!", "!="));
            operatorCombo.setValue("=");
            operatorCombo.setPrefWidth(80);
            operatorCombo.setPrefHeight(25);

            valueField = (CustomTextField) TextFields.createClearableTextField();
            valueField.setPromptText("Value");
            valueField.setPrefHeight(25);
            HBox.setHgrow(valueField, Priority.ALWAYS);

            final var btnRemove = new Button();
            btnRemove.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));
            styleButton(btnRemove);
            btnRemove.setOnAction(_ -> parent.removeChild(this));

            itemCombo.valueProperty().addListener((_, _, _) -> updatePreview());
            operatorCombo.valueProperty().addListener((_, _, _) -> updatePreview());
            valueField.textProperty().addListener((_, _, _) -> updatePreview());

            getChildren().addAll(itemCombo, operatorCombo, valueField, btnRemove);
        }

        public void setItem(final String item) {
            itemCombo.setValue(item);
        }

        public void setOperator(final String op) {
            operatorCombo.setValue(op);
        }

        public void setValue(final String val) {
            valueField.setText(val);
        }

        @Override
        public Optional<String> toFilterPart() {
            final var item = itemCombo.getValue();
            final var op   = operatorCombo.getValue();
            final var val  = valueField.getText();

            if (item == null || item.isBlank() || val == null || val.isBlank()) {
                return Optional.empty();
            }
            if ("!".equals(op)) {
                return Optional.of("(!(" + item + "=*))");
            }
            if ("!=".equals(op)) {
                return Optional.of("(!(" + item + "=" + val + "))");
            }
            return Optional.of("(" + item + op + val + ")");
        }

        @Override
        public Node asNode() {
            return this;
        }
    }

    // --- Parser Implementation ---

    private void parseFilter(String filter, final FilterGroup target) {
        if (filter == null || filter.isBlank())
            return;
        filter = filter.trim();
        if (filter.startsWith("(") && filter.endsWith(")")) {
            final String inner = filter.substring(1, filter.length() - 1).trim();
            if (inner.startsWith("&")) {
                target.setOperator("&");
                parseChildren(inner.substring(1).trim(), target);
            } else if (inner.startsWith("|")) {
                target.setOperator("|");
                parseChildren(inner.substring(1).trim(), target);
            } else if (inner.startsWith("!")) {
                target.setOperator("!");
                parseChildren(inner.substring(1).trim(), target);
            } else {
                final var row = target.addCondition();
                parseAssertion(inner, row);
            }
        }
    }

    private void parseChildren(String content, final FilterGroup target) {
        while (!content.isEmpty()) {
            final int end = findMatchingParenthesis(content, 0);
            if (end == -1)
                break;
            final String child = content.substring(0, end + 1);
            if (child.startsWith("(&") || child.startsWith("(|") || child.startsWith("(!")) {
                final FilterGroup subGroup = target.addGroup();
                parseFilter(child, subGroup);
            } else {
                final var row = target.addCondition();
                parseAssertion(child.substring(1, child.length() - 1), row);
            }
            content = content.substring(end + 1).trim();
        }
    }

    private void parseAssertion(final String inner, final FilterRow row) {
        final String[] ops = { "~=", ">=", "<=", "=", "!" };
        for (final String op : ops) {
            final int idx = inner.indexOf(op);
            if (idx != -1) {
                final String key = inner.substring(0, idx).trim();
                final String val = inner.substring(idx + op.length()).trim();
                row.setItem(key);
                row.setOperator(op);
                row.setValue(val);
                return;
            }
        }
    }

    private int findMatchingParenthesis(final String str, final int start) {
        int balance = 0;
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == '(')
                balance++;
            else if (str.charAt(i) == ')')
                balance--;
            if (balance == 0)
                return i;
        }
        return -1;
    }
}
