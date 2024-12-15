/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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

import static com.osgifx.console.event.topics.EventReceiveEventTopics.CLEAR_EVENTS_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

public final class ClearEventsTableHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private IEventBroker eventBroker;
    @Inject
    @Named("is_connected")
    private boolean      isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean      isSnapshotAgent;

    @Execute
    public void execute() {
        eventBroker.post(CLEAR_EVENTS_TOPIC, "");
        logger.atInfo().log("Clear events table command sent");
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected && !isSnapshotAgent;
    }

}
