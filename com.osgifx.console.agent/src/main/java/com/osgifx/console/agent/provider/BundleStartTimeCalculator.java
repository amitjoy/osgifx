/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.agent.provider;

import static java.util.stream.Collectors.toList;
import static org.osgi.framework.BundleEvent.STARTED;
import static org.osgi.framework.BundleEvent.STARTING;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

public final class BundleStartTimeCalculator implements SynchronousBundleListener {

    public static final class BundleStartDuration {

        private final String   symbolicName;
        private final Instant  startingAt;
        private final Duration startedAfter;

        public BundleStartDuration(final String symbolicName, final Instant startingAt, final Duration startedAfter) {
            this.symbolicName = symbolicName;
            this.startingAt   = startingAt;
            this.startedAfter = startedAfter;
        }

        public String getSymbolicName() {
            return symbolicName;
        }

        public Instant getStartingAt() {
            return startingAt;
        }

        public Duration getStartedAfter() {
            return startedAfter;
        }
    }

    private final Map<Long, StartTime> bundleToStartTime = new HashMap<>();
    private final Clock                clock             = Clock.systemUTC();
    private final long                 ourBundleId;

    public BundleStartTimeCalculator(final long ourBundleId) {
        this.ourBundleId = ourBundleId;
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        final Bundle bundle = event.getBundle();

        // this bundle is already starting by the time this is invoked. We also can't
        // get proper timing from the framework bundle
        if (bundle.getBundleId() == SYSTEM_BUNDLE_ID || bundle.getBundleId() == ourBundleId) {
            return;
        }

        synchronized (bundleToStartTime) {
            switch (event.getType()) {
            case STARTING:
                bundleToStartTime.put(bundle.getBundleId(), new StartTime(bundle.getSymbolicName(), clock.millis()));
                break;
            case STARTED:
                final StartTime startTime = bundleToStartTime.get(bundle.getBundleId());
                if (startTime == null) {
                    return;
                }
                startTime.started(clock.millis());
                break;
            default:
                break;
            }
        }
    }

    public List<BundleStartDuration> getBundleStartDurations() {
        synchronized (bundleToStartTime) {
            return bundleToStartTime.values().stream().map(StartTime::toBundleStartDuration).collect(toList());
        }
    }

    static class StartTime {
        private final String bundleSymbolicName;
        private final long   startingTimestamp;
        private long         startedTimestamp;

        public StartTime(final String bundleSymbolicName, final long startingTimestamp) {
            this.bundleSymbolicName = bundleSymbolicName;
            this.startingTimestamp  = startingTimestamp;
        }

        public long getDuration() {
            return startedTimestamp - startingTimestamp;
        }

        public String getBundleSymbolicName() {
            return bundleSymbolicName;
        }

        public void started(final long startedTimestamp) {
            this.startedTimestamp = startedTimestamp;
        }

        public BundleStartDuration toBundleStartDuration() {
            return new BundleStartDuration(bundleSymbolicName, Instant.ofEpochMilli(startingTimestamp),
                    Duration.ofMillis(startedTimestamp - startingTimestamp));
        }
    }
}