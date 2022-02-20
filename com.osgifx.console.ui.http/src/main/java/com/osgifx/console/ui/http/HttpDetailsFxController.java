/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
import com.osgifx.console.agent.dto.XErrorPageDTO;
import com.osgifx.console.agent.dto.XFilterDTO;
import com.osgifx.console.agent.dto.XHttpInfoDTO;
import com.osgifx.console.agent.dto.XListenerDTO;
import com.osgifx.console.agent.dto.XResourceDTO;
import com.osgifx.console.agent.dto.XServletDTO;

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

    void initControls(final XHttpInfoDTO httpContext) {
        if (formRenderer != null) {
            rootPanel.getChildren().remove(formRenderer);
        }
        formRenderer = createForm(httpContext);
        rootPanel.setCenter(formRenderer);
    }

    private FormRenderer createForm(final XHttpInfoDTO httpContext) {
        final String type = httpContext.type;
        final Form   form = Form
                .of(Section.of(initContextProperties(httpContext).toArray(new Field[0])).title("Servlet Context Properties"),
                        Section.of(initSpecificProperties(httpContext).toArray(new Field[0])).title(type + " Properties"))
                .title("Configuration Properties");

        final FormRenderer renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        return renderer;
    }

    private List<Field<?>> initContextProperties(final XHttpInfoDTO httpContext) {

        final Field<?> contextNameField      = Field.ofStringType(httpContext.contextName).editable(false).label("Name");
        final Field<?> contextPathField      = Field.ofStringType(httpContext.contextPath).editable(false).label("Path");
        final Field<?> contextServiceIdField = Field.ofIntegerType(Math.toIntExact(httpContext.contextServiceId)).editable(false)
                .label("Service ID");

        return Arrays.asList(contextNameField, contextPathField, contextServiceIdField);
    }

    private List<Field<?>> initSpecificProperties(final XHttpInfoDTO httpContext) {
        final String type = httpContext.type;
        return switch (type) {
            case "Servlet" -> initServletProperties(httpContext);
            case "Filter" -> initFilterProperties(httpContext);
            case "Resource" -> initResourceProperties(httpContext);
            case "Listener" -> initListenerProperties(httpContext);
            case "Error Page" -> initErrorPageProperties(httpContext);
            default -> List.of();
        };
    }

    private List<Field<?>> initErrorPageProperties(final XHttpInfoDTO httpContext) {
        final XErrorPageDTO errrorPage = (XErrorPageDTO) httpContext;

        final Field<?> nameField           = Field.ofStringType(errrorPage.name).editable(false).label("Name");
        final Field<?> asyncSupportedField = Field.ofBooleanType(errrorPage.asyncSupported).editable(false).label("Async Supported");
        final Field<?> serviceIdField      = Field.ofIntegerType(Math.toIntExact(errrorPage.serviceId)).editable(false).label("Service ID");
        final Field<?> infoField           = Field.ofStringType(errrorPage.servletInfo).editable(false).label("Info");
        final Field<?> exceptionsField     = Field.ofMultiSelectionType(errrorPage.exceptions).label("Exceptions");
        final Field<?> errorCodesField     = Field.ofMultiSelectionType(errrorPage.errorCodes).label("ErrorCodes");

        return Arrays.asList(nameField, asyncSupportedField, serviceIdField, infoField, exceptionsField, errorCodesField);
    }

    private List<Field<?>> initListenerProperties(final XHttpInfoDTO httpContext) {
        final XListenerDTO listener = (XListenerDTO) httpContext;

        final Field<?> serviceIdField = Field.ofIntegerType(Math.toIntExact(listener.serviceId)).editable(false).label("Service ID");
        final Field<?> typesField     = Field.ofMultiSelectionType(listener.types).label("Types");

        return Arrays.asList(serviceIdField, typesField);
    }

    private List<Field<?>> initResourceProperties(final XHttpInfoDTO httpContext) {
        final XResourceDTO resource = (XResourceDTO) httpContext;

        final Field<?> prefixField    = Field.ofStringType(resource.prefix).editable(false).label("Prefix");
        final Field<?> serviceIdField = Field.ofIntegerType(Math.toIntExact(resource.serviceId)).editable(false).label("Service ID");
        final Field<?> patternsField  = Field.ofMultiSelectionType(resource.patterns).label("Patterns");

        return Arrays.asList(prefixField, serviceIdField, patternsField);
    }

    private List<Field<?>> initFilterProperties(final XHttpInfoDTO httpContext) {
        final XFilterDTO filter = (XFilterDTO) httpContext;

        final Field<?> nameField           = Field.ofStringType(filter.name).editable(false).label("Name");
        final Field<?> asyncSupportedField = Field.ofBooleanType(filter.asyncSupported).editable(false).label("Async Supported");
        final Field<?> serviceIdField      = Field.ofIntegerType(Math.toIntExact(filter.serviceId)).editable(false).label("Service ID");
        final Field<?> servletNamesField   = Field.ofMultiSelectionType(filter.servletNames).label("Servlets");
        final Field<?> regexsField         = Field.ofMultiSelectionType(filter.regexs).label("Regular Expressions");
        final Field<?> dispatcherField     = Field.ofMultiSelectionType(filter.dispatcher).label("Dispatcher");
        final Field<?> patternsField       = Field.ofMultiSelectionType(filter.patterns).label("Patterns");

        return Arrays.asList(nameField, asyncSupportedField, serviceIdField, patternsField, servletNamesField, regexsField,
                dispatcherField);
    }

    private List<Field<?>> initServletProperties(final XHttpInfoDTO httpContext) {
        final XServletDTO servlet = (XServletDTO) httpContext;

        final Field<?> nameField           = Field.ofStringType(servlet.name).editable(false).label("Name");
        final Field<?> asyncSupportedField = Field.ofBooleanType(servlet.asyncSupported).editable(false).label("Async Supported");
        final Field<?> serviceIdField      = Field.ofIntegerType(Math.toIntExact(servlet.serviceId)).editable(false).label("Service ID");
        final Field<?> infoField           = Field.ofStringType(servlet.servletInfo).editable(false).label("Info");
        final Field<?> patternsField       = Field.ofMultiSelectionType(servlet.patterns).label("Patterns");

        return Arrays.asList(nameField, asyncSupportedField, serviceIdField, infoField, patternsField);
    }

}
