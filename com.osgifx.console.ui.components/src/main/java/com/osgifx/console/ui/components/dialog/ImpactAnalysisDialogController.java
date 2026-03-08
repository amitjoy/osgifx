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
package com.osgifx.console.ui.components.dialog;

import java.util.Collection;

import javax.inject.Inject;

import org.controlsfx.control.MaskerPane;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.components.model.ImpactAnalyzer;
import com.osgifx.console.ui.components.model.XImpactDTO;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public final class ImpactAnalysisDialogController {

    @FXML
    private Glyph                           headerIcon;
    @FXML
    private Label                           headerLabel;
    @FXML
    private Label                           summaryLabel;
    @FXML
    private TableView<XImpactDTO>           impactTable;
    @FXML
    private TableColumn<XImpactDTO, String> affectedItemColumn;
    @FXML
    private TableColumn<XImpactDTO, String> impactTypeColumn;
    @FXML
    private MaskerPane                      maskerPane;
    @FXML
    private Label                           descriptionLabel;
    @Inject
    private Executor                        executor;
    @Log
    @Inject
    private FluentLogger                    logger;
    @Inject
    private ThreadSynchronize               threadSync;

    @FXML
    public void initialize() {
        affectedItemColumn.setCellValueFactory(data -> data.getValue().affectedItemProperty());
        impactTypeColumn.setCellValueFactory(data -> data.getValue().impactTypeProperty());

        impactTypeColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    final var hBox = new HBox(5);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    Glyph icon;
                    switch (item) {
                        case "ACTIVATION":
                        case "SATISFACTION":
                            icon = new Glyph("FontAwesome", FontAwesome.Glyph.CHECK_CIRCLE).color(Color.GREEN);
                            break;
                        case "DEACTIVATION":
                            icon = new Glyph("FontAwesome", FontAwesome.Glyph.EXCLAMATION_TRIANGLE).color(Color.RED);
                            break;
                        case "SERVICE_UNBIND":
                            icon = new Glyph("FontAwesome", FontAwesome.Glyph.BOLT).color(Color.BLUEVIOLET);
                            break;
                        case "DEPENDENCY":
                            icon = new Glyph("FontAwesome", FontAwesome.Glyph.LINK).color(Color.GRAY);
                            break;
                        default:
                            icon = new Glyph("FontAwesome", FontAwesome.Glyph.INFO_CIRCLE).color(Color.GRAY);
                            break;
                    }
                    hBox.getChildren().addAll(icon, new Label(item));
                    setGraphic(hBox);
                }
            }
        });

        impactTable.getSelectionModel().selectedItemProperty().addListener((_, _, newSelection) -> {
            if (newSelection != null) {
                descriptionLabel.setText(newSelection.getDescription());
            } else {
                descriptionLabel.setText("");
            }
        });

        logger.atInfo().log("ImpactAnalysisDialogController initialized");
    }

    public void analyze(final String action,
                        final Collection<XComponentDTO> selection,
                        final Collection<XComponentDTO> allComponents,
                        final Collection<XBundleDTO> allBundles) {
        final var target = selection.size() == 1 ? selection.iterator().next().name : selection.size() + " components";

        headerLabel.setText(action + ": " + target);
        if ("DISABLE".equalsIgnoreCase(action)) {
            headerIcon.setIcon(FontAwesome.Glyph.STOP);
            headerIcon.setStyle("-fx-text-fill: #dc2626;");
        } else if ("ENABLE".equalsIgnoreCase(action)) {
            headerIcon.setIcon(FontAwesome.Glyph.PLAY_CIRCLE);
            headerIcon.setStyle("-fx-text-fill: #16a34a;");
        }

        summaryLabel.setText("Analyzing the potential impact of performing " + action + " on " + target + "...");
        maskerPane.setVisible(true);

        executor.supplyAsync(() -> {
            if ("DISABLE".equalsIgnoreCase(action)) {
                return ImpactAnalyzer.calculateDisableImpact(selection, allComponents, allBundles);
            }
            if ("ENABLE".equalsIgnoreCase(action)) {
                return ImpactAnalyzer.calculateEnableImpact(selection, allComponents, allBundles);
            }
            return FXCollections.<XImpactDTO> emptyObservableList();
        }).thenAccept(impacts -> {
            threadSync.asyncExec(() -> {
                impactTable.setItems(FXCollections.observableArrayList(impacts));
                final var impactCount = impacts.size();
                summaryLabel.setText(String.format("The %s action on %s will affect %d %s.", action, target,
                        impactCount, impactCount == 1 ? "item" : "items"));
                maskerPane.setVisible(false);
            });
        }).exceptionally(e -> {
            logger.atError().withException(e).log("Error analyzing impacts");
            threadSync.asyncExec(() -> maskerPane.setVisible(false));
            return null;
        });
    }
}
