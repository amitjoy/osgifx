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
package com.osgifx.console.ui.dto;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.di.FXMLBuilder;
import org.eclipse.fx.ui.di.FXMLBuilder.Data;
import org.eclipse.fx.ui.di.FXMLLoader;
import org.eclipse.fx.ui.di.FXMLLoaderFactory;

import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

@SuppressWarnings("deprecation")
public final class DtoFxUI {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private ConsoleStatusBar  statusBar;
    @Inject
    private BorderPane        parentNode;
    @Inject
    @FXMLLoader
    private FXMLLoaderFactory fxmlLoader;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;
    @Inject
    private ConsoleMaskerPane progressPane;
    private DtoFxController   fxController;

    @PostConstruct
    public void postConstruct() {
        createControls();
        logger.atDebug().log("DTO part has been initialized");
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
        progressPane.setVisible(true);
        loadContent();
    }

    private void loadContent() {
        final Task<Void> task = new Task<>() {

            Data<Node, DtoFxController> loadedData;

            @Override
            protected Void call() throws Exception {
                loadedData = loadFXML("/fxml/tab-content.fxml");
                if (loadedData == null) {
                    return null;
                }
                fxController = loadedData.getController();
                return null;
            }

            @Override
            protected void succeeded() {
                parentNode.getChildren().clear();
                parentNode.setCenter(loadedData.getNode());
                initStatusBar(parentNode);
                progressPane.setVisible(false);
            }
        };
        parentNode.getChildren().clear();
        progressPane.addTo(parentNode);
        initStatusBar(parentNode);
        CompletableFuture.runAsync(task);
    }

    private void initStatusBar(final BorderPane parent) {
        if (isConnected) {
            final var node = Fx.initStatusBarButton(() -> fxController.updateModel(), "Refresh", "REFRESH");
            statusBar.clearAllInRight();
            if (!isSnapshotAgent) {
                statusBar.addToRight(node);
            }
        } else {
            statusBar.clearAllInRight();
        }
        statusBar.addTo(parent);
    }

    private Data<Node, DtoFxController> loadFXML(final String resourceName) {
        final FXMLBuilder<Node> builder = fxmlLoader.loadBundleRelative(resourceName);
        try {
            return builder.loadWithController();
        } catch (final Exception e) {
            return null;
        }
    }

}
