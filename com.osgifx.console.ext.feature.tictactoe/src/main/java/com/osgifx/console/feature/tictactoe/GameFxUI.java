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
package com.osgifx.console.feature.tictactoe;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.agent.ExtensionHelper;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public final class GameFxUI {

	@Log
	@Inject
	private FluentLogger      logger;
	@Inject
	@OSGiBundle
	private BundleContext     context;
	@Inject
	private ConsoleStatusBar  statusBar;
	@Inject
	private Supervisor        supervisor;
	@Inject
	private ConsoleMaskerPane progressPane;
	@Inject
	@Named("is_connected")
	private boolean           isConnected;

	@PostConstruct
	public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
		createControls(parent, loader);
		logger.atDebug().log("Tic-Tac-Toe game play part has been initialized");
	}

	@Focus
	public void onFocus() {
		if (isConnected) {
			final var agent = supervisor.getAgent();
			final var name  = "my-agent-extension";

			final Map<String, Object> context1 = Map.of("propValue", 500);
			final var                 result1  = agent.executeExtension(name, context1);
			System.out.println(result1);

			final var context2 = new MyContextDTO();
			context2.propValue = 500;

			final var result2 = ExtensionHelper.executeExtension(agent, name, context2, MyResultDTO.class);
			System.out.println(result2);
		}
	}

	@Inject
	@Optional
	private void updateOnAgentConnectedEvent( //
	        @UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data, //
	        final BorderPane parent, //
	        @LocalInstance final FXMLLoader loader) {
		logger.atInfo().log("Agent connected event received");
		createControls(parent, loader);
	}

	@Inject
	@Optional
	private void updateOnAgentDisconnectedEvent( //
	        @UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data, //
	        final BorderPane parent, //
	        @LocalInstance final FXMLLoader loader) {
		logger.atInfo().log("Agent disconnected event received");
		createControls(parent, loader);
	}

	private void createControls(final BorderPane parent, final FXMLLoader loader) {
		progressPane.setVisible(true);
		final Task<Void> task = new Task<>() {

			Node tabContent;

			@Override
			protected Void call() throws Exception {
				tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content.fxml");
				return null;
			}

			@Override
			protected void succeeded() {
				parent.getChildren().clear();
				parent.setCenter(tabContent);
				statusBar.addTo(parent);
				progressPane.setVisible(false);
			}
		};
		parent.getChildren().clear();
		progressPane.addTo(parent);
		statusBar.addTo(parent);

		CompletableFuture.runAsync(task);
	}

}
