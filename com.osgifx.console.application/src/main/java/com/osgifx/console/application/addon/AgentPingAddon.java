/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.application.addon;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.RpcType.SOCKET_RPC;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.application.dialog.SocketConnectionSettingDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

public final class AgentPingAddon {

    private static final Duration INITIAL_DELAY = Duration.ofSeconds(0);
    private static final Duration MAX_DELAY     = Duration.ofSeconds(5);

    @Log
    @Inject
    private FluentLogger                                  logger;
    @Inject
    private Executor                                      executor;
    @Inject
    @Optional
    private Supervisor                                    supervisor;
    @Inject
    private IEventBroker                                  eventBroker;
    @Inject
    @ContextValue("is_connected")
    private ContextBoundValue<Boolean>                    isConnected;
    @Inject
    @Optional
    @ContextValue("is_local_agent")
    private ContextBoundValue<Boolean>                    isLocalAgent;
    @Inject
    @ContextValue("connected.agent")
    private ContextBoundValue<String>                     connectedAgent;
    @Inject
    @Optional
    @ContextValue("selected.settings")
    private ContextBoundValue<SocketConnectionSettingDTO> selectedSettings;
    private volatile ScheduledFuture<?>                   future;

    @PostConstruct
    public void init() {
        logger.atInfo().log("Agent ping addon has been initialized");
    }

    @Inject
    @Optional
    private void agentConnected(@EventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent connected event has been received");
        if (supervisor.getType() == SOCKET_RPC) {
            future = executor.scheduleWithFixedDelay(() -> {
                try {
                    supervisor.getAgent().ping();
                } catch (final Exception e) {
                    logger.atWarning().log("Agent ping request did not succeed");
                    eventBroker.post(AGENT_DISCONNECTED_EVENT_TOPIC, "");

                    isConnected.publish(false);
                    isLocalAgent.publish(false);
                    selectedSettings.publish(null);
                    connectedAgent.publish(null);
                }
            }, INITIAL_DELAY, MAX_DELAY);
        }
        logger.atInfo().log("Agent ping scheduler has been started");
    }

    @Inject
    @Optional
    private void agentDisconnected(@EventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent disconnected event has been received");
        if (future != null) {
            future.cancel(true);
        }
        connectedAgent.publish(null);
    }

    @PreDestroy
    private void destroy() {
        logger.atInfo().log("Agent ping addon has been destroyed");
    }

}
