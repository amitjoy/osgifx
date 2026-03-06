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
package com.osgifx.console.ui.cdi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.osgifx.console.agent.dto.XCdiComponentDTO;
import com.osgifx.console.agent.dto.XCdiContainerDTO;
import com.osgifx.console.agent.dto.XCdiExtensionDTO;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class CdiDetailsFxController {

    @Log
    @Inject
    private FluentLogger                          logger;
    @FXML
    private TabPane                               rootPane;
    @FXML
    private BorderPane                            infoPane;
    @FXML
    private TableView<XCdiComponentDTO>           componentsTable;
    @FXML
    private TableColumn<XCdiComponentDTO, String> componentNameColumn;
    @FXML
    private TableColumn<XCdiComponentDTO, String> componentTypeColumn;
    @FXML
    private TableColumn<XCdiComponentDTO, String> componentEnabledColumn;
    @FXML
    private TableView<XCdiExtensionDTO>           extensionsTable;
    @FXML
    private TableColumn<XCdiExtensionDTO, String> extensionServiceIdColumn;
    @FXML
    private TableColumn<XCdiExtensionDTO, String> extensionFilterColumn;
    @FXML
    private ListView<String>                      errorsList;

    private FormRenderer formRenderer;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    void initControls(final XCdiContainerDTO container) {
        if (formRenderer != null) {
            infoPane.getChildren().remove(formRenderer);
        }
        formRenderer = createForm(container);
        infoPane.setCenter(formRenderer);

        componentNameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        componentTypeColumn.setCellValueFactory(new DTOCellValueFactory<>("type", String.class));
        componentEnabledColumn.setCellValueFactory(
                new DTOCellValueFactory<>("enabled", String.class, s -> String.valueOf(s.enabled)));
        componentsTable.setItems(FXCollections
                .observableArrayList(container.components == null ? Collections.emptyList() : container.components));

        extensionServiceIdColumn.setCellValueFactory(
                new DTOCellValueFactory<>("serviceId", String.class, s -> String.valueOf(s.serviceId)));
        extensionFilterColumn.setCellValueFactory(new DTOCellValueFactory<>("serviceFilter", String.class));
        extensionsTable.setItems(FXCollections
                .observableArrayList(container.extensions == null ? Collections.emptyList() : container.extensions));

        errorsList.setItems(FXCollections
                .observableArrayList(container.errors == null ? Collections.emptyList() : container.errors));
    }

    private FormRenderer createForm(final XCdiContainerDTO container) {
        // @formatter:off
        final var form = Form.of(
                                Section.of(initProperties(container).toArray(new Field[0]))
                                       .title("CDI Container Details"))
                             .title("CDI Configuration Properties");
       // @formatter:on

        final var renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        Fx.addCopySupportToReadOnlyLabels(renderer);
        return renderer;
    }

    private List<Field<?>> initProperties(final XCdiContainerDTO container) {
        final var numComponents = container.components == null ? 0 : container.components.size();
        final var numExtensions = container.extensions == null ? 0 : container.extensions.size();
        final var numErrors     = container.errors == null ? 0 : container.errors.size();

        // @formatter:off
        final Field<?> idField           = Field.ofStringType(container.id)
                                                .editable(false)
                                                .label("ID");
        final Field<?> bundleField       = Field.ofStringType(String.valueOf(container.bundleId))
                                                .editable(false)
                                                .label("Bundle ID");
        final Field<?> componentsField   = Field.ofIntegerType(numComponents)
                                                .editable(false)
                                                .label("Component Count");
        final Field<?> extensionsField   = Field.ofIntegerType(numExtensions)
                                                .editable(false)
                                                .label("Extension Count");
        final Field<?> errorsField       = Field.ofIntegerType(numErrors)
                                                .editable(false)
                                                .label("Error Count");
        // @formatter:on
        return Arrays.asList(idField, bundleField, componentsField, extensionsField, errorsField);
    }

}
