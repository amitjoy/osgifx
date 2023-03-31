/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.application.dialog.MqttConnectionSettingDTO;
import com.osgifx.console.application.dialog.SocketConnectionSettingDTO;
import com.osgifx.console.application.preference.ConnectionsProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class MqttConnectionSettingsDialogController {

    @FXML
    private TableView<MqttConnectionSettingDTO>              connectionTable;
    @FXML
    private TableColumn<MqttConnectionSettingDTO, String>    nameColumn;
    @FXML
    private TableColumn<MqttConnectionSettingDTO, String>    serverColumn;
    @FXML
    private TableColumn<MqttConnectionSettingDTO, Integer>   portColumn;
    @FXML
    private TableColumn<SocketConnectionSettingDTO, Integer> timeoutColumn;
    @FXML
    private TableColumn<MqttConnectionSettingDTO, String>    usernameColumn;
    @Log
    @Inject
    private FluentLogger                                     logger;
    @Inject
    private ConnectionsProvider                              connectionsProvider;
    @Inject
    @ContextValue("selected.settings")
    private ContextBoundValue<MqttConnectionSettingDTO>      selectedSettings;

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        serverColumn.setCellValueFactory(new DTOCellValueFactory<>("server", String.class));
        portColumn.setCellValueFactory(new DTOCellValueFactory<>("port", Integer.class));
        timeoutColumn.setCellValueFactory(new DTOCellValueFactory<>("timeout", Integer.class));
        usernameColumn.setCellValueFactory(new DTOCellValueFactory<>("username", String.class));

        connectionTable.setItems(connectionsProvider.getMqttConnections());
        connectionTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSettings, newSettings) -> selectedSettings.publish(newSettings));

        TableFilter.forTableView(connectionTable).apply();
        logger.atDebug().log("FXML controller has been initialized");
    }

    public ReadOnlyObjectProperty<MqttConnectionSettingDTO> selectedSettings() {
        return connectionTable.getSelectionModel().selectedItemProperty();
    }

}
