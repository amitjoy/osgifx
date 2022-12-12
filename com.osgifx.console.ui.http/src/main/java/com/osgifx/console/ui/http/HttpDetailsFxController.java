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
package com.osgifx.console.ui.http;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.osgifx.console.agent.dto.XHttpComponentDTO;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class HttpDetailsFxController {

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

    void initControls(final XHttpComponentDTO httpComponent) {
        if (formRenderer != null) {
            rootPanel.getChildren().remove(formRenderer);
        }
        formRenderer = createForm(httpComponent);
        rootPanel.setCenter(formRenderer);
    }

    private FormRenderer createForm(final XHttpComponentDTO httpComponent) {
        final var type = httpComponent.type;

        // @formatter:off
        final var form = Form.of(
                                Section.of(initContextProperties(httpComponent).toArray(new Field[0]))
                                       .title("Servlet Context Properties"),
                                Section.of(initSpecificProperties(httpComponent).toArray(new Field[0]))
                                       .title(type + " Properties"))
                             .title("Configuration Properties");
       // @formatter:on

        final var renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        return renderer;
    }

    private List<Field<?>> initContextProperties(final XHttpComponentDTO httpComponent) {
        // @formatter:off
        final Field<?> contextNameField      = Field.ofStringType(httpComponent.contextName)
                                                    .editable(false)
                                                    .label("Name");

        final Field<?> contextPathField      = Field.ofStringType(httpComponent.contextPath)
                                                    .editable(false)
                                                    .label("Path");

        final Field<?> contextServiceIdField = Field.ofIntegerType(Math.toIntExact(httpComponent.contextServiceId))
                                                    .editable(false)
                                                    .label("Service ID");
        // @formatter:on

        return Arrays.asList(contextNameField, contextPathField, contextServiceIdField);
    }

    private List<Field<?>> initSpecificProperties(final XHttpComponentDTO httpComponent) {
        final var type = httpComponent.type;
        return switch (type) {
            case "Servlet" -> initServletProperties(httpComponent);
            case "Filter" -> initFilterProperties(httpComponent);
            case "Resource" -> initResourceProperties(httpComponent);
            case "Listener" -> initListenerProperties(httpComponent);
            case "Error Page" -> initErrorPageProperties(httpComponent);
            default -> List.of();
        };
    }

    private List<Field<?>> initErrorPageProperties(final XHttpComponentDTO errrorPage) {
        // @formatter:off
        final Field<?> nameField           = Field.ofStringType(errrorPage.name)
                                                  .editable(false)
                                                  .label("Name");

        final Field<?> asyncSupportedField = Field.ofBooleanType(errrorPage.asyncSupported)
                                                  .editable(false)
                                                  .label("Async Supported");

        final Field<?> serviceIdField      = Field.ofIntegerType(Math.toIntExact(errrorPage.serviceId))
                                                  .editable(false)
                                                  .label("Service ID");

        final Field<?> infoField           = Field.ofStringType(errrorPage.servletInfo)
                                                  .editable(false)
                                                  .label("Info");

        final Field<?> exceptionsField     = Field.ofMultiSelectionType(errrorPage.exceptions)
                                                  .label("Exceptions");

        final Field<?> errorCodesField     = Field.ofMultiSelectionType(errrorPage.errorCodes)
                                                  .label("ErrorCodes");
        // @formatter:on
        return List.of(nameField, asyncSupportedField, serviceIdField, infoField, exceptionsField, errorCodesField);
    }

    private List<Field<?>> initListenerProperties(final XHttpComponentDTO listener) {
        // @formatter:off
        final Field<?> serviceIdField = Field.ofIntegerType(Math.toIntExact(listener.serviceId))
                                             .editable(false)
                                             .label("Service ID");

        final Field<?> typesField     = Field.ofMultiSelectionType(listener.types)
                                             .label("Types");
        // @formatter:on
        return List.of(serviceIdField, typesField);
    }

    private List<Field<?>> initResourceProperties(final XHttpComponentDTO resource) {
        // @formatter:off
        final Field<?> prefixField    = Field.ofStringType(resource.prefix)
                                             .editable(false)
                                             .label("Prefix");

        final Field<?> serviceIdField = Field.ofIntegerType(Math.toIntExact(resource.serviceId))
                                             .editable(false)
                                             .label("Service ID");

        final Field<?> patternsField  = Field.ofMultiSelectionType(resource.patterns)
                                             .label("Patterns");
        // @formatter:on
        return List.of(prefixField, serviceIdField, patternsField);
    }

    private List<Field<?>> initFilterProperties(final XHttpComponentDTO filter) {
        // @formatter:off
        final Field<?> nameField           = Field.ofStringType(filter.name)
                                                  .editable(false)
                                                  .label("Name");

        final Field<?> asyncSupportedField = Field.ofBooleanType(filter.asyncSupported)
                                                  .editable(false)
                                                  .label("Async Supported");

        final Field<?> serviceIdField      = Field.ofIntegerType(Math.toIntExact(filter.serviceId))
                                                  .editable(false)
                                                  .label("Service ID");

        final Field<?> servletNamesField   = Field.ofMultiSelectionType(filter.servletNames)
                                                  .label("Servlets");

        final Field<?> regexsField         = Field.ofMultiSelectionType(filter.regexs)
                                                  .label("Regular Expressions");

        final Field<?> dispatcherField     = Field.ofMultiSelectionType(filter.dispatcher)
                                                  .label("Dispatcher");

        final Field<?> patternsField       = Field.ofMultiSelectionType(filter.patterns)
                                                  .label("Patterns");
        // @formatter:on
        return List.of(nameField, asyncSupportedField, serviceIdField, patternsField, servletNamesField, regexsField,
                dispatcherField);
    }

    private List<Field<?>> initServletProperties(final XHttpComponentDTO servlet) {
        // @formatter:off
        final Field<?> nameField           = Field.ofStringType(servlet.name)
                                                  .editable(false)
                                                  .label("Name");

        final Field<?> asyncSupportedField = Field.ofBooleanType(servlet.asyncSupported)
                                                  .editable(false)
                                                  .label("Async Supported");

        final Field<?> serviceIdField      = Field.ofIntegerType(Math.toIntExact(servlet.serviceId))
                                                  .editable(false)
                                                  .label("Service ID");

        final Field<?> infoField           = Field.ofStringType(servlet.servletInfo)
                                                  .editable(false)
                                                  .label("Info");

        final Field<?> patternsField       = Field.ofMultiSelectionType(servlet.patterns)
                                                  .label("Patterns");
        // @formatter:on
        return List.of(nameField, asyncSupportedField, serviceIdField, infoField, patternsField);
    }

}
