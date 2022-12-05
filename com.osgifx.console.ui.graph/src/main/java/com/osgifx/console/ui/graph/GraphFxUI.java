/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http:www.apache.org/licenses/LICENSE-2.0
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
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.di.FXMLBuilder;
import org.eclipse.fx.ui.di.FXMLBuilder.Data;
import org.eclipse.fx.ui.di.FXMLLoader;
import org.eclipse.fx.ui.di.FXMLLoaderFactory;
import org.osgi.framework.BundleContext;

import com.google.common.base.Enums;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

@SuppressWarnings("deprecation")
public final class GraphFxUI {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private MPart             part;
    @Inject
    @OSGiBundle
    private BundleContext     context;
    @Inject
    private ConsoleStatusBar  statusBar;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    @FXMLLoader
    private FXMLLoaderFactory fxmlLoader;
    @Inject
    private BorderPane        parentNode;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;
    @Inject
    private EPartService      partService;
    @Inject
    private DataProvider      dataProvider;
    @Inject
    private ConsoleMaskerPane progressPane;
    private GraphController   loadedController;

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
        if (loadedController == null) {
            threadSync.asyncExec(() -> FxDialog.showChoiceDialog("Select Graph Generation Type",
                    getClass().getClassLoader(), "/graphic/images/graph.png", strType -> {
                        final Task<Void> task = new Task<>() {
                            @Override
                            protected Void call() throws Exception {
                                final var type = Enums.getIfPresent(GraphController.Type.class, strType.toUpperCase())
                                        .or(GraphController.Type.BUNDLES);
                                loadContent(type);
                                return null;
                            }
                        };
                        CompletableFuture.runAsync(task);
                    }, () -> partService.hidePart(part), "Bundles", "Bundles", "Components"));
        } else {
            loadContent(loadedController.type());
        }
    }

    private void loadContent(final GraphController.Type type) {
        Node tabContent;
        threadSync.asyncExec(() -> progressPane.addTo(parentNode));
        String resource;
        if (type == GraphController.Type.BUNDLES) {
            resource = "/fxml/tab-content-for-bundles.fxml";
        } else {
            resource = "/fxml/tab-content-for-components.fxml";
        }
        final var data = loadFXML(resource);
        if (data == null) {
            logger.atError().log("Graph UI resource '%s' could not be loaded", resource);
            return;
        }
        tabContent       = data.getNode();
        loadedController = data.getController();

        final var content = tabContent; // required for lambda as it needs to be effectively final
        progressPane.setVisible(false);
        threadSync.asyncExec(() -> parentNode.setCenter(content));
    }

    private void initStatusBar() {
        if (isConnected) {
            final var node = Fx.initStatusBarButton(() -> loadedController.updateModel(), "Refresh", "REFRESH");
            statusBar.clearAllInRight();
            if (!isSnapshotAgent) {
                statusBar.addToRight(node);
            }
        } else {
            statusBar.clearAllInRight();
        }
        statusBar.addTo(parentNode);
    }

    private Data<Node, GraphController> loadFXML(final String resourceName) {
        final FXMLBuilder<Node> builder = fxmlLoader.loadBundleRelative(resourceName);
        try {
            return builder.loadWithController();
        } catch (final Exception e) {
            return null;
        }
    }

}
