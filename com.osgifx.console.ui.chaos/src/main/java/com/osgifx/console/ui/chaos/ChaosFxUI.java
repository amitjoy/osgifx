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
package com.osgifx.console.ui.chaos;

import static com.osgifx.console.constants.FxConstants.ROOT_FXML;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

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

import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public final class ChaosFxUI {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    @OSGiBundle
    private BundleContext     context;
    @Inject
    private Executor          executor;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;
    @Inject
    private ConsoleStatusBar  statusBar;
    @Inject
    private ConsoleMaskerPane progressPane;

    private ChaosFxController controller;

    @PostConstruct
    public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
        setupShutdownHook(parent);
        logger.atDebug().log("Chaos Monkey part has been initialized");
    }

    private void setupShutdownHook(final BorderPane parent) {
        parent.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene != null && newScene.getWindow() != null) {
                newScene.getWindow().setOnHiding(_ -> {
                    if (controller != null && controller.isRunning()) {
                        logger.atInfo().log("Emergency shutdown triggered for Chaos Monkey");
                        controller.emergencyShutdown();
                    }
                });
            }
        });
    }

    @Focus
    public void onFocus() {
        // Chaos monkey has its own data provider logic or reuses others.
    }

    @Inject
    @Optional
    private void updateOnAgentConnectedEvent(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data,
                                             final BorderPane parent,
                                             @LocalInstance final FXMLLoader loader) {
        logger.atInfo().log("Agent connected event received");
        stopExistingChaos();
        createControls(parent, loader);
    }

    @Inject
    @Optional
    private void updateOnAgentDisconnectedEvent(@UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data,
                                                final BorderPane parent,
                                                @LocalInstance final FXMLLoader loader) {
        logger.atInfo().log("Agent disconnected event received");
        stopExistingChaos();
        statusBar.disableRpcProgressTracking();
        createControls(parent, loader);
    }

    private void stopExistingChaos() {
        if (controller != null && controller.isRunning()) {
            logger.atInfo().log("Stopping existing chaos before reload");
            controller.emergencyShutdown();
        }
    }

    private void createControls(final BorderPane parent, final FXMLLoader loader) {
        if (!isConnected) {
            progressPane.setVisible(false);
            parent.getChildren().clear();
            parent.setCenter(Fx.createDisconnectedPlaceholder());
            statusBar.addTo(parent);
            return;
        }
        if (isSnapshotAgent) {
            progressPane.setVisible(false);
            parent.getChildren().clear();
            parent.setCenter(Fx.createSnapshotPlaceholder());
            statusBar.addTo(parent);
            return;
        }
        progressPane.setVisible(true);
        final Task<Void> task = new Task<>() {

            Node tabContent;

            @Override
            protected Void call() throws Exception {
                tabContent = Fx.loadFXML(loader, context, ROOT_FXML);
                controller = loader.getController();
                return null;
            }

            @Override
            protected void succeeded() {
                parent.getChildren().clear();
                parent.setCenter(tabContent);
                statusBar.addTo(parent);
                progressPane.setVisible(false);
            }

            @Override
            protected void failed() {
                logger.atError().withException(getException()).log("Failed to load Chaos Monkey UI");
                progressPane.setVisible(false);
            }
        };
        parent.getChildren().clear();
        progressPane.addTo(parent);
        initStatusBar(parent);
        executor.runAsync(task);
    }

    private void initStatusBar(final BorderPane parent) {
        statusBar.clearAllInRight();
        statusBar.addTo(parent);
        if (isConnected) {
            statusBar.enableRpcProgressTracking();
        }
    }

}
