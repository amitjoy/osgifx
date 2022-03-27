/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.command.CommandService;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Maps;
import com.osgifx.console.application.dialog.ConnectToAgentDialog;
import com.osgifx.console.application.dialog.ConnectToAgentDialog.ActionType;
import com.osgifx.console.application.dialog.ConnectionDialog;
import com.osgifx.console.application.dialog.ConnectionSettingDTO;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class ConnectToAgentHandler {

	private static final String COMMAND_ID_MANAGE_CONNECTION = "com.osgifx.console.application.command.preference";

	@Log
	@Inject
	private FluentLogger               logger;
	@Inject
	private ThreadSynchronize          threadSync;
	@Inject
	private IEclipseContext            context;
	@Inject
	private IEventBroker               eventBroker;
	@Inject
	private Supervisor                 supervisor;
	@Inject
	private CommandService             commandService;
	@Inject
	@ContextValue("is_connected")
	private ContextBoundValue<Boolean> isConnected;
	@Inject
	@ContextValue("connected.agent")
	private ContextBoundValue<String>  connectedAgent;
	@Inject
	@Named("selected.settings")
	private ConnectionSettingDTO       selectedSettings;
	private ProgressDialog             progressDialog;

	@Execute
	public void execute() {
		final var connectToAgentDialog = new ConnectToAgentDialog();
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
			return;
		}
		if (selectedButton == connectToAgentDialog.getButtonType(ActionType.REMOVE_CONNECTION)) {
			removeConnection();
			return;
		}
		if (selectedButton == connectToAgentDialog.getButtonType(ActionType.CONNECT)) {
			connectAgent();
		}
	}

	@CanExecute
	public boolean canExecute() {
		return !isConnected.getValue();
	}

	private void addConnection() {
		logger.atInfo().log("'%s'-'addConnection(..)' event has been invoked", getClass().getSimpleName());

		final var connectionDialog = new ConnectionDialog();
		ContextInjectionFactory.inject(connectionDialog, context);
		logger.atInfo().log("Injected connection dialog to eclipse context");
		connectionDialog.init();

		final var value = connectionDialog.showAndWait();

		if (value.isPresent()) {
			final var dto = value.get();
			triggerCommand(dto, "ADD");
			logger.atInfo().log("ADD command has been invoked for %s", dto);
			Fx.showSuccessNotification("Connection Settings", "New connection settings has been added successfully");
		}
	}

	private void removeConnection() {
		logger.atInfo().log("'%s'-'removeConnection(..)' event has been invoked", getClass().getSimpleName());
		if (selectedSettings == null) {
			logger.atInfo().log("No connection setting has been selected");
			return;
		}
		triggerCommand(selectedSettings, "REMOVE");
		logger.atInfo().log("REMOVE command has been invoked for %s", selectedSettings);
		Fx.showSuccessNotification("Connection Settings", "Connection settings has been removed successfully");
	}

	private void connectAgent() {
		logger.atInfo().log("'%s'-'connectAgent(..)' event has been invoked", getClass().getSimpleName());
		if (selectedSettings == null) {
			logger.atInfo().log("No connection setting has been selected");
			return;
		}
		logger.atInfo().log("Selected connection: %s", selectedSettings);
		final Task<Void> connectTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				try {
					updateMessage("Connecting to " + selectedSettings.host + ":" + selectedSettings.port);
					supervisor.connect(selectedSettings.host, selectedSettings.port, selectedSettings.timeout);
					logger.atInfo().log("Successfully connected to %s", selectedSettings);
				} catch (final Exception e) {
					logger.atError().withException(e).log("Cannot connect to %s", selectedSettings);
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
				logger.atInfo().log("Agent connected event has been sent for %s", selectedSettings);
				final var connection = selectedSettings.host + ":" + selectedSettings.port;

				eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC, connection);
				connectedAgent.publish(connection);
				isConnected.publish(true);
			}
		};

		final var th = new Thread(connectTask);
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
		if (progressDialog != null) {
			progressDialog.close();
		}
	}

}
