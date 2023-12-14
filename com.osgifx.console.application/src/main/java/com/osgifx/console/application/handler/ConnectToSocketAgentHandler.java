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
package com.osgifx.console.application.handler;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.REMOTE_RPC;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.SNAPSHOT;
import static javafx.scene.control.ButtonType.CANCEL;

import java.util.Map;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.command.CommandService;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Maps;
import com.osgifx.console.application.dialog.ConnectToSocketAgentDialog;
import com.osgifx.console.application.dialog.ConnectToSocketAgentDialog.ActionType;
import com.osgifx.console.application.dialog.SocketConnectionDialog;
import com.osgifx.console.application.dialog.SocketConnectionSettingDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.SocketConnection;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.supervisor.factory.SupervisorFactory;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import jakarta.inject.Inject;
import javafx.concurrent.Task;

public final class ConnectToSocketAgentHandler {

    private static final String COMMAND_ID_MANAGE_CONNECTION = "com.osgifx.console.application.command.socket.connection.preference";

    @Log
    @Inject
    private FluentLogger                                  logger;
    @Inject
    private Executor                                      executor;
    @Inject
    private ThreadSynchronize                             threadSync;
    @Inject
    private IEclipseContext                               context;
    @Inject
    private IEventBroker                                  eventBroker;
    @Inject
    @Optional
    private Supervisor                                    supervisor;
    @Inject
    private CommandService                                commandService;
    @Inject
    @Optional
    @ContextValue("is_connected")
    private ContextBoundValue<Boolean>                    isConnected;
    @Inject
    @Optional
    @ContextValue("is_local_agent")
    private ContextBoundValue<Boolean>                    isLocalAgent;
    @Inject
    @Optional
    @ContextValue("is_snapshot_agent")
    private ContextBoundValue<Boolean>                    isSnapshotAgent;
    @Inject
    @Optional
    @ContextValue("connected.agent")
    private ContextBoundValue<String>                     connectedAgent;
    @Inject
    @ContextValue("selected.settings")
    private ContextBoundValue<SocketConnectionSettingDTO> selectedSettings;
    @Inject
    private SupervisorFactory                             supervisorFactory;
    private ProgressDialog                                progressDialog;

    @Execute
    public void execute() {
        final var connectToAgentDialog = new ConnectToSocketAgentDialog();
        ContextInjectionFactory.inject(connectToAgentDialog, context);
        logger.atInfo().log("Injected connect to agent dialog to eclipse context");

        connectToAgentDialog.init();

        final var result = connectToAgentDialog.showAndWait();
        if (!result.isPresent()) {
            logger.atInfo().log("No button has been selected");
            return;
        }
        final var selectedButton = result.get();
        if (selectedButton == connectToAgentDialog.getButtonType(ActionType.ADD_CONNECTION)) {
            addConnection();
            removeCurrentSelection();
            return;
        }
        if (selectedButton == connectToAgentDialog.getButtonType(ActionType.EDIT_CONNECTION)) {
            editConnection(selectedSettings.getValue());
            return;
        }
        if (selectedButton == connectToAgentDialog.getButtonType(ActionType.REMOVE_CONNECTION)) {
            removeConnection();
            removeCurrentSelection();
            return;
        }
        if (selectedButton == connectToAgentDialog.getButtonType(ActionType.CONNECT)) {
            connectAgent();
            removeCurrentSelection();
            return;
        }
        if (selectedButton == CANCEL) {
            removeCurrentSelection();
        }
    }

    @CanExecute
    public boolean canExecute() {
        return !isConnected.getValue();
    }

    private void addConnection() {
        logger.atInfo().log("'%s'-'addConnection(..)' event has been invoked", getClass().getSimpleName());

        final var connectionDialog = new SocketConnectionDialog();
        ContextInjectionFactory.inject(connectionDialog, context);
        logger.atInfo().log("Injected connection dialog to eclipse context");
        connectionDialog.init(null);

        final var value = connectionDialog.showAndWait();

        if (value.isPresent()) {
            final var dto = value.get();
            triggerCommand(dto, "ADD");
            logger.atInfo().log("ADD command has been invoked for %s", dto);
            Fx.showSuccessNotification("Connection Settings", "New connection settings has been added successfully");
        }
    }

