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
package com.osgifx.console.ui.mcp;

import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static org.osgi.service.condition.Condition.CONDITION_ID;
import static org.osgi.service.condition.Condition.INSTANCE;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.controlsfx.control.table.TableFilter;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;
import org.osgi.service.condition.Condition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.mcp.data.McpDataProvider;
import com.osgifx.console.ui.mcp.dto.McpLogDTO;
import com.osgifx.console.ui.mcp.dto.McpToolDTO;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

public final class McpFxController {

    @Log
    @Inject
    private FluentLogger                    logger;
    @Inject
    @LocalInstance
    private FXMLLoader                      loader;
    @FXML
    private BorderPane                      root;
    @FXML
    private Button                          actionButton;
    @FXML
    private Button                          refreshLogsButton;
    @FXML
    private Button                          clearLogsButton;
    @FXML
    private Label                           statusLabel;
    @FXML
    private TableView<McpToolDTO>           toolsTable;
    @FXML
    private TableColumn<McpToolDTO, String> nameColumn;
    @FXML
    private TableColumn<McpToolDTO, String> descriptionColumn;
    @FXML
    private TableColumn<McpToolDTO, String> schemaColumn;
    @FXML
    private TableView<McpLogDTO>            logsTable;
    @FXML
    private TableColumn<McpLogDTO, String>  timestampColumn;
    @FXML
    private TableColumn<McpLogDTO, String>  typeColumn;
    @FXML
    private TableColumn<McpLogDTO, String>  contentColumn;

    @Inject
    @Optional
    private McpDataProvider   mcpDataProvider;
    @Inject
    @OSGiBundle
    private BundleContext     bundleContext;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    private Executor          executor;

    private static OSGiResult                mcpResult;
    private final ObservableList<McpToolDTO> tools = FXCollections.observableArrayList();
    private final ObservableList<McpLogDTO>  logs  = FXCollections.observableArrayList();
    private final Gson                       gson  = new GsonBuilder().setPrettyPrinting().create();

    @FXML
    public void initialize() {
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(toolsTable);
            Fx.addTablePlaceholderWhenDisconnected(logsTable);
            actionButton.setDisable(true);
            refreshLogsButton.setDisable(true);
            clearLogsButton.setDisable(true);
            statusLabel.setDisable(true);
            return;
        }
        try {
            if (mcpDataProvider == null) {
                actionButton.setDisable(true);
                refreshLogsButton.setDisable(true);
                clearLogsButton.setDisable(true);
                statusLabel.setText("Status: UNAVAILABLE");
                Fx.showErrorNotification("Model Context Protocol", "MCP data provider service is unavailable");
                return;
            }
            initToolsTable();
            initLogsTable();
            updateTools();
            updateLogs();
            updateStatus();
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
            Fx.showErrorNotification("Model Context Protocol", "MCP UI controller initialization failed");
        }
    }

    private void initToolsTable() {
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        descriptionColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
        schemaColumn.setCellValueFactory(cellData -> cellData.getValue().schemaProperty());

        toolsTable.setItems(tools);
        Fx.addContextMenuToCopyContent(toolsTable);
        TableFilter.forTableView(toolsTable).lazy(true).apply();
    }

    private void initLogsTable() {
        timestampColumn.setCellValueFactory(cellData -> Fx.formatTime(cellData.getValue().timestampProperty().get()));
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        contentColumn.setCellValueFactory(cellData -> cellData.getValue().contentProperty());

        logsTable.setItems(logs);
        Fx.addContextMenuToCopyContent(logsTable);
        TableFilter.forTableView(logsTable).lazy(true).apply();
    }

    private void updateTools() {
        if (mcpDataProvider == null) {
            return;
        }
        tools.clear();
        final var registeredTools = mcpDataProvider.tools();
        for (final var tool : registeredTools) {
            final var schema = gson.toJson(tool.inputSchema);
            tools.add(new McpToolDTO(tool.name, tool.description, schema));
        }
    }

    private void updateLogs() {
        if (mcpDataProvider == null) {
            return;
        }
        logs.clear();
        final var serverLogs = mcpDataProvider.logs();
        for (final var log : serverLogs) {
            logs.add(new McpLogDTO(log));
        }
    }

    private void updateStatus() {
        final var isStarted = Boolean.getBoolean("is_started_mcp");
        if (isStarted) {
            statusLabel.setText("Status: RUNNING");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            actionButton.setText("Stop MCP Server");
            actionButton.setOnAction(_ -> stopServer());
            refreshLogsButton.setDisable(false);
            clearLogsButton.setDisable(false);
        } else {
            statusLabel.setText("Status: STOPPED");
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            actionButton.setText("Start MCP Server");
            actionButton.setOnAction(_ -> startServer());
            refreshLogsButton.setDisable(true);
            clearLogsButton.setDisable(true);
        }
    }

    private void startServer() {
        executor.runAsync(() -> {
            mcpResult = OSGi.register(Condition.class, INSTANCE, Map.of(CONDITION_ID, "osgi.fx.mcp"))
                    .run(bundleContext);
            System.setProperty("is_started_mcp", "true");
            threadSync.asyncExec(() -> {
                updateStatus();
                Fx.showSuccessNotification("Model Context Protocol", "MCP server has been started");
            });
        });
    }

    private void stopServer() {
        executor.runAsync(() -> {
            if (mcpResult != null) {
                mcpResult.close();
                mcpResult = null;
            }
            System.setProperty("is_started_mcp", "false");
            threadSync.asyncExec(() -> {
                updateStatus();
                Fx.showSuccessNotification("Model Context Protocol", "MCP server has been stopped");
            });
        });
    }

    @FXML
    private void refreshTools() {
        updateTools();
        Fx.showSuccessNotification("MCP Tools", "Tool list has been refreshed");
    }

    @FXML
    private void refreshLogs() {
        updateLogs();
        Fx.showSuccessNotification("MCP Logs", "Log list has been refreshed");
    }

    @FXML
    private void clearLogs() {
        logs.clear();
        // Ideally clear server logs too if we had an API for that
        Fx.showSuccessNotification("MCP Logs", "Log list has been cleared");
    }

    @Inject
    @Optional
    private void agentDisconnected(@EventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent disconnected event has been received");
        final var isStarted = Boolean.getBoolean("is_started_mcp");
        if (isStarted) {
            stopServer();
        }
    }

}
