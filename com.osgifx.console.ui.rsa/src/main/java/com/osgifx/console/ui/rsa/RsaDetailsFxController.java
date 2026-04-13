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
import com.osgifx.console.agent.dto.RemoteServiceDirection;
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
        final List<String> handledKeys    = new ArrayList<>();
        final var          identification = initIdentificationProperties(rsaEntry, handledKeys);
        final var          service        = initServiceProperties(rsaEntry, handledKeys);
        final var          distribution   = initDistributionProperties(rsaEntry, handledKeys);
        final var          ecf            = initEcfProperties(rsaEntry.properties, handledKeys);
        final var          aries          = initAriesProperties(rsaEntry.properties, handledKeys);
        final var          custom         = initCustomProperties(rsaEntry.properties, handledKeys);

        final List<Section> sections = new ArrayList<>();
        if (!identification.isEmpty()) {
            sections.add(Section.of(identification.toArray(new Field[0])).title("Endpoint Identification"));
        }
        if (!service.isEmpty()) {
            sections.add(Section.of(service.toArray(new Field[0])).title("Service Attributes"));
        }
        if (!distribution.isEmpty()) {
            sections.add(Section.of(distribution.toArray(new Field[0])).title("Distribution Metadata"));
        }
        if (!ecf.isEmpty()) {
            sections.add(Section.of(ecf.toArray(new Field[0])).title("ECF Metadata"));
        }
        if (!aries.isEmpty()) {
            sections.add(Section.of(aries.toArray(new Field[0])).title("Aries Metadata"));
        }
        if (!custom.isEmpty()) {
            sections.add(Section.of(custom.toArray(new Field[0])).title("Custom Properties"));
        }

        final var form = Form.of(sections.toArray(new Section[0])).title("Remote Service Details");

        final var renderer = new FormRenderer(form);

        Fx.addCopySupportToReadOnlyLabels(renderer);
        return renderer;
    }

    private List<Field<?>> initIdentificationProperties(final XRemoteServiceDTO rsaEntry,
                                                        final List<String> handledKeys) {
        final List<Field<?>> fields = new ArrayList<>();

        final String endpointId = (String) rsaEntry.properties.getOrDefault("endpoint.id", rsaEntry.id);
        fields.add(Field.ofStringType(endpointId == null ? "" : endpointId).editable(false).label("Endpoint ID"));
        handledKeys.add("endpoint.id");
        handledKeys.add("service.remote.endpoint.id");

        if (rsaEntry.localServiceId != null) {
            final String label = rsaEntry.direction == RemoteServiceDirection.IMPORT ? "Remote Service ID"
                    : "Local Service ID";
            fields.add(Field.ofStringType(rsaEntry.localServiceId.toString()).editable(false).label(label));
        }
        handledKeys.add("endpoint.service.id");

        final String frameworkUUID = (String) rsaEntry.properties.getOrDefault("endpoint.framework.uuid",
                rsaEntry.frameworkUUID);
        fields.add(
                Field.ofStringType(frameworkUUID == null ? "" : frameworkUUID).editable(false).label("Framework UUID"));
        handledKeys.add("endpoint.framework.uuid");
        handledKeys.add("service.remote.framework.uuid");

        return fields;
    }

    private List<Field<?>> initServiceProperties(final XRemoteServiceDTO rsaEntry, final List<String> handledKeys) {
        final List<Field<?>> fields = new ArrayList<>();

        final Object objectClass = rsaEntry.properties.getOrDefault("objectClass", rsaEntry.objectClass);
        if (objectClass instanceof final Iterable<?> iterable) {
            final List<String> stringList = new ArrayList<>();
            iterable.forEach(o -> stringList.add(o.toString()));
            fields.add(Field.ofMultiSelectionType(stringList).label("Object Classes"));
        } else if (objectClass instanceof final String[] array) {
            fields.add(Field.ofMultiSelectionType(List.of(array)).label("Object Classes"));
        } else if (objectClass != null) {
            fields.add(Field.ofStringType(objectClass.toString()).editable(false).label("Object Classes"));
        }
        handledKeys.add("objectClass");

        final Object exportedInterfaces = rsaEntry.properties.get("service.exported.interfaces");
        if (exportedInterfaces != null) {
            if (exportedInterfaces instanceof final Iterable<?> iterable) {
                final List<String> stringList = new ArrayList<>();
                iterable.forEach(o -> stringList.add(o.toString()));
                fields.add(Field.ofMultiSelectionType(stringList).label("Exported Interfaces"));
            } else if (exportedInterfaces instanceof final String[] array) {
                fields.add(Field.ofMultiSelectionType(List.of(array)).label("Exported Interfaces"));
            } else {
                fields.add(
                        Field.ofStringType(exportedInterfaces.toString()).editable(false).label("Exported Interfaces"));
            }
            handledKeys.add("service.exported.interfaces");
        }

        final Object asyncInterfaces = rsaEntry.properties.get("ecf.exported.async.interfaces");
        if (asyncInterfaces != null) {
            if (asyncInterfaces instanceof final Iterable<?> iterable) {
                final List<String> stringList = new ArrayList<>();
                iterable.forEach(o -> stringList.add(o.toString()));
                fields.add(Field.ofMultiSelectionType(stringList).label("Async Interfaces"));
            } else if (asyncInterfaces instanceof final String[] array) {
                fields.add(Field.ofMultiSelectionType(List.of(array)).label("Async Interfaces"));
            } else {
                fields.add(Field.ofStringType(asyncInterfaces.toString()).editable(false).label("Async Interfaces"));
            }
            handledKeys.add("ecf.exported.async.interfaces");
        }

        final List<String> packageVersions = new ArrayList<>();
        rsaEntry.properties.forEach((k, v) -> {
            if (k.startsWith("endpoint.package.version.")) {
                final String packageName = k.substring("endpoint.package.version.".length());
                packageVersions.add(packageName + "=" + v);
                handledKeys.add(k);
            }
        });
        if (!packageVersions.isEmpty()) {
            fields.add(Field.ofMultiSelectionType(packageVersions).label("Package Versions"));
        }

        return fields;
    }

    private List<Field<?>> initDistributionProperties(final XRemoteServiceDTO rsaEntry,
                                                      final List<String> handledKeys) {
        final List<Field<?>> fields = new ArrayList<>();

        fields.add(Field.ofStringType(rsaEntry.direction == null ? "" : rsaEntry.direction.toString()).editable(false)
                .label("Direction"));

        if (rsaEntry.provider != null) {
            final String label = (rsaEntry.provider.startsWith("ecf") || rsaEntry.provider.startsWith("aries"))
                    ? "Container ID"
                    : "Provider";
            fields.add(Field.ofStringType(rsaEntry.provider).editable(false).label(label));
        }
        handledKeys.add("endpoint.service.sender.id");
        handledKeys.add("ecf.endpoint.id");
        handledKeys.add("aries.tcp.id");
        handledKeys.add("aries.fastbin.id");

        final Object importedConfigs = rsaEntry.properties.get("service.imported.configs");
        if (importedConfigs != null) {
            if (importedConfigs instanceof final Iterable<?> iterable) {
                final List<String> stringList = new ArrayList<>();
                iterable.forEach(o -> stringList.add(o.toString()));
                fields.add(Field.ofMultiSelectionType(stringList).label("Used Configurations"));
            } else if (importedConfigs instanceof final String[] array) {
                fields.add(Field.ofMultiSelectionType(List.of(array)).label("Used Configurations"));
            } else {
                fields.add(Field.ofStringType(importedConfigs.toString()).editable(false).label("Used Configurations"));
            }
            handledKeys.add("service.imported.configs");
        }

        final Object exportedConfigs = rsaEntry.properties.get("service.exported.configs");
        if (exportedConfigs != null) {
            if (exportedConfigs instanceof final Iterable<?> iterable) {
                final List<String> stringList = new ArrayList<>();
                iterable.forEach(o -> stringList.add(o.toString()));
                fields.add(Field.ofMultiSelectionType(stringList).label("Requested Configurations"));
            } else if (exportedConfigs instanceof final String[] array) {
                fields.add(Field.ofMultiSelectionType(List.of(array)).label("Requested Configurations"));
            } else {
                fields.add(Field.ofStringType(exportedConfigs.toString()).editable(false)
                        .label("Requested Configurations"));
            }
            handledKeys.add("service.exported.configs");
        }

        final Object intents = rsaEntry.properties.getOrDefault("service.intents", rsaEntry.intents);
        if (intents instanceof final Iterable<?> iterable) {
            final List<String> stringList = new ArrayList<>();
            iterable.forEach(o -> stringList.add(o.toString()));
            fields.add(Field.ofMultiSelectionType(stringList).label("Intents"));
        } else if (intents instanceof final String[] array) {
            fields.add(Field.ofMultiSelectionType(List.of(array)).label("Intents"));
        } else if (intents != null) {
            fields.add(Field.ofStringType(intents.toString()).editable(false).label("Intents"));
        }
        handledKeys.add("service.intents");

        final Object exportedIntents = rsaEntry.properties.get("service.exported.intents");
        if (exportedIntents != null) {
            if (exportedIntents instanceof final Iterable<?> iterable) {
                final List<String> stringList = new ArrayList<>();
                iterable.forEach(o -> stringList.add(o.toString()));
                fields.add(Field.ofMultiSelectionType(stringList).label("Exported Intents"));
            } else if (exportedIntents instanceof final String[] array) {
                fields.add(Field.ofMultiSelectionType(List.of(array)).label("Exported Intents"));
            } else {
                fields.add(Field.ofStringType(exportedIntents.toString()).editable(false).label("Exported Intents"));
            }
            handledKeys.add("service.exported.intents");
        }

        final Object extraExportedIntents = rsaEntry.properties.get("service.exported.intents.extra");
        if (extraExportedIntents != null) {
            if (extraExportedIntents instanceof final Iterable<?> iterable) {
                final List<String> stringList = new ArrayList<>();
                iterable.forEach(o -> stringList.add(o.toString()));
                fields.add(Field.ofMultiSelectionType(stringList).label("Extra Exported Intents"));
            } else if (extraExportedIntents instanceof final String[] array) {
                fields.add(Field.ofMultiSelectionType(List.of(array)).label("Extra Exported Intents"));
            } else {
                fields.add(Field.ofStringType(extraExportedIntents.toString()).editable(false)
                        .label("Extra Exported Intents"));
            }
            handledKeys.add("service.exported.intents.extra");
        }

        return fields;
    }

    private List<Field<?>> initEcfProperties(final Map<String, Object> properties, final List<String> handledKeys) {
        final List<Field<?>> fields = new ArrayList<>();

        if (properties != null) {
            properties.forEach((key, value) -> {
                if ((key.startsWith("ecf.") || key.startsWith("org.eclipse.ecf.") || key.startsWith("zoodiscovery."))
                        && !handledKeys.contains(key)) {
                    final String strValue = value == null ? "" : value.toString();
                    fields.add(Field.ofStringType(strValue).editable(false).label(key));
                    handledKeys.add(key);
                }
            });
        }

        return fields;
    }

    private List<Field<?>> initAriesProperties(final Map<String, Object> properties, final List<String> handledKeys) {
        final List<Field<?>> fields = new ArrayList<>();

        if (properties != null) {
            properties.forEach((key, value) -> {
                if ((key.startsWith("aries.") || key.startsWith("org.apache.aries.") || key.startsWith("zookeeper."))
                        && !handledKeys.contains(key)) {
                    final String strValue = value == null ? "" : value.toString();
                    fields.add(Field.ofStringType(strValue).editable(false).label(key));
                    handledKeys.add(key);
                }
            });
        }

        return fields;
    }

    private List<Field<?>> initCustomProperties(final Map<String, Object> properties, final List<String> handledKeys) {
        final List<Field<?>> fields = new ArrayList<>();

        if (properties != null) {
            properties.forEach((key, value) -> {
                if (handledKeys.contains(key) || key.startsWith("service.imported")
                        || key.startsWith("endpoint.package.version.")) {
                    return;
                }
                final String strValue = value == null ? "" : value.toString();
                fields.add(Field.ofStringType(strValue).editable(false).label(key));
            });
        }

        return fields;
    }
}
