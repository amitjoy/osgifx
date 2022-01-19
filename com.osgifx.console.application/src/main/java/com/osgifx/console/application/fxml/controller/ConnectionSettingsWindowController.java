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

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.command.CommandService;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Maps;
import com.osgifx.console.application.dialog.ConnectionDialog;
import com.osgifx.console.application.dialog.ConnectionSettingDTO;
import com.osgifx.console.application.preference.ConnectionsProvider;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.FxDialog;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class ConnectionSettingsWindowController {

    private static final String CONNECTION_WINDOW_ID         = "com.osgifx.console.window.connection";
    private static final String COMMAND_ID_MANAGE_CONNECTION = "com.osgifx.console.application.command.preference";

    @FXML
    private Button                                     connectButton;
    @FXML
    private Button                                     addConnectionButton;
    @FXML
    private Button                                     removeConnectionButton;
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
    private ThreadSynchronize                          threadSync;
    @Inject
    private IEclipseContext                            context;
    @Inject
    private EModelService                              model;
    @Inject
    private MApplication                               application;
    @Inject
    private IEventBroker                               eventBroker;
    @Inject
    private Supervisor                                 supervisor;
    @Inject
    private CommandService                             commandService;
    @Inject
    private ConnectionsProvider                        connectionsProvider;
    private ProgressDialog                             progressDialog;

    @FXML
    public void initialize() {
        hostColumn.setCellValueFactory(new DTOCellValueFactory<>("host", String.class));
        portColumn.setCellValueFactory(new DTOCellValueFactory<>("port", Integer.class));
        timeoutColumn.setCellValueFactory(new DTOCellValueFactory<>("timeout", Integer.class));

        connectionTable.setItems(connectionsProvider.getConnections());
        connectionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            connectButton.setDisable(newSelection == null);
            removeConnectionButton.setDisable(newSelection == null);
        });
        TableFilter.forTableView(connectionTable).apply();
        logger.atInfo().log("FXML controller has been initialized");
    }

    @FXML
    public void handleClose(final ActionEvent event) {
        logger.atInfo().log("Platform is going to shutdown");
        Platform.exit();
    }

    @FXML
    public void addConnection(final ActionEvent event) {
        logger.atInfo().log("FXML controller 'addConnection(..)' event has been invoked");

        final ConnectionDialog connectionDialog = new ConnectionDialog();
        ContextInjectionFactory.inject(connectionDialog, context);
        logger.atInfo().log("Injected connection dialog to eclipse context");
        connectionDialog.init();

        final Optional<ConnectionSettingDTO> value = connectionDialog.showAndWait();

        if (value.isPresent()) {
            final ConnectionSettingDTO dto = value.get();
            triggerCommand(dto, "ADD");
            logger.atInfo().log("ADD command has been invoked for %s", dto);
        }
    }

    @FXML
    public void removeConnection(final ActionEvent event) {
        logger.atInfo().log("FXML controller 'removeConnection(..)' event has been invoked");
        final ConnectionSettingDTO dto = connectionTable.getSelectionModel().getSelectedItem();
        triggerCommand(dto, "REMOVE");
        logger.atInfo().log("REMOVE command has been invoked for %s", dto);
    }

    @FXML
    public void connectAgent(final ActionEvent event) {
        logger.atInfo().log("FXML controller 'connectAgent(..)' event has been invoked");
        final ConnectionSettingDTO selectedConnection = connectionTable.getSelectionModel().getSelectedItem();
        logger.atInfo().log("Selected connection: %s", selectedConnection);
        final Task<Void> connectTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("Connecting to " + selectedConnection.host + ":" + selectedConnection.port);
                    supervisor.connect(selectedConnection.host, selectedConnection.port, selectedConnection.timeout);
                    logger.atInfo().log("Successfully connected to %s", selectedConnection);
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot connect to %s", selectedConnection);
                    threadSync.asyncExec(() -> {
                        progressDialog.close();
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                logger.atInfo().log("Agent connected event has been sent for %s", selectedConnection);
                eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC, selectedConnection.host + ":" + selectedConnection.port);
            }
        };

        final Thread th = new Thread(connectTask);
        th.setDaemon(true);
        th.start();

        progressDialog = FxDialog.showProgressDialog("Remote Connection", connectTask, getClass().getClassLoader());
    }

    private void triggerCommand(final ConnectionSettingDTO dto, final String type) {
        final Map<String, Object> properties = Maps.newHashMap();

        properties.put("host", dto.host);
        properties.put("port", dto.port);
        properties.put("timeout", dto.timeout);
        properties.put("type", type);

        commandService.execute(COMMAND_ID_MANAGE_CONNECTION, properties);
    }

    @Inject
    @org.eclipse.e4.core.di.annotations.Optional
    private void agentConnected(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent connected event received");
        final MWindow connectionChooserWindow = (MWindow) model.find(CONNECTION_WINDOW_ID, application);
        connectionChooserWindow.setVisible(false);
        progressDialog.close();
    }

}