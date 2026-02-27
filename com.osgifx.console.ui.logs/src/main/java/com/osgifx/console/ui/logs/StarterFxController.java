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
package com.osgifx.console.ui.logs;

import static com.osgifx.console.event.topics.LogReceiveEventTopics.CLEAR_LOGS_TOPIC;
import static com.osgifx.console.event.topics.LogReceiveEventTopics.LOG_RECEIVE_STARTED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.LogReceiveEventTopics.LOG_RECEIVE_STOPPED_EVENT_TOPIC;
import static org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.SegmentedButton;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.ui.di.FXMLBuilder;
import org.eclipse.fx.ui.di.FXMLLoader;
import org.eclipse.fx.ui.di.FXMLLoaderFactory;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.LogEntryListener;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

@SuppressWarnings("deprecation")
public final class StarterFxController {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    @FXMLLoader
    private FXMLLoaderFactory loader;
    @Inject
    private IEventBroker      eventBroker;
    @Inject
    private Executor          executor;
    @Inject
    @Optional
    private Supervisor        supervisor;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    private LogEntryListener  logEntryListener;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;
    @FXML
    private BorderPane        mainPane;
    @FXML
    private ToggleButton      logsViewButton;
    @FXML
    private ToggleButton      configurationsViewButton;
    @FXML
    private SegmentedButton   logsActionTypeButton;
    @FXML
    private Button            clearLogsButton;
    @FXML
    private Button            toggleLogReceiveButton;

    private final BooleanSupplier   isReceivingLog        = () -> Boolean.getBoolean("is_receiving_log");
    private final Consumer<Boolean> isReceivingLogUpdater = flag -> System.setProperty("is_receiving_log",
            String.valueOf(flag));

    @FXML
    public void initialize() {
        initLogsActionTypeButton();
        initButtons();
        initButtonIcons();
        showLogEvents();
        updateButtonStates();
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void initButtonIcons() {
        clearLogsButton.setGraphic(createIcon("/graphic/icons/clear.png"));
    }

    @FXML
    public void clearLogs() {
        eventBroker.post(CLEAR_LOGS_TOPIC, "");
        logger.atInfo().log("Clear logs table command sent");
    }

    @FXML
    public void toggleLogReceive() {
        Agent agent;
        if (supervisor == null || (agent = supervisor.getAgent()) == null) {
            logger.atInfo().log("Agent not connected");
            return;
        }
        final var flag = !isReceivingLog.getAsBoolean();
        if (flag) {
            // @formatter:off
            executor.runAsync(agent::enableReceivingLog)
                    .thenRun(() -> supervisor.addOSGiLogListener(logEntryListener))
                    .thenRun(() -> logger.atInfo().log("OSGi logs will now be displayed"))
                    .thenRun(() -> threadSync.asyncExec(() -> {
                        eventBroker.post(LOG_RECEIVE_STARTED_EVENT_TOPIC, String.valueOf(flag));
                        Fx.showSuccessNotification("Log Notification", "Logs will now be displayed");
                        updateToggleButtonLabel(true);}))
                    .thenRun(() -> isReceivingLogUpdater.accept(true));
        } else {
            executor.runAsync(agent::disableReceivingLog)
                    .thenRun(() -> supervisor.removeOSGiLogListener(logEntryListener))
                    .thenRun(() -> logger.atInfo().log("OSGi logs will not be displayed anymore"))
                    .thenRun(() -> threadSync.asyncExec(() -> {
                        eventBroker.post(LOG_RECEIVE_STOPPED_EVENT_TOPIC, String.valueOf(flag));
                        Fx.showSuccessNotification("Log Notification", "Logs will not be displayed anymore");
                        updateToggleButtonLabel(false);}))
                    .thenRun(() -> isReceivingLogUpdater.accept(false));
            // @formatter:on
        }
    }

    private void initLogsActionTypeButton() {
        logsActionTypeButton.getStyleClass().add(STYLE_CLASS_DARK);
        logsActionTypeButton.getToggleGroup().selectedToggleProperty().addListener((_, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });
    }

    private void initButtons() {
        logsViewButton.setOnMouseClicked(_ -> showLogEvents());
        configurationsViewButton.setOnMouseClicked(_ -> showConfigurations());
    }

    private void updateButtonStates() {
        final var disableActions = !isConnected || isSnapshotAgent;
        clearLogsButton.setDisable(disableActions);
        toggleLogReceiveButton.setDisable(disableActions);
        updateToggleButtonLabel(isReceivingLog.getAsBoolean());
    }

    private void updateToggleButtonLabel(final boolean isReceiving) {
        if (isReceiving) {
            toggleLogReceiveButton.setText("Stop Displaying Logs");
            toggleLogReceiveButton.setGraphic(createIcon("/graphic/icons/stop.png"));
        } else {
            toggleLogReceiveButton.setText("Start Displaying Logs");
            toggleLogReceiveButton.setGraphic(createIcon("/graphic/icons/start.png"));
        }
    }

    private ImageView createIcon(final String path) {
        final var image     = new Image(getClass().getResourceAsStream(path));
        final var imageView = new ImageView(image);
        imageView.setFitHeight(16.0);
        imageView.setFitWidth(16.0);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private void showLogEvents() {
        final var node = loadFXML("/fxml/tab-content-for-logs.fxml");
        mainPane.setCenter(node);
        logger.atDebug().log("Loaded log events");
    }

    private void showConfigurations() {
        final var node = loadFXML("/fxml/tab-content-for-configurations.fxml");
        mainPane.setCenter(node);
        logger.atDebug().log("Loaded log configurations");
    }

    private Node loadFXML(final String resourceName) {
        final FXMLBuilder<Node> builder = loader.loadBundleRelative(resourceName);
        try {
            return builder.load();
        } catch (final Exception e) {
            return null;
        }
    }

}
