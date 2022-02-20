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

import static com.osgifx.console.util.fx.ConsoleFxHelper.makeNullSafe;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XHttpContextInfoDTO;
import com.osgifx.console.agent.dto.XHttpInfoDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class HttpFxController {

    @Log
    @Inject
    private FluentLogger            logger;
    @Inject
    @LocalInstance
    private FXMLLoader              loader;
    @FXML
    private TableView<XHttpInfoDTO> table;
    @Inject
    @OSGiBundle
    private BundleContext           context;
    @Inject
    @Named("is_connected")
    private boolean                 isConnected;
    @Inject
    private DataProvider            dataProvider;

    @FXML
    public void initialize() {
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            return;
        }
        createControls();
        Fx.disableSelectionModel(table);
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void createControls() {
        final BorderPane                           expandedNode   = (BorderPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final HttpDetailsFxController              controller     = loader.getController();
        final TableRowExpanderColumn<XHttpInfoDTO> expanderColumn = new TableRowExpanderColumn<>(expandedContext -> {
                                                                      controller.initControls(expandedContext.getValue());
                                                                      return expandedNode;
                                                                  });

        final TableColumn<XHttpInfoDTO, String> componentColumn = new TableColumn<>("Component Name");
        componentColumn.setPrefWidth(550);
        componentColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class, s -> {
            // resource doesn't have associated name field
            try {
                s.getClass().getField("name");
            } catch (final Exception e) {
                return "No name associated";
            }
            return null; // not gonna happen as the value
        }));

        final TableColumn<XHttpInfoDTO, String> contextNameColumn = new TableColumn<>("Context Name");
        contextNameColumn.setPrefWidth(150);
        contextNameColumn.setCellValueFactory(new DTOCellValueFactory<>("contextName", String.class));

        final TableColumn<XHttpInfoDTO, String> contextPathColumn = new TableColumn<>("Context Path");
        contextPathColumn.setPrefWidth(200);
        contextPathColumn.setCellValueFactory(new DTOCellValueFactory<>("contextPath", String.class));

        final TableColumn<XHttpInfoDTO, String> contextServiceIdColumn = new TableColumn<>("Context Service ID");
        contextServiceIdColumn.setPrefWidth(140);
        contextServiceIdColumn.setCellValueFactory(new DTOCellValueFactory<>("contextServiceId", String.class));

        final TableColumn<XHttpInfoDTO, String> componentTypeColumn = new TableColumn<>("Type");
        componentTypeColumn.setPrefWidth(100);
        componentTypeColumn.setCellValueFactory(new DTOCellValueFactory<>("type", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(componentColumn);
        table.getColumns().add(contextNameColumn);
        table.getColumns().add(contextPathColumn);
        table.getColumns().add(contextServiceIdColumn);
        table.getColumns().add(componentTypeColumn);

        final ObservableList<XHttpInfoDTO> httpComponents = FXCollections.observableArrayList();
        final XHttpContextInfoDTO          httpContext    = dataProvider.httpContext();
        if (httpContext != null) {
            httpComponents.addAll(makeNullSafe(httpContext.servlets));
            httpComponents.addAll(makeNullSafe(httpContext.filters));
            httpComponents.addAll(makeNullSafe(httpContext.listeners));
            httpComponents.addAll(makeNullSafe(httpContext.resources));
            httpComponents.addAll(makeNullSafe(httpContext.errorPages));
        }

        final ObservableList<XHttpInfoDTO> httpRuntime = httpComponents;
        table.setItems(httpRuntime);
        Fx.sortBy(table, componentColumn);

        TableFilter.forTableView(table).apply();
    }

}
