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
package com.osgifx.console.application.ui;

import static com.google.common.base.Preconditions.checkNotNull;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.scene.paint.Color.GREEN;
import static javafx.scene.paint.Color.RED;
import static javafx.scene.paint.Color.TRANSPARENT;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.StatusBar;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.e4.core.di.annotations.Optional;

import com.osgifx.console.api.RpcCallInfo;
import com.osgifx.console.api.RpcProgressTracker;
import com.osgifx.console.ui.ConsoleStatusBar;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;

public final class ConsoleStatusBarProvider implements ConsoleStatusBar {

    @Inject
    @Optional
    @Named("connected.agent")
    private String connectedAgent;

    @Inject
    @Optional
    private RpcProgressTracker rpcProgressTracker;

    private final StatusBar statusBar = new StatusBar();
    private Button          rpcProgressButton;
    private PopOver         rpcProgressPopover;

    @Override
    public void addTo(final BorderPane pane) {
        final var glyph = new Glyph("FontAwesome", FontAwesome.Glyph.DESKTOP);
        glyph.useGradientEffect();
        glyph.useHoverEffect();

        final var button = new Button("", glyph);
        button.setBackground(new Background(new BackgroundFill(TRANSPARENT, new CornerRadii(2), new Insets(4))));

        statusBar.getLeftItems().clear();
        statusBar.getLeftItems().add(button);
        statusBar.getLeftItems().add(new Separator(VERTICAL));

        final String statusBarText;
        if (connectedAgent != null) {
            glyph.color(GREEN);
            statusBarText = connectedAgent;
        } else {
            glyph.color(RED);
            statusBarText = "Disconnected";
        }
        statusBar.setText(statusBarText);
        pane.setBottom(statusBar);
    }

    @Override
    public DoubleProperty progressProperty() {
        return statusBar.progressProperty();
    }

    @Override
    public void addToLeft(final Node node) {
        checkNotNull(node, "Specified node cannot be null");
        statusBar.getLeftItems().add(node);
    }

    @Override
    public void addToRight(final Node node) {
        checkNotNull(node, "Specified node cannot be null");
        statusBar.getRightItems().add(node);
    }

    @Override
    public void clearAllInLeft() {
        statusBar.getLeftItems().clear();
    }

    @Override
    public void clearAllInRight() {
        statusBar.getRightItems().clear();
        // Reset RPC button reference since it was removed
        rpcProgressButton = null;
        if (rpcProgressPopover != null && rpcProgressPopover.isShowing()) {
            rpcProgressPopover.hide();
        }
        rpcProgressPopover = null;
    }

    @Override
    public void enableRpcProgressTracking() {
        if (rpcProgressTracker == null) {
            return; // Tracker not available
        }
        
        // Check if button is already added to this status bar instance
        if (rpcProgressButton != null && statusBar.getRightItems().contains(rpcProgressButton)) {
            return; // Already enabled in this status bar
        }

        // Create RPC progress button (always visible)
        final var glyph = new Glyph("FontAwesome", FontAwesome.Glyph.TASKS);
        glyph.useGradientEffect();
        glyph.useHoverEffect();

        rpcProgressButton = new Button("", glyph);
        rpcProgressButton
                .setBackground(new Background(new BackgroundFill(TRANSPARENT, new CornerRadii(2), new Insets(4))));
        rpcProgressButton.setTooltip(new javafx.scene.control.Tooltip("RPC Progress"));

        // Update button style based on active RPC count
        rpcProgressTracker.activeRpcCountProperty().addListener((_, _, newVal) -> {
            if (newVal.intValue() == 0 && rpcProgressPopover != null && rpcProgressPopover.isShowing()) {
                rpcProgressPopover.hide();
            }
        });

        // Create popover content
        final VBox popoverContent = createRpcPopoverContent();
        rpcProgressPopover = new PopOver(popoverContent);
        rpcProgressPopover.setTitle("Active RPC Calls");
        rpcProgressPopover.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        rpcProgressPopover.setDetachable(false);

        // Show popover on button click
        rpcProgressButton.setOnAction(_ -> {
            if (rpcProgressPopover.isShowing()) {
                rpcProgressPopover.hide();
            } else {
                rpcProgressPopover.show(rpcProgressButton);
            }
        });

        // Add to status bar (right side)
        statusBar.getRightItems().add(new Separator(VERTICAL));
        statusBar.getRightItems().add(rpcProgressButton);
    }

    @Override
    public void disableRpcProgressTracking() {
        if (rpcProgressButton != null) {
            statusBar.getRightItems().remove(rpcProgressButton);
            // Also remove the separator before the button if it exists
            if (!statusBar.getRightItems().isEmpty()) {
                final Node lastItem = statusBar.getRightItems().get(statusBar.getRightItems().size() - 1);
                if (lastItem instanceof Separator) {
                    statusBar.getRightItems().remove(lastItem);
                }
            }
            rpcProgressButton = null;
            if (rpcProgressPopover != null && rpcProgressPopover.isShowing()) {
                rpcProgressPopover.hide();
            }
            rpcProgressPopover = null;
        }
    }

    @Override
    public Node getRpcProgressButton() {
        return rpcProgressButton;
    }

    private VBox createRpcPopoverContent() {
        final VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(500);
        content.setPrefHeight(300);

        // Create table for active RPC calls
        final TableView<RpcCallInfo> table = new TableView<>();
        table.setItems(rpcProgressTracker.getActiveRpcCalls());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Method name column
        final TableColumn<RpcCallInfo, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(data -> data.getValue().descriptionProperty());
        methodCol.setPrefWidth(200);

        // Progress column
        final TableColumn<RpcCallInfo, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setCellValueFactory(data -> data.getValue().progressProperty().asObject());
        progressCol.setCellFactory(_ -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            {
                progressBar.setPrefWidth(150);
            }

            @Override
            protected void updateItem(Double progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                } else {
                    progressBar.setProgress(progress);
                    setGraphic(progressBar);
                }
            }
        });
        progressCol.setPrefWidth(150);

        // Duration column
        final TableColumn<RpcCallInfo, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(data -> {
            final long   durationMs  = data.getValue().getDurationMs();
            final String durationStr = String.format("%.1fs", durationMs / 1000.0);
            return new SimpleStringProperty(durationStr);
        });
        durationCol.setPrefWidth(80);

        table.getColumns().add(methodCol);
        table.getColumns().add(progressCol);
        table.getColumns().add(durationCol);

        // Add placeholder for empty table
        final Label placeholder = new Label("No active RPC calls");
        table.setPlaceholder(placeholder);

        content.getChildren().add(table);
        return content;
    }

}
