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
package com.osgifx.console.application.fxml.controller;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.application.dialog.ConnectionSettingDTO;
import com.osgifx.console.application.preference.ConnectionsProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;

import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class ConnectionSettingsDialogController {

    @FXML
    private TableView<ConnectionSettingDTO>            connectionTable;
    @FXML
    private TableColumn<ConnectionSettingDTO, String>  hostColumn;
    @FXML
    private TableColumn<ConnectionSettingDTO, Integer> portColumn;
    @FXML
    private TableColumn<ConnectionSettingDTO, Integer> timeoutColumn;
    @Log
    @Inject
    private FluentLogger                               logger;
    @Inject
    private ConnectionsProvider                        connectionsProvider;
    @Inject
    @ContextValue("selected.settings")
    private Property<ConnectionSettingDTO>             selectedSettings;

    @FXML
    public void initialize() {
        hostColumn.setCellValueFactory(new DTOCellValueFactory<>("host", String.class));
        portColumn.setCellValueFactory(new DTOCellValueFactory<>("port", Integer.class));
        timeoutColumn.setCellValueFactory(new DTOCellValueFactory<>("timeout", Integer.class));

        connectionTable.setItems(connectionsProvider.getConnections());
        selectedSettings.bind(connectionTable.getSelectionModel().selectedItemProperty());
        TableFilter.forTableView(connectionTable).apply();
        logger.atInfo().log("FXML controller has been initialized");
    }

}
