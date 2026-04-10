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
package com.osgifx.console.ui.rsa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.osgifx.console.agent.dto.XRemoteServiceDTO;
import com.osgifx.console.util.fx.Fx;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public final class RsaDetailsFxController {

    @Log
    @Inject
    private FluentLogger logger;
    @FXML
    private BorderPane   rootPanel;
    private FormRenderer formRenderer;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    public void initControls(final XRemoteServiceDTO rsaEntry) {
        if (formRenderer != null) {
            rootPanel.getChildren().remove(formRenderer);
        }
        formRenderer = createForm(rsaEntry);
        formRenderer.setOpacity(0.0);
        rootPanel.setCenter(formRenderer);

        scheduleLayoutFix(formRenderer, 0);
    }

    private void scheduleLayoutFix(final Node node, final int attempt) {
        if (attempt > 50) {
            logger.atWarning().log("Could not find GridPanes after 50 attempts");
            return;
        }

        final List<GridPane> grids = findAllGridPanesRecursively(node);

        if (!grids.isEmpty()) {
            applyGridConstraints(grids);
            Fx.makeReadOnlyLabelsCopyable(node);
            node.setOpacity(1.0);
        } else {
            if (node instanceof Parent p) {
                p.layout();
            }
            final var timer = new PauseTransition(Duration.millis(200));
            timer.setOnFinished(_ -> scheduleLayoutFix(node, attempt + 1));
            timer.play();
        }
    }

    private List<GridPane> findAllGridPanesRecursively(final Node node) {
        final List<GridPane> results = new ArrayList<>();
        if (node instanceof GridPane grid) {
            results.add(grid);
        }
        if (node instanceof final Parent parent) {
            for (final Node child : parent.getChildrenUnmodifiable()) {
                results.addAll(findAllGridPanesRecursively(child));
            }
        }
        return results;
    }

    private void applyGridConstraints(final List<GridPane> grids) {
        for (final GridPane grid : grids) {
            grid.setHgap(5);

            grid.getRowConstraints().clear();

            grid.getColumnConstraints().clear();

            for (final Node child : grid.getChildren()) {
                if (child instanceof Label label && label.getStyleClass().contains("formsfx-label")) {
                    label.setWrapText(true);
                    label.setMinHeight(Region.USE_COMPUTED_SIZE);
                    label.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    label.setMaxHeight(Double.MAX_VALUE);

                    label.setAlignment(Pos.TOP_LEFT);
                    label.setTextAlignment(TextAlignment.LEFT);
                    label.setMaxWidth(Double.MAX_VALUE);
                    GridPane.setHalignment(label, HPos.LEFT);
                    GridPane.setFillWidth(label, true);

                    label.setPadding(new Insets(3, 0, 0, 0));
                    GridPane.setValignment(label, VPos.TOP);
                }
            }

            // Enforce a symmetric 12-column grid
            // Columns 0,1 (Label 1) and 6,7 (Label 2) get 17.5% each (35% total per label)
            // Remaining columns (Inputs) get 3.75% each (15% total per input in 2-col layout)
            final List<ColumnConstraints> constraints = new ArrayList<>();

            for (int i = 0; i < 12; i++) {
                final var col = new ColumnConstraints();
                if (i == 0 || i == 1 || i == 6 || i == 7) {
                    col.setPercentWidth(17.5);
                } else {
                    col.setPercentWidth(3.75);
                }
                constraints.add(col);
            }

            grid.getColumnConstraints().clear();
            grid.getColumnConstraints().addAll(constraints);
        }
    }

    private FormRenderer createForm(final XRemoteServiceDTO rsaEntry) {
        final var form = Form
                .of(Section.of(initBasicProperties(rsaEntry).toArray(new Field[0])).title("Endpoint Information"),
                        Section.of(initCustomProperties(rsaEntry.properties).toArray(new Field[0]))
                                .title("Endpoint Properties"))
                .title("Remote Service Details");

        final var renderer = new FormRenderer(form);

        Fx.addCopySupportToReadOnlyLabels(renderer);
        return renderer;
    }

    private List<Field<?>> initBasicProperties(final XRemoteServiceDTO rsaEntry) {
        final List<Field<?>> fields = new ArrayList<>();

        fields.add(Field.ofStringType(rsaEntry.id == null ? "" : rsaEntry.id).editable(false).label("Endpoint ID"));

        fields.add(Field.ofStringType(rsaEntry.direction == null ? "" : rsaEntry.direction.toString()).editable(false)
                .label("Direction"));

        fields.add(Field.ofStringType(rsaEntry.frameworkUUID == null ? "" : rsaEntry.frameworkUUID).editable(false)
                .label("Framework UUID"));

        if (rsaEntry.objectClass != null) {
            fields.add(Field.ofMultiSelectionType(rsaEntry.objectClass).label("Object Classes"));
        }

        if (rsaEntry.localServiceId != null) {
            fields.add(
                    Field.ofStringType(rsaEntry.localServiceId.toString()).editable(false).label("Local Service ID"));
        }

        if (rsaEntry.provider != null) {
            fields.add(Field.ofStringType(rsaEntry.provider).editable(false).label("Provider"));
        }

        if (rsaEntry.intents != null && !rsaEntry.intents.isEmpty()) {
            fields.add(Field.ofMultiSelectionType(rsaEntry.intents).label("Intents"));
        }

        return fields;
    }

    private List<Field<?>> initCustomProperties(final Map<String, Object> properties) {
        final List<Field<?>> fields = new ArrayList<>();

        if (properties != null) {
            properties.forEach((key, value) -> {
                final String strValue = value == null ? "" : value.toString();
                fields.add(Field.ofStringType(strValue).editable(false).label(key));
            });
        }

        return fields;
    }
}
