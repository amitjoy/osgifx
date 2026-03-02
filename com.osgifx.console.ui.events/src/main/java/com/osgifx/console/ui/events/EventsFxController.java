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
package com.osgifx.console.ui.events;

import static com.osgifx.console.event.topics.EventReceiveEventTopics.CLEAR_EVENTS_TOPIC;
import static com.osgifx.console.event.topics.EventReceiveEventTopics.EVENT_RECEIVE_STARTED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.EventReceiveEventTopics.EVENT_RECEIVE_STOPPED_EVENT_TOPIC;

import java.io.IOException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.events.converter.EventManager;
import com.osgifx.console.ui.events.dialog.SendEventDialog;
import com.osgifx.console.ui.events.dialog.TopicEntryDialog;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public final class EventsFxController {

    private static final String PID = "event.receive.topics";

    @Log
    @Inject
    private FluentLogger                   logger;
    @Inject
    @LocalInstance
    private FXMLLoader                     loader;
    @FXML
    private TableView<XEventDTO>           table;
    @Inject
    @OSGiBundle
    private BundleContext                  context;
    @Inject
    @Named("is_connected")
    private boolean                        isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                        isSnapshotAgent;
    @Inject
    private DataProvider                   dataProvider;
    @Inject
    private IEventBroker                   eventBroker;
    @Inject
    private Executor                       executor;
    @Inject
    @Optional
    private Supervisor                     supervisor;
    @Inject
    private ThreadSynchronize              threadSync;
    @Inject
    private ConfigurationAdmin             configAdmin;
    @Inject
    @org.eclipse.e4.core.di.annotations.Optional
    @ContextValue("subscribed_topics")
    private ContextBoundValue<Set<String>> subscribedTopics;
    @Inject
    @org.eclipse.fx.core.di.Service(filterExpression = "(supplier.id=events)")
    private EventListener                  eventListener;
    @Inject
    private IEclipseContext                eclipseContext;
    @Inject
    private EventManager                   eventManager;
    @FXML
    private Button                         clearEventsButton;
    @FXML
    private Button                         toggleEventReceiveButton;
    @FXML
    private Button                         sendEventButton;

    private TableRowDataFeatures<XEventDTO> previouslyExpanded;
    private final BooleanSupplier           isReceivingEvent        = () -> Boolean.getBoolean("is_receiving_event");
    private final Consumer<Boolean>         isReceivingEventUpdater = flag -> System.setProperty("is_receiving_event",
            String.valueOf(flag));

    @FXML
    public void initialize() {
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            initButtonIcons();
            updateButtonStates();
            return;
        }
        try {
            createControls();
            Fx.disableSelectionModel(table);
            initButtonIcons();
            updateButtonStates();
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    @FXML
    public void clearEvents() {
        eventBroker.post(CLEAR_EVENTS_TOPIC, "");
        logger.atInfo().log("Clear events table command sent");
    }

    @FXML
    public void toggleEventReceive() {
        Agent agent;
        if (supervisor == null || (agent = supervisor.getAgent()) == null) {
            logger.atInfo().log("Agent not connected");
            return;
        }
        final var flag = !isReceivingEvent.getAsBoolean();
        if (flag) {
            final var dialog = new TopicEntryDialog();
            ContextInjectionFactory.inject(dialog, eclipseContext);
            dialog.init();

            final var event = dialog.showAndWait();
            if (!event.isPresent()) {
                return;
            }
            final var topics = event.get();
            if (topics.isEmpty()) {
                return;
            }
            subscribedTopics.publish(topics);
            updateConfig(topics);

            // @formatter:off
            executor.runAsync(agent::enableReceivingEvent)
                    .thenRun(() -> threadSync.asyncExec(() -> {
                                            eventBroker.post(EVENT_RECEIVE_STARTED_EVENT_TOPIC, String.valueOf(flag));
                                            Fx.showSuccessNotification("Event Notification", "Events will now be received");
                                            updateToggleButtonLabel(true);}))
                    .thenRun(() -> supervisor.addOSGiEventListener(eventListener))
                    .thenRun(() -> logger.atInfo().log("OSGi events will now be received"))
                    .thenRun(() -> isReceivingEventUpdater.accept(true));
            // @formatter:on
        } else {
            subscribedTopics.publish(Set.of());
            updateConfig(Set.of());

            // @formatter:off
            executor.runAsync(agent::disableReceivingEvent)
                    .thenRun(() -> threadSync.asyncExec(() -> {
                                            eventBroker.post(EVENT_RECEIVE_STOPPED_EVENT_TOPIC, String.valueOf(flag));
                                            Fx.showSuccessNotification("Event Notification", "Events will not be received anymore");
                                            updateToggleButtonLabel(false);}))
                    .thenRun(() -> supervisor.removeOSGiEventListener(eventListener))
                    .thenRun(() -> logger.atInfo().log("OSGi events will not be received anymore"))
                    .thenRun(() -> isReceivingEventUpdater.accept(false));
            // @formatter:on
        }
    }

    @FXML
    public void sendEvent() {
        final var dialog = new SendEventDialog();
        ContextInjectionFactory.inject(dialog, eclipseContext);
        logger.atInfo().log("Injected send event dialog to eclipse context");
        dialog.init();

        final var event = dialog.showAndWait();
        if (event.isPresent()) {
            final Task<Void> sendEventTask = new Task<>() {

                @Override
                protected Void call() throws Exception {
                    try {
                        final var dto        = event.get();
                        final var topic      = dto.topic();
                        final var isSync     = dto.isSync();
                        final var properties = dto.properties();

                        if (StringUtils.isBlank(topic) || properties == null) {
                            return null;
                        }

                        XResultDTO result;
                        final var  parsedTopic = topic.strip();
                        if (isSync) {
                            result = eventManager.sendEvent(parsedTopic, properties);
                        } else {
                            result = eventManager.postEvent(parsedTopic, properties);
                        }
                        if (result == null) {
                            return null;
                        }
                        switch (result.result) {
                            case XResultDTO.SUCCESS:
                                threadSync.asyncExec(() -> Fx.showSuccessNotification("Send Event", result.response));
                                logger.atInfo().log("Event sent successfully to '%s'", parsedTopic);
                                break;
                            case XResultDTO.ERROR:
                                threadSync.asyncExec(() -> Fx.showErrorNotification("Send Event", result.response));
                                logger.atError().log("Event could not be sent to '%s'", parsedTopic);
                                break;
                            case XResultDTO.SKIPPED:
                                threadSync.asyncExec(() -> Fx.showErrorNotification("Send Event", result.response));
                                logger.atError().log("Event could not be sent to '%s' because %s", parsedTopic,
                                        result.response);
                                break;
                            default:
                                break;
                        }
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Event could not be sent");
                        threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                    }
                    return null;
                }
            };
            executor.runAsync(sendEventTask);
        }
    }

    private void updateButtonStates() {
        final var disableActions = !isConnected || isSnapshotAgent;
        clearEventsButton.setDisable(disableActions);
        toggleEventReceiveButton.setDisable(disableActions);
        sendEventButton.setDisable(disableActions);
        updateToggleButtonLabel(isReceivingEvent.getAsBoolean());
    }

    private void initButtonIcons() {
        clearEventsButton.setGraphic(createIcon("/graphic/icons/clear.png"));
        sendEventButton.setGraphic(createIcon("/graphic/icons/send-event.png"));
    }

    private void updateToggleButtonLabel(final boolean isReceiving) {
        if (isReceiving) {
            toggleEventReceiveButton.setText("Stop Receiving Events");
            toggleEventReceiveButton.setGraphic(createIcon("/graphic/icons/stop.png"));
        } else {
            toggleEventReceiveButton.setText("Start Receiving Events");
            toggleEventReceiveButton.setGraphic(createIcon("/graphic/icons/start.png"));
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

    private void updateConfig(final Set<String> topics) {
        try {
            final var configuration = configAdmin.getConfiguration(PID, "?");
            if (!topics.isEmpty()) {
                final Dictionary<String, String[]> properties = FrameworkUtil
                        .asDictionary(Map.of("topics", topics.toArray(new String[0])));
                configuration.update(properties);
            } else {
                configuration.delete();
            }
        } catch (final IOException e) {
            logger.atError().withException(e).log("Cannot retrieve configuration '%s'", PID);
        }
    }

    private void createControls() {
        final var expandedNode   = (GridPane) Fx.loadFXML(loader, context, "/fxml/expander-column-content.fxml");
        final var controller     = (EventDetailsFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XEventDTO>(current -> {
                                     if (previouslyExpanded != null
                                             && current.getValue() == previouslyExpanded.getValue()) {
                                         return expandedNode;
                                     }
                                     if (previouslyExpanded != null && previouslyExpanded.isExpanded()) {
                                         previouslyExpanded.toggleExpanded();
                                     }
                                     controller.initControls(current.getValue());
                                     previouslyExpanded = current;
                                     return expandedNode;
                                 });

        final var receivedAtColumn = new TableColumn<XEventDTO, Date>("Received At");

        receivedAtColumn.setPrefWidth(290);
        receivedAtColumn.setCellValueFactory(new DTOCellValueFactory<>("received", Date.class));

        final var topicColumn = new TableColumn<XEventDTO, String>("Topic");

        topicColumn.setPrefWidth(650);
        topicColumn.setCellValueFactory(new DTOCellValueFactory<>("topic", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(receivedAtColumn);
        table.getColumns().add(topicColumn);

        final var events = dataProvider.events();
        threadSync.asyncExec(() -> {
            table.setItems(events);
            TableFilter.forTableView(table).lazy(true).apply();
            sortByReceivedAt(receivedAtColumn);
        });
    }

    private void sortByReceivedAt(final TableColumn<XEventDTO, Date> column) {
        column.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(column);
        table.sort();
    }

}
