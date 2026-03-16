/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
import static org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.SegmentedButton;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.di.FXMLBuilder;
import org.eclipse.fx.ui.di.FXMLBuilder.Data;
import org.eclipse.fx.ui.di.FXMLLoader;
import org.eclipse.fx.ui.di.FXMLLoaderFactory;
import org.osgi.framework.BundleContext;

import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

@SuppressWarnings("deprecation")
public final class GraphFxUI {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    @OSGiBundle
    private BundleContext     context;
    @Inject
    private Executor          executor;
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
    private DataProvider      dataProvider;
    @Inject
    private ConsoleMaskerPane progressPane;
    private GraphController   loadedController;
    private HBox              toolBar;
    private ToggleButton      bundleBtn;
    private ToggleButton      componentBtn;
    private SegmentedButton   segmentedButton;

    @PostConstruct
    public void postConstruct(final BorderPane parent) {
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
        statusBar.disableRpcProgressTracking();
        createControls();
    }

    private void createControls() {
        initStatusBar();
        initToolBar();
        if (loadedController == null) {
            final Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    loadContent(GraphController.Type.BUNDLES);
                    return null;
                }
            };
            executor.runAsync(task);
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
        threadSync.asyncExec(() -> {
            parentNode.setCenter(content);
            initStatusBar();
            updateButtonStates();
        });
    }

    private void initToolBar() {
        if (toolBar == null) {
            toolBar = new HBox();
            toolBar.setAlignment(Pos.CENTER_LEFT);
            toolBar.setSpacing(10.0);
            toolBar.setPadding(new Insets(8, 10, 8, 10));
            parentNode.setTop(toolBar);
        }
        toolBar.getChildren().clear();
        bundleBtn = new ToggleButton("Bundle Topology");
        bundleBtn.setOnAction(_ -> switchContent(GraphController.Type.BUNDLES));

        componentBtn = new ToggleButton("Component Topology");
        componentBtn.setOnAction(_ -> switchContent(GraphController.Type.COMPONENTS));

        segmentedButton = new SegmentedButton(bundleBtn, componentBtn);
        segmentedButton.getStyleClass().add(STYLE_CLASS_DARK);
        toolBar.getChildren().add(segmentedButton);
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (toolBar == null || segmentedButton == null) {
            return;
        }
        if (!isConnected) {
            segmentedButton.setDisable(true);
            return;
        }
        segmentedButton.setDisable(false);
        final var type = loadedController == null ? GraphController.Type.BUNDLES : loadedController.type();

        if (type == GraphController.Type.BUNDLES) {
            bundleBtn.setSelected(true);
            componentBtn.setSelected(false);
        } else {
            bundleBtn.setSelected(false);
            componentBtn.setSelected(true);
        }
    }

    private void switchContent(final GraphController.Type type) {
        final Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                loadContent(type);
                return null;
            }
        };
        executor.runAsync(task);
    }

    private void initStatusBar() {
        statusBar.clearAllInRight();
        if (isConnected) {
            statusBar.enableRpcProgressTracking();
            final var syncNode = Fx.initStatusBarButton(this::refreshData, "Sync", "REFRESH");
            if (!isSnapshotAgent) {
                statusBar.addToRight(syncNode);
            }
        }
        statusBar.addTo(parentNode);
    }

    private void refreshData() {
        if (loadedController != null) {
            final var type = loadedController.type();
            if (type == GraphController.Type.BUNDLES) {
                dataProvider.retrieveInfo("bundles", true);
            } else if (type == GraphController.Type.COMPONENTS) {
                dataProvider.retrieveInfo("components", true);
            }
        }
    }

    private Data<Node, GraphController> loadFXML(final String resourceName) {
        final FXMLBuilder<Node> builder = fxmlLoader.loadBundleRelative(resourceName);
        try {
            return builder.loadWithController();
        } catch (final Exception _) {
            return null;
        }
    }

}
