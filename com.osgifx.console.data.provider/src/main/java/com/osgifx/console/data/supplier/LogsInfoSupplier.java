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
package com.osgifx.console.data.supplier;

import static com.osgifx.console.data.supplier.LogsInfoSupplier.LOGS_ID;
import static com.osgifx.console.event.topics.LogReceiveEventTopics.CLEAR_LOGS_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.LOG_LISTENER_REMOVED_EVENT_TOPIC;
import static javafx.collections.FXCollections.observableArrayList;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.data.manager.RuntimeInfoSupplier;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.LogEntryListener;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.ObservableList;

@Component
@ServiceRanking(113)
@SupplierID(LOGS_ID)
@EventTopics({ AGENT_DISCONNECTED_EVENT_TOPIC, CLEAR_LOGS_TOPIC, LOG_LISTENER_REMOVED_EVENT_TOPIC })
public final class LogsInfoSupplier implements RuntimeInfoSupplier, LogEntryListener, EventHandler {

    public static final String LOGS_ID = "logs";

    private static final int CACHE_INVALIDATE_INITIAL_DELAY = 0;
    private static final int CACHE_INVALIDATE_DELAY         = 5;
    private static final int CACHE_INVALIDATE_THRESHOLD     = 100;
    private static final int CACHE_INVALIDATE_RANGE_START   = 0;
    private static final int CACHE_INVALIDATE_RANGE_END     = 50;

    @Reference
    private LoggerFactory               factory;
    @Reference
    private Executor                    executor;
    @Reference
    private ThreadSynchronize           threadSync;
    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile Supervisor         supervisor;
    private FluentLogger                logger;
    private volatile ScheduledFuture<?> future;
    private final Lock                  lock = new ReentrantLock();

    private final ObservableList<XLogEntryDTO> logs = observableArrayList();

    @Activate
    void init() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Deactivate
    void deactivate() {
        future.cancel(true);
    }

    @Override
    public void retrieve() {
        logger.atInfo().log("Skipped log events info retrieval as it will be pushed by remote runtime agent");
    }

    @Override
    public ObservableList<?> supply() {
        return logs;
    }

    @Override
    public void logged(final XLogEntryDTO logEntry) {
        if (future == null) {
            executor.scheduleWithFixedDelay(() -> {
                lock.lock();
                try {
                    final var size = logs.size();
                    if (size > CACHE_INVALIDATE_THRESHOLD) {
                        logs.remove(CACHE_INVALIDATE_RANGE_START, CACHE_INVALIDATE_RANGE_END);
                    }
                } finally {
                    lock.unlock();
                }
            }, Duration.ofSeconds(CACHE_INVALIDATE_INITIAL_DELAY), Duration.ofSeconds(CACHE_INVALIDATE_DELAY));
        }
        lock.lock();
        try {
            logs.add(logEntry);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void handleEvent(final Event event) {
        final var topic = event.getTopic();
        threadSync.asyncExec(logs::clear);
        if (AGENT_DISCONNECTED_EVENT_TOPIC.equals(topic) || LOG_LISTENER_REMOVED_EVENT_TOPIC.equals(topic)) {
            future.cancel(true);
            future = null;
        }
    }

}
