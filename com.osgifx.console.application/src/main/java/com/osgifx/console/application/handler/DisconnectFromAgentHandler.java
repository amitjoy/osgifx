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
package com.osgifx.console.application.handler;

import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

public final class DisconnectFromAgentHandler {

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    private IEclipseContext            context;
    @Inject
    private Supervisor                 supervisor;
    @Inject
    private IEventBroker               eventBroker;
    @Inject
    @ContextValue("is_connected")
    private ContextBoundValue<Boolean> isConnected;

    @PostConstruct
    public void init() {
        context.declareModifiable("is_connected");
    }

    @Execute
    public void execute() {
        try {
            supervisor.getAgent().abort();
            eventBroker.post(AGENT_DISCONNECTED_EVENT_TOPIC, "");
            isConnected.publish(false);
            Fx.showSuccessNotification("Agent Connection", "Agent connection has been successfully aborted");
        } catch (final Exception e) {
            logger.atError().withException(e).log("Agent connection cannot be aborted");
            Fx.showErrorNotification("Agent Connection", "Agent connection cannot be aborted");
        }
    }

    @CanExecute
    public boolean canExecute() {
        return supervisor.getAgent() != null;
    }

}
