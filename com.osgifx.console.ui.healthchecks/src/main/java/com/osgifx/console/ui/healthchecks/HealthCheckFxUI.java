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
package com.osgifx.console.ui.healthchecks;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

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

import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public final class HealthCheckFxUI {

	@Log
	@Inject
	private FluentLogger      logger;
	@Inject
	@OSGiBundle
	private BundleContext     context;
	@Inject
	private ConsoleStatusBar  statusBar;
	@Inject
	@Named("is_connected")
	private boolean           isConnected;
	@Inject
	private DataProvider      dataProvider;
	@Inject
	private ConsoleMaskerPane progressPane;

	@PostConstruct
	public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
		createControls(parent, loader);
		logger.atDebug().log("Healthcheck part has been initialized");
	}

	@Focus
	public void onFocus() {
		if (isConnected) {
			refreshData();
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
		initStatusBar(parent);
		CompletableFuture.runAsync(task);
	}

	private void initStatusBar(final BorderPane parent) {
		if (isConnected) {
			final var node = Fx.initStatusBarButton(this::refreshData, "Refresh", "REFRESH");
			statusBar.clearAllInRight();
			statusBar.addToRight(node);
		} else {
			statusBar.clearAllInRight();
		}
		statusBar.addTo(parent);
	}

	private void refreshData() {
		dataProvider.retrieveInfo("healthchecks", true);
	}

}
