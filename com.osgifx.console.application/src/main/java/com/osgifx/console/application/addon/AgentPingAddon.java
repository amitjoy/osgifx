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
package com.osgifx.console.application.addon;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.supervisor.Supervisor;

public final class AgentPingAddon {

    private static final long   INITIAL_DELAY = Duration.ofSeconds(0).toMillis();
    private static final long   MAX_DELAY     = Duration.ofSeconds(5).toMillis();
    private static final String THREAD_NAME   = "osgifx-agent-ping";

    @Log
    @Inject
    private FluentLogger                logger;
    @Inject
    private Supervisor                  supervisor;
    @Inject
    private IEventBroker                eventBroker;
    private ScheduledExecutorService    executor;
    private volatile ScheduledFuture<?> future;

    @PostConstruct
    public void init() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, THREAD_NAME));
        logger.atInfo().log("Agent ping addon has been initialized");
    }

    @Inject
    @Optional
    private void agentConnected(@EventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent connected event has been received");
        future = executor.scheduleWithFixedDelay(() -> {
            try {
                supervisor.getAgent().ping();
            } catch (final Exception e) {
                logger.atWarning().log("Agent ping request did not succeed");
                eventBroker.post(AGENT_DISCONNECTED_EVENT_TOPIC, "");
            }
        }, INITIAL_DELAY, MAX_DELAY, MILLISECONDS);
        logger.atInfo().log("Agent ping scheduler has been started");
    }

    @Inject
    @Optional
    private void agentDisconnected(@EventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent disconnected event has been received");
        future.cancel(true);
        future = null;
    }

    @PreDestroy
    private void destory() {
        executor.shutdownNow();
        logger.atInfo().log("Agent ping addon has been destroyed");
    }

}
