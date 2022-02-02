/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.controlsfx.control.MaskerPane;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

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
    private EPartService                  partService;
    @Inject
    private ThreadSynchronize             threadSync;
    private final MaskerPane              progressPane = new MaskerPane();
    private final AtomicReference<String> loadedType   = new AtomicReference<>();

    private static final String BUNDLES_GRAPH_TYPE    = "Bundles";
    private static final String COMPONENTS_GRAPH_TYPE = "Components";

    @PostConstruct
    public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
        logger.atDebug().log("Graph part has been initialized");
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
        statusBar.addTo(parent);
        if (loadedType.get() == null) {
            threadSync.asyncExec(() -> FxDialog.showChoiceDialog("Select Graph Generation Type", getClass().getClassLoader(),
                    "/graphic/images/graph.png", type -> {
                        final Task<?> task = new Task<Void>() {
                                                 @Override
                                                 protected Void call() throws Exception {
                                                     loadContent(parent, loader, type);
                                                     return null;
                                                 }
                                             };
                        final Thread thread = new Thread(task);
                        thread.setDaemon(true);
                        thread.start();
                    }, () -> partService.hidePart(part), BUNDLES_GRAPH_TYPE, BUNDLES_GRAPH_TYPE, COMPONENTS_GRAPH_TYPE));
        } else {
            loadContent(parent, loader, loadedType.get());
        }
    }

    private void loadContent(final BorderPane parent, final FXMLLoader loader, final String type) {
        Node tabContent = null;
        threadSync.asyncExec(() -> parent.setCenter(progressPane));
        if (BUNDLES_GRAPH_TYPE.equalsIgnoreCase(type)) {
            tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content-for-bundles.fxml");
            loadedType.set(BUNDLES_GRAPH_TYPE);
        } else {
            tabContent = Fx.loadFXML(loader, context, "/fxml/tab-content-for-components.fxml");
            loadedType.set(COMPONENTS_GRAPH_TYPE);
        }
        final Node content = tabContent; // required for lambda as it needs to be effectively final
        progressPane.setVisible(false);
        threadSync.asyncExec(() -> parent.setCenter(content));
    }

}
