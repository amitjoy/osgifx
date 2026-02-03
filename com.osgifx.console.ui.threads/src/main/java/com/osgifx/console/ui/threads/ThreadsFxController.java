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
package com.osgifx.console.ui.threads;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

public final class ThreadsFxController {

    @Log
    @Inject
    private FluentLogger                    logger;
    @FXML
    private TableView<XThreadDTO>           table;
    @FXML
    private TableColumn<XThreadDTO, String> nameColumn;
    @FXML
    private TableColumn<XThreadDTO, String> idColumn;
    @FXML
    private TableColumn<XThreadDTO, String> priorityColumn;
    @FXML
    private TableColumn<XThreadDTO, String> stateColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isInterruptedColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isAliveColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isDaemonColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isDeadlockedColumn;
    @Inject
    @Named("is_connected")
    private boolean                         isConnected;
    @Inject
    private DataProvider                    dataProvider;

    @FXML
    public void initialize() {
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            return;
        }
        try {
            initCells();
            Fx.addContextMenuToCopyContent(table);
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    private void initCells() {
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        Fx.addCellFactory(nameColumn, b -> b.isDeadlocked, Color.RED, Color.BLACK);

        idColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        priorityColumn.setCellValueFactory(new DTOCellValueFactory<>("priority", String.class));
        stateColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));
        isInterruptedColumn.setCellValueFactory(new DTOCellValueFactory<>("isInterrupted", String.class));
        isAliveColumn.setCellValueFactory(new DTOCellValueFactory<>("isAlive", String.class));
        isDaemonColumn.setCellValueFactory(new DTOCellValueFactory<>("isDaemon", String.class));

        isDeadlockedColumn.setCellValueFactory(new DTOCellValueFactory<>("isDeadlocked", String.class));
        Fx.addCellFactory(isDeadlockedColumn, b -> b.isDeadlocked, Color.RED, Color.BLACK);

        table.setItems(dataProvider.threads());
        TableFilter.forTableView(table).lazy(true).apply();
    }

}
