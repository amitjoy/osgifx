/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
package com.osgifx.console.ui.roles;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

public final class RolesFxController {

    @Log
    @Inject
    private FluentLogger                   logger;
    @Inject
    @LocalInstance
    private FXMLLoader                     loader;
    @FXML
    private TableView<XRoleDTO>            table;
    @Inject
    @OSGiBundle
    private BundleContext                  context;
    @Inject
    @Named("is_connected")
    private boolean                        isConnected;
    @Inject
    private DataProvider                   dataProvider;
    private TableRowDataFeatures<XRoleDTO> previouslyExpanded;

    @FXML
    public void initialize() {
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            return;
        }
        try {
            createControls();
            Fx.disableSelectionModel(table);
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    private void createControls() {
        final var expandedNode   = (BorderPane) Fx.loadFXML(loader, context, "/fxml/expander-column-content.fxml");
        final var controller     = (RoleEditorFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XRoleDTO>(current -> {
                                     if (previouslyExpanded != null
                                             && current.getValue() == previouslyExpanded.getValue()) {
                                         return expandedNode;
                                     }
                                     if (previouslyExpanded != null && previouslyExpanded.isExpanded()) {
                                         previouslyExpanded.toggleExpanded();
                                     }
                                     controller.initControls(current.getValue());
                                     previouslyExpanded = current;
                                     return expandedNode;
                                 });

        final var roleNameColumn = new TableColumn<XRoleDTO, String>("Name");
        roleNameColumn.setPrefWidth(580);
        roleNameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final var roleTypeColumn = new TableColumn<XRoleDTO, String>("Type");
        roleTypeColumn.setPrefWidth(400);
        roleTypeColumn.setCellValueFactory(new DTOCellValueFactory<>("type", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(roleNameColumn);
        table.getColumns().add(roleTypeColumn);

        table.setItems(dataProvider.roles());
        TableFilter.forTableView(table).lazy(true).apply();
    }

}
