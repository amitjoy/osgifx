package in.bytehue.osgifx.console.application.fxml.controller;

import static in.bytehue.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.core.command.CommandService;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;

import in.bytehue.osgifx.console.application.dialog.ConnectionDialog;
import in.bytehue.osgifx.console.application.dialog.ConnectionSettingDTO;
import in.bytehue.osgifx.console.application.preference.ConnectionsProvider;
import in.bytehue.osgifx.console.supervisor.Supervisor;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.StageStyle;

public final class ConnectionSettingsWindowController {

    private static final String CONNECTION_WINDOW_ID         = "in.bytehue.osgifx.console.window.connection";
    private static final String COMMAND_ID_MANAGE_CONNECTION = "in.bytehue.osgifx.console.application.command.preference";

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
    private Logger                                     logger;
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
            if (newSelection != null) {
                connectButton.setDisable(false);
                removeConnectionButton.setDisable(false);
            }
        });
        TableFilter.forTableView(connectionTable).apply();
        logger.info("FXML controller (" + getClass() + ") has been initialized");
    }

    @FXML
    public void handleClose(final ActionEvent event) {
        logger.info("Platform is going to shutdown");
        Platform.exit();
    }

    @FXML
    public void addConnection(final ActionEvent event) {
        logger.info("FXML controller (" + getClass() + ") 'addConnection(..)' event has been invoked");
        final ConnectionDialog               connectionDialog = new ConnectionDialog();
        final Optional<ConnectionSettingDTO> value            = connectionDialog.showAndWait();
        if (value.isPresent()) {
            final ConnectionSettingDTO dto = value.get();
            triggerCommand(dto, "ADD");
            logger.info("ADD command has been invoked for " + dto);
        }
    }

    @FXML
    public void removeConnection(final ActionEvent event) {
        logger.info("FXML controller (" + getClass() + ") 'removeConnection(..)' event has been invoked");
        final ConnectionSettingDTO dto = connectionTable.getSelectionModel().getSelectedItem();
        triggerCommand(dto, "REMOVE");
        logger.info("REMOVE command has been invoked for " + dto);
    }

    @FXML
    public void connectAgent(final ActionEvent event) {
        logger.info("FXML controller (" + getClass() + ") 'connectAgent(..)' event has been invoked");
        final ConnectionSettingDTO selectedConnection = connectionTable.getSelectionModel().getSelectedItem();
        logger.info("Selected connection: " + selectedConnection);
        final Task<Void> connectTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateMessage("Connecting to " + selectedConnection.host + ":" + selectedConnection.port);
                    supervisor.connect(selectedConnection.host, selectedConnection.port, selectedConnection.timeout);
                    logger.info("Successfully connected to " + selectedConnection);
                } catch (final Exception e) {
                    logger.error("Cannot connect to " + selectedConnection, e);
                    Platform.runLater(() -> {
                        progressDialog.close();
                        final ExceptionDialog dialog = new ExceptionDialog(e);
                        dialog.initStyle(StageStyle.UNDECORATED);
                        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/default.css").toExternalForm());
                        dialog.show();
                    });
                    throw e;
                }
                return null;
            }

            @Override
            protected void succeeded() {
                logger.debug("Agent connected event has been sent for " + selectedConnection);
                eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC, selectedConnection.host + ":" + selectedConnection.port);
            }
        };

        final Thread th = new Thread(connectTask);
        th.setDaemon(true);
        th.start();

        progressDialog = new ProgressDialog(connectTask);
        progressDialog.setHeaderText("Remote Connection");
        progressDialog.initStyle(StageStyle.UNDECORATED);
        progressDialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/default.css").toExternalForm());
        progressDialog.show();
    }

    private void triggerCommand(final ConnectionSettingDTO dto, final String type) {
        final Map<String, Object> properties = new HashMap<>();

        properties.put("host", dto.host);
        properties.put("port", dto.port);
        properties.put("timeout", dto.timeout);
        properties.put("type", type);

        commandService.execute(COMMAND_ID_MANAGE_CONNECTION, properties);
    }

    @Inject
    @org.eclipse.e4.core.di.annotations.Optional
    private void agentConnected(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        logger.debug("Agent connected event received");
        final MWindow connectionChooserWindow = (MWindow) model.find(CONNECTION_WINDOW_ID, application);
        connectionChooserWindow.setVisible(false);
        progressDialog.close();
    }

}
