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
package com.osgifx.console.ui.events.handler;

import static com.osgifx.console.event.topics.EventReceiveEventTopics.EVENT_RECEIVE_STARTED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.EventReceiveEventTopics.EVENT_RECEIVE_STOPPED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MDirectMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.di.Service;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.EventListener;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.events.dialog.TopicEntryDialog;
import com.osgifx.console.util.fx.Fx;

public final class EventReceiveMenuContributionHandler {

    private static final String PID = "event.receive.topics";

    @Log
    @Inject
    private FluentLogger                   logger;
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
    private IEventBroker                   eventBroker;
    @Inject
    @Service(filterExpression = "(supplier.id=events)")
    private EventListener                  eventListener;
    @Inject
    private EModelService                  modelService;
    @Inject
    @Named("is_connected")
    private boolean                        isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                        isSnapshotAgent;
    @Inject
    @OSGiBundle
    private BundleContext                  bundleContext;
    @Inject
    private IEclipseContext                eclipseContext;
    @Inject
    @Optional
    @ContextValue("subscribed_topics")
    private ContextBoundValue<Set<String>> subscribedTopics;
    private final BooleanSupplier          isReceivingEvent        = () -> Boolean.getBoolean("is_receiving_event");
    private final Consumer<Boolean>        isReceivingEventUpdater = flag -> System.setProperty("is_receiving_event",
            String.valueOf(flag));

    @AboutToShow
    public void aboutToShow(final List<MMenuElement> items, final MWindow window) {
        prepareMenu(items, isReceivingEvent.getAsBoolean());
    }

    @Execute
    public void execute(final MDirectMenuItem menuItem) {
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
                                            Fx.showSuccessNotification("Event Notification", "Events will now be received");}))
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
                                            Fx.showSuccessNotification("Event Notification", "Events will not be received anymore");}))
                    .thenRun(() -> supervisor.removeOSGiEventListener(eventListener))
                    .thenRun(() -> logger.atInfo().log("OSGi events will not be received anymore"))
                    .thenRun(() -> isReceivingEventUpdater.accept(false));
            // @formatter:on
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected && !isSnapshotAgent;
    }

    private void prepareMenu(final List<MMenuElement> items, final boolean value) {
        final MDirectMenuItem eventActionMenu;
        if (value) {
            eventActionMenu = createEventActionMenu(Type.STOP);
        } else {
            eventActionMenu = createEventActionMenu(Type.START);
        }
        items.add(eventActionMenu);
    }

    private MDirectMenuItem createEventActionMenu(final Type type) {
        String label;
        String icon;
        if (type == Type.STOP) {
            label = "Stop Receiving Events";
            icon  = "stop.png";
        } else {
            label = "Start Receiving Events";
            icon  = "start.png";
        }
        final var dynamicItem = modelService.createModelElement(MDirectMenuItem.class);
        final var bsn         = bundleContext.getBundle().getSymbolicName();

        dynamicItem.setLabel(label);
        dynamicItem.setIconURI("platform:/plugin/" + bsn + "/graphic/icons/" + icon);
        dynamicItem.setContributorURI("platform:/plugin/" + bsn);
        dynamicItem.setContributionURI("bundleclass://" + bsn + "/" + getClass().getName());

        return dynamicItem;
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

    @Inject
    @Optional
    private void updateOnAgentConnectedEvent(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent connected event received");
        Agent agent;
        if (supervisor == null || (agent = supervisor.getAgent()) == null) {
            logger.atInfo().log("Agent not connected");
            return;
        }
        executor.runAsync(() -> {
            final var currentState = agent.isReceivingEventEnabled();
            if (currentState) {
                supervisor.addOSGiEventListener(eventListener);
                logger.atInfo().log("OSGi event listener has been added");
            } else {
                supervisor.removeOSGiEventListener(eventListener);
                logger.atInfo().log("OSGi event listener has been removed");
            }
        });
    }

    private enum Type {
        START,
        STOP
    }

}
