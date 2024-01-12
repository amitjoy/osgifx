/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
package com.osgifx.console.ui.logs.handler;

import static com.osgifx.console.event.topics.LogReceiveEventTopics.LOG_RECEIVE_STARTED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.LogReceiveEventTopics.LOG_RECEIVE_STOPPED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

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
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.LogEntryListener;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

public final class LogReceiveMenuContributionHandler {

    @Log
    @Inject
    private FluentLogger            logger;
    @Inject
    @OSGiBundle
    private BundleContext           context;
    @Inject
    private Executor                executor;
    @Inject
    @Optional
    private Supervisor              supervisor;
    @Inject
    private ThreadSynchronize       threadSync;
    @Inject
    private IEventBroker            eventBroker;
    @Inject
    private EModelService           modelService;
    @Inject
    private LogEntryListener        logEntryListener;
    @Inject
    @Named("is_connected")
    private boolean                 isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                 isSnapshotAgent;
    private final BooleanSupplier   isReceivingLog        = () -> Boolean.getBoolean("is_receiving_log");
    private final Consumer<Boolean> isReceivingLogUpdater = flag -> System.setProperty("is_receiving_log",
            String.valueOf(flag));

    @AboutToShow
    public void aboutToShow(final List<MMenuElement> items, final MWindow window) {
        prepareMenu(items, isReceivingLog.getAsBoolean());
    }

    @Execute
    public void execute(final MDirectMenuItem menuItem) {
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
                        Fx.showSuccessNotification("Log Notification", "Logs will now be displayed");}))
                    .thenRun(() -> isReceivingLogUpdater.accept(true));
        } else {
            executor.runAsync(agent::disableReceivingLog)
                    .thenRun(() -> supervisor.removeOSGiLogListener(logEntryListener))
                    .thenRun(() -> logger.atInfo().log("OSGi logs will now be displayed"))
                    .thenRun(() -> threadSync.asyncExec(() -> {
                        eventBroker.post(LOG_RECEIVE_STOPPED_EVENT_TOPIC, String.valueOf(flag));
                        Fx.showSuccessNotification("Log Notification", "Logs will not be displayed anymore");}))
                    .thenRun(() -> isReceivingLogUpdater.accept(false));
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
            eventActionMenu = createLogActionMenu(Type.STOP);
        } else {
            eventActionMenu = createLogActionMenu(Type.START);
        }
        items.add(eventActionMenu);
    }

    private MDirectMenuItem createLogActionMenu(final Type type) {
        String label;
        String icon;
        if (type == Type.STOP) {
            label = "Stop Displaying Logs";
            icon  = "stop.png";
        } else {
            label = "Start Displaying Logs";
            icon  = "start.png";
        }
        final var dynamicItem = modelService.createModelElement(MDirectMenuItem.class);
        final var bsn         = context.getBundle().getSymbolicName();

        dynamicItem.setLabel(label);
        dynamicItem.setIconURI("platform:/plugin/" + bsn + "/graphic/icons/" + icon);
        dynamicItem.setContributorURI("platform:/plugin/" + bsn);
        dynamicItem.setContributionURI("bundleclass://" + bsn + "/" + getClass().getName());

        return dynamicItem;
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
            final var currentState = agent.isReceivingLogEnabled();
            if (currentState) {
                supervisor.addOSGiLogListener(logEntryListener);
                logger.atInfo().log("OSGi log listener has been added");
            } else {
                supervisor.removeOSGiLogListener(logEntryListener);
                logger.atInfo().log("OSGi log listener has been removed");
            }
        });
    }

    private enum Type {
        START,
        STOP
    }

}
