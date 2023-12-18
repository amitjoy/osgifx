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
package com.osgifx.console.ui.services;

import java.util.Map.Entry;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.util.fx.Fx;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class ServiceDetailsFxController {

    @Log
    @Inject
    private FluentLogger                               logger;
    @FXML
    private Label                                      idLabel;
    @FXML
    private Label                                      bundleIdLabel;
    @FXML
    private Label                                      bundleBsnLabel;
    @FXML
    private ListView<String>                           objectClassesList;
    @FXML
    private TableView<Entry<String, String>>           propertiesTable;
    @FXML
    private TableColumn<Entry<String, String>, String> propertiesTableColumn1;
    @FXML
    private TableColumn<Entry<String, String>, String> propertiesTableColumn2;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    void initControls(final XServiceDTO service) {
        idLabel.setText(String.valueOf(service.id));
        bundleBsnLabel.setText(service.registeringBundle);
        bundleIdLabel.setText(String.valueOf(service.bundleId));

        propertiesTableColumn1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));
        propertiesTableColumn2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue()));
        propertiesTable.setItems(FXCollections.observableArrayList(service.properties.entrySet()));

        objectClassesList.getItems().clear();
        objectClassesList.getItems().addAll(service.types);

        TableFilter.forTableView(propertiesTable).apply();

        Fx.addContextMenuToCopyContent(propertiesTable);
        Fx.addContextMenuToCopyContent(objectClassesList);
    }

}
