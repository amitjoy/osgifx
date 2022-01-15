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
package com.osgifx.console.ui.components;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class ComponentsFxController {

    @Log
    @Inject
    private FluentLogger                        logger;
    @Inject
    @LocalInstance
    private FXMLLoader                          loader;
    @FXML
    private TableView<XComponentDTO>            table;
    @Inject
    private DataProvider                        dataProvider;
    @Inject
    @Named("com.osgifx.console.ui.components")
    private BundleContext                       context;
    private TableRowDataFeatures<XComponentDTO> selectedComponent;

    @FXML
    public void initialize() {
        createControls();
        Fx.disableSelectionModel(table);
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void createControls() {
        final GridPane                              expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final ComponentDetailsFxController          controller     = loader.getController();
        final TableRowExpanderColumn<XComponentDTO> expanderColumn = new TableRowExpanderColumn<>(expandedComponent -> {
                                                                       controller.initControls(expandedComponent.getValue());
                                                                       if (selectedComponent != null && selectedComponent.isExpanded()) {
                                                                           selectedComponent.toggleExpanded();
                                                                       }
                                                                       selectedComponent = expandedComponent;
                                                                       return expandedNode;
                                                                   });

        final TableColumn<XComponentDTO, String> componentNameColumn = new TableColumn<>("Name");

        componentNameColumn.setPrefWidth(900);
        componentNameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final TableColumn<XComponentDTO, String> stateColumn = new TableColumn<>("State");

        stateColumn.setPrefWidth(200);
        stateColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(componentNameColumn);
        table.getColumns().add(stateColumn);

        final ObservableList<XComponentDTO> bundles = dataProvider.components();
        table.setItems(bundles);
        Fx.sortBy(table, componentNameColumn);

        TableFilter.forTableView(table).apply();
    }

}
