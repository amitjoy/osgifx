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
package com.osgifx.console.ui.events;

import static com.osgifx.console.constants.FxConstants.ROOT_FXML;
import static com.osgifx.console.event.topics.EventReceiveEventTopics.EVENT_RECEIVE_STARTED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.EventReceiveEventTopics.EVENT_RECEIVE_STOPPED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.ui.events.dialog.SubscribedEventsDialog;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

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
    private IEclipseContext   eclipseContext;

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
        if (isConnected) {
            final var node = Fx.initStatusBarButton(this::showSubscribedEventTopicsDialog, "Subscribed Event Topics", "GEAR");
            statusBar.clearAllInRight();
            if (!isSnapshotAgent) {
                statusBar.addToRight(node);
            }
        } else {
            statusBar.clearAllInRight();
        }
        statusBar.addTo(parent);
    }

    private void showSubscribedEventTopicsDialog() {
        final var dialog = new SubscribedEventsDialog();
        ContextInjectionFactory.inject(dialog, eclipseContext);
        dialog.init();
        dialog.showAndWait();
    }

}
