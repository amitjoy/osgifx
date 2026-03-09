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
package com.osgifx.console.ui.events;

import static com.osgifx.console.constants.FxConstants.ROOT_FXML;
import static com.osgifx.console.event.topics.EventReceiveEventTopics.EVENT_RECEIVE_STARTED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.EventReceiveEventTopics.EVENT_RECEIVE_STOPPED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.PopOver;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public final class EventsFxUI {

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
    private ConsoleMaskerPane progressPane;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;
    @Inject
    private DataProvider      dataProvider;
    @Inject
    private IEclipseContext   eclipseContext;

    private PopOver popover;

    @PostConstruct
    public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
        logger.atDebug().log("Events part has been initialized");
    }

    @Inject
    @Optional
    private void updateOnEventReceiveStarted(@UIEventTopic(EVENT_RECEIVE_STARTED_EVENT_TOPIC) final String data,
                                             final BorderPane parent,
                                             @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    @Inject
    @Optional
    private void updateOnEventReceiveStopped(@UIEventTopic(EVENT_RECEIVE_STOPPED_EVENT_TOPIC) final String data,
                                             final BorderPane parent,
                                             @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
    }

    @Inject
    @Optional
    private void updateOnAgentConnectedEvent(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data,
                                             final BorderPane parent,
                                             @LocalInstance final FXMLLoader loader) {
        logger.atInfo().log("Agent connected event received");
        createControls(parent, loader);
    }

    @Inject
    @Optional
    private void updateOnAgentDisconnectedEvent(@UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data,
                                                final BorderPane parent,
                                                @LocalInstance final FXMLLoader loader) {
        logger.atInfo().log("Agent disconnected event received");
        statusBar.disableRpcProgressTracking();
        createControls(parent, loader);
    }

    private void createControls(final BorderPane parent, final FXMLLoader loader) {
        progressPane.setVisible(true);
        final Task<Void> task = new Task<>() {

            Node tabContent;

            @Override
            protected Void call() throws Exception {
                tabContent = Fx.loadFXML(loader, context, ROOT_FXML);
                return null;
            }

            @Override
            protected void succeeded() {
                parent.getChildren().clear();
                parent.setCenter(tabContent);
                initStatusBar(parent);
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
            final var isEventAdminAvailable = dataProvider.runtimeCapabilities().stream()
                    .anyMatch(cap -> "EVENT_ADMIN".equals(cap.id) && cap.isAvailable);
            if (!isSnapshotAgent && isEventAdminAvailable) {
                final var button = (Button) Fx.initStatusBarButton(null, "Subscribed Event Topics", "GEAR");
                button.setOnAction(_ -> showSubscribedEventTopicsPopover(button));
                statusBar.addToRight(button);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void showSubscribedEventTopicsPopover(final Node source) {
        if (popover != null && popover.isShowing()) {
            popover.hide();
            return;
        }
        final var content = new VBox();
        content.setPadding(new Insets(10));
        content.setPrefWidth(500);
        content.setPrefHeight(300);

        final var listView = new ListView<String>();
        final var topics   = (Set<String>) eclipseContext.get("subscribed_topics");
        if (topics != null) {
            listView.getItems().addAll(topics);
        }
        final var placeholder = new Label("No subscribed topic");
        listView.setPlaceholder(placeholder);

        content.getChildren().add(listView);

        popover = new PopOver(content);
        popover.setTitle("Subscribed Event Topics");
        popover.setArrowLocation(PopOver.ArrowLocation.BOTTOM_RIGHT);
        popover.setDetachable(false);
        popover.show(source);
    }

}