    private void editConnection(final SocketConnectionSettingDTO setting) {
        logger.atInfo().log("'%s'-'editConnection(..)' event has been invoked", getClass().getSimpleName());

        final var connectionDialog = new SocketConnectionDialog();
        ContextInjectionFactory.inject(connectionDialog, context);
        logger.atInfo().log("Injected connection dialog to eclipse context");
        connectionDialog.init(setting);

        final var value = connectionDialog.showAndWait();

        if (value.isPresent()) {
            final var dto = value.get();
            triggerCommand(dto, "EDIT");
            logger.atInfo().log("EDIT command has been invoked for %s", dto);
            Fx.showSuccessNotification("Connection Settings", "Connection settings has been updated successfully");
        }
    }

    private void removeConnection() {
        logger.atInfo().log("'%s'-'removeConnection(..)' event has been invoked", getClass().getSimpleName());
        final SocketConnectionSettingDTO settings = selectedSettings.getValue();
        if (settings == null) {
            logger.atInfo().log("No connection setting has been selected");
            return;
        }
        triggerCommand(settings, "REMOVE");
        logger.atInfo().log("REMOVE command has been invoked for %s", settings);
        Fx.showSuccessNotification("Connection Settings", "Connection settings has been removed successfully");
    }

    private void connectAgent() {
        logger.atInfo().log("'%s'-'connectAgent(..)' event has been invoked", getClass().getSimpleName());
        final var settings = selectedSettings.getValue();
        if (settings == null) {
            logger.atInfo().log("No connection setting has been selected");
            return;
        }
        logger.atInfo().log("Selected connection: %s", selectedSettings);
        final Task<Void> connectTask = new Task<>() {

            @Override
            protected Void call() throws Exception {
                try {
                    supervisorFactory.removeSupervisor(SNAPSHOT);
                    supervisorFactory.createSupervisor(REMOTE_RPC);
                    updateMessage("Connecting to " + settings.host + ":" + settings.port);

                    // @formatter:off
                    final var socketConnection = SocketConnection
                            .builder()
                            .host(settings.host)
                            .port(settings.port)
                            .timeout(settings.timeout)
                            .truststore(settings.trustStorePath)
                            .truststorePass(settings.trustStorePassword)
                            .build();
                    // @formatter:on

                    supervisor.connect(socketConnection);
                    logger.atInfo().log("Successfully connected to %s", settings);
                    return null;
                } catch (final InterruptedException e) {
                    logger.atInfo().log("Connection task interrupted");
                    threadSync.asyncExec(progressDialog::close);
                    supervisorFactory.removeSupervisor(REMOTE_RPC);
                    throw e;
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot connect to %s", settings);
                    threadSync.asyncExec(() -> {
                        progressDialog.close();
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    supervisorFactory.removeSupervisor(REMOTE_RPC);
                    throw e;
                }
            }

            @Override
            protected void succeeded() {
                logger.atInfo().log("Agent connected event has been sent for %s", settings);
                final var connection = "[SOCKET] " + settings.host + ":" + settings.port;

                eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC, connection);
                connectedAgent.publish(connection);
                isConnected.publish(true);
                isLocalAgent.publish(false);
                isSnapshotAgent.publish(false);
            }
        };

        final var taskFuture = executor.runAsync(connectTask);
        progressDialog = FxDialog.showProgressDialog("Remote Connection", connectTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    private void triggerCommand(final SocketConnectionSettingDTO dto, final String type) {
        final Map<String, Object> properties = Maps.newHashMap();

        properties.put("id", dto.id);
        properties.put("name", dto.name);
        properties.put("host", dto.host);
        properties.put("port", dto.port);
        properties.put("timeout", dto.timeout);
        properties.put("type", type);
        properties.put("truststore", dto.trustStorePath);
        properties.put("truststorePassword", dto.trustStorePassword);

        commandService.execute(COMMAND_ID_MANAGE_CONNECTION, properties);
    }

    private void removeCurrentSelection() {
        selectedSettings.publish(null);
        logger.atDebug().log("Current selection has been removed");
    }

    @Inject
    @Optional
    private void agentConnected(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent connected event received");
        if (progressDialog != null) {
            progressDialog.close();
        }
    }

}
