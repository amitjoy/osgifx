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
package com.osgifx.console.ui.graph;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public final class GraphFxUI {

	@Log
	@Inject
	private FluentLogger                  logger;
	@Inject
	private MPart                         part;
	@Inject
	@OSGiBundle
	private BundleContext                 context;
	@Inject
	private ConsoleStatusBar              statusBar;
	@Inject
	private ThreadSynchronize             threadSync;
	@Inject
	@LocalInstance
	private FXMLLoader                    fxmlLoader;
	@Inject
	private BorderPane                    parentNode;
	@Inject
	@Named("is_connected")
	private boolean                       isConnected;
	@Inject
	private EPartService                  partService;
	@Inject
	private DataProvider                  dataProvider;
	@Inject
	private ConsoleMaskerPane             progressPane;
	private final AtomicReference<String> loadedType = new AtomicReference<>();

	private static final String BUNDLES_GRAPH_TYPE    = "Bundles";
	private static final String COMPONENTS_GRAPH_TYPE = "Components";

	@PostConstruct
	public void postConstruct() {
		createControls();
		logger.atDebug().log("Graph part has been initialized");
	}

	@Focus
	public void onFocus() {
		if (isConnected) {
			dataProvider.retrieveInfo("bundles", true);
			dataProvider.retrieveInfo("components", true);
		}
	}

	@Inject
	@Optional
	private void updateOnAgentConnectedEvent(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
		logger.atInfo().log("Agent connected event received");
		createControls();
	}

	@Inject
	@Optional
	private void updateOnAgentDisconnectedEvent(@UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data) {
		logger.atInfo().log("Agent disconnected event received");
		createControls();
	}

	private void createControls() {
		initStatusBar();
		if (loadedType.get() == null) {
			threadSync.asyncExec(() -> FxDialog.showChoiceDialog("Select Graph Generation Type", getClass().getClassLoader(),
			        "/graphic/images/graph.png", type -> {
				        final Task<Void> task = new Task<>() {
					        @Override
					        protected Void call() throws Exception {
						        loadContent(type);
						        return null;
					        }
				        };
				        CompletableFuture.runAsync(task);
			        }, () -> partService.hidePart(part), BUNDLES_GRAPH_TYPE, BUNDLES_GRAPH_TYPE, COMPONENTS_GRAPH_TYPE));
		} else {
			loadContent(loadedType.get());
		}
	}

	private void loadContent(final String type) {
		Node tabContent = null;
		threadSync.asyncExec(() -> progressPane.addTo(parentNode));
		if (BUNDLES_GRAPH_TYPE.equalsIgnoreCase(type)) {
			tabContent = Fx.loadFXML(fxmlLoader, context, "/fxml/tab-content-for-bundles.fxml");
			loadedType.set(BUNDLES_GRAPH_TYPE);
		} else {
			tabContent = Fx.loadFXML(fxmlLoader, context, "/fxml/tab-content-for-components.fxml");
			loadedType.set(COMPONENTS_GRAPH_TYPE);
		}
		final var content = tabContent; // required for lambda as it needs to be effectively final
		progressPane.setVisible(false);
		threadSync.asyncExec(() -> parentNode.setCenter(content));
	}

	private void initStatusBar() {
		if (isConnected) {
			final var node = Fx.initStatusBarButton(() -> {
				final var controller = (GraphController) fxmlLoader.getController();
				controller.updateModel();
			}, "Refresh", "REFRESH");
			statusBar.clearAllInRight();
			statusBar.addToRight(node);
		} else {
			statusBar.clearAllInRight();
		}
		statusBar.addTo(parentNode);
	}

}
