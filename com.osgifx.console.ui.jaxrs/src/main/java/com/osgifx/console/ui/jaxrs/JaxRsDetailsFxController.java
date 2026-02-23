/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   jaxrs://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.ui.jaxrs;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.osgifx.console.agent.dto.XJaxRsComponentDTO;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class JaxRsDetailsFxController {

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

    void initControls(final XJaxRsComponentDTO jaxrsComponent) {
        if (formRenderer != null) {
            rootPanel.getChildren().remove(formRenderer);
        }
        formRenderer = createForm(jaxrsComponent);
        rootPanel.setCenter(formRenderer);
    }

    private FormRenderer createForm(final XJaxRsComponentDTO jaxrsComponent) {
        final var type = jaxrsComponent.type;

        // @formatter:off
        final var form = Form.of(
                                Section.of(initCommonProperties(jaxrsComponent).toArray(new Field[0]))
                                       .title("Common Properties"),
                                Section.of(initSpecificProperties(jaxrsComponent).toArray(new Field[0]))
                                       .title(type + " Properties"))
                             .title("JAX-RS Configuration Properties");
       // @formatter:on

        final var renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        return renderer;
    }

    private List<Field<?>> initCommonProperties(final XJaxRsComponentDTO jaxrsComponent) {
        // @formatter:off
        final Field<?> nameField      = Field.ofStringType(jaxrsComponent.name)
                                             .editable(false)
                                             .label("Name");
        final Field<?> typeField      = Field.ofStringType(jaxrsComponent.type)
                                             .editable(false)
                                             .label("Type");
        final Field<?> serviceIdField = Field.ofIntegerType(Math.toIntExact(jaxrsComponent.serviceId))
                                             .editable(false)
                                             .label("Service ID");
        final Field<?> statusField    = Field.ofStringType(jaxrsComponent.isFailed ? "Failed" : "Active")
                                             .editable(false)
                                             .label("Status");
        // @formatter:on
        return Arrays.asList(nameField, typeField, serviceIdField, statusField);
    }

    private List<Field<?>> initSpecificProperties(final XJaxRsComponentDTO jaxrsComponent) {
        final var type = jaxrsComponent.type;
        return switch (type) {
            case "Application" -> initApplicationProperties(jaxrsComponent);
            case "Extension" -> initExtensionProperties(jaxrsComponent);
            case "Resource" -> initResourceProperties(jaxrsComponent);
            default -> List.of();
        };
    }

    private List<Field<?>> initApplicationProperties(final XJaxRsComponentDTO app) {
        // @formatter:off
        final Field<?> baseField  = Field.ofStringType(app.base)
                                         .editable(false)
                                         .label("Base");
        // @formatter:on
        return List.of(baseField);
    }

    private List<Field<?>> initExtensionProperties(final XJaxRsComponentDTO ext) {
        // @formatter:off
        final Field<?> extensionTypesField = Field.ofMultiSelectionType(ext.extensionTypes)
                                                  .label("Extension Types");
        final Field<?> consumesField       = Field.ofMultiSelectionType(ext.consumes)
                                                  .label("Consumes");
        final Field<?> producesField       = Field.ofMultiSelectionType(ext.produces)
                                                  .label("Produces");
        // @formatter:on
        return List.of(extensionTypesField, consumesField, producesField);
    }

    private List<Field<?>> initResourceProperties(final XJaxRsComponentDTO resource) {
        // @formatter:off
        final Field<?> consumesField = Field.ofMultiSelectionType(resource.consumes)
                                            .label("Consumes");
        final Field<?> producesField = Field.ofMultiSelectionType(resource.produces)
                                            .label("Produces");
        // @formatter:on
        return List.of(consumesField, producesField);
    }

}
