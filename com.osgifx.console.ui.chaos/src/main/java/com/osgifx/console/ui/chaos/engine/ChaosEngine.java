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
package com.osgifx.console.ui.chaos.engine;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.chaos.model.ActionLog;

import javafx.application.Platform;

public final class ChaosEngine {

    private final Map<String, ChaosEvent>            victimLedger   = new ConcurrentHashMap<>();
    private final AtomicBoolean                      isRunning      = new AtomicBoolean(false);
    private ScheduledExecutorService                 scheduler;
    private ChaosConfig                              config;
    private Supervisor                               supervisor;
    private DataProvider                             dataProvider;
    private Consumer<ActionLog>                      logConsumer;
    private final Map<String, ChaosEvent.TargetType> offlineTargets = new ConcurrentHashMap<>();
    private final DateTimeFormatter                  timeFormatter  = DateTimeFormatter.ofPattern("HH:mm:ss");
    private Instant                                  startTime;

    public void start(final ChaosConfig config,
                      final Supervisor supervisor,
                      final DataProvider dataProvider,
                      final Consumer<ActionLog> logConsumer) {
        this.config       = requireNonNull(config);
        this.supervisor   = requireNonNull(supervisor);
        this.dataProvider = requireNonNull(dataProvider);
        this.logConsumer  = requireNonNull(logConsumer);
        this.startTime    = Instant.now();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "chaos-engine"));
        isRunning.set(true);

        scheduler.scheduleAtFixedRate(this::cycle, 0, config.actionInterval, SECONDS);
        log("🚀", "Chaos engine unleashed", "Engine");
    }

    public void stop() {
        if (!isRunning.get()) {
            return;
        }
        isRunning.set(false);
        log("⛔", "Chaos engine halted", "Engine");
        revertAllVictims();
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void cycle() {
        try {
            if (Duration.between(startTime, Instant.now()).toMinutes() >= config.autoStopMinutes) {
                log("⏰", "Auto-stop timer reached (" + config.autoStopMinutes + " min)", "Engine");
                Platform.runLater(this::stop);
                return;
            }
            revertExpiredVictims();
            selectAndDisruptVictims();
        } catch (final Exception e) {
            log("❌", "Chaos cycle failed: " + e.getMessage(), "Engine");
        }
    }

    private void revertExpiredVictims() {
        final Instant                             now = Instant.now();
        final Iterator<Entry<String, ChaosEvent>> it  = victimLedger.entrySet().iterator();
        while (it.hasNext()) {
            final ChaosEvent event = it.next().getValue();
            if (now.isAfter(event.scheduledRevertAt) && !event.isReverted) {
                revertVictim(event);
                it.remove();
            }
        }
    }

    private void selectAndDisruptVictims() {
        if (victimLedger.size() >= config.concurrency) {
            return;
        }
        final List<TargetSelector.Victim> victims = TargetSelector.selectVictims(config, dataProvider,
                victimLedger.keySet());
        for (final TargetSelector.Victim victim : victims) {
            disruptVictim(victim);
        }
    }

    private void disruptVictim(final TargetSelector.Victim victim) {
        final Instant    now      = Instant.now();
        final Instant    revertAt = now.plusSeconds(config.downtimeDuration);
        final ChaosEvent event    = new ChaosEvent(victim.id, victim.name, victim.type, now, revertAt);

        try {
            if (victim.type == ChaosEvent.TargetType.BUNDLE) {
                supervisor.getAgent().stop(Long.parseLong(victim.id));
            } else {
                supervisor.getAgent().disableComponentById(Long.parseLong(victim.id));
            }
            victimLedger.put(victim.id, event);
            offlineTargets.put(victim.name, victim.type);
            log("💥", "Disrupted " + victim.type + ": " + victim.name, victim.name);
        } catch (final Exception e) {
            log("❌", "Failed to disrupt " + victim.name + ": " + e.getMessage(), victim.name);
        }
    }

    private void revertVictim(final ChaosEvent event) {
        try {
            if (event.type == ChaosEvent.TargetType.BUNDLE) {
                supervisor.getAgent().start(Long.parseLong(event.targetId));
            } else {
                supervisor.getAgent().enableComponentById(Long.parseLong(event.targetId));
            }
            event.isReverted = true;
            offlineTargets.remove(event.targetName);
            log("🛠️", "Reverted " + event.type + ": " + event.targetName, event.targetName);
        } catch (final Exception e) {
            log("❌", "Failed to revert " + event.targetName + ": " + e.getMessage(), event.targetName);
        }
    }

    private void revertAllVictims() {
        victimLedger.values().forEach(this::revertVictim);
        victimLedger.clear();
        offlineTargets.clear();
    }

    private void log(final String icon, final String message, final String target) {
        final ActionLog entry = new ActionLog(LocalTime.now().format(timeFormatter), icon, target, message);
        Platform.runLater(() -> logConsumer.accept(entry));
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public Map<String, ChaosEvent.TargetType> getOfflineTargets() {
        return offlineTargets;
    }

}
