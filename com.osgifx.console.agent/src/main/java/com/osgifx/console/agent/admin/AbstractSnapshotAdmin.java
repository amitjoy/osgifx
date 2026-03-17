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
package com.osgifx.console.agent.admin;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.Lz4Codec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;

/**
 * Base class for all admin classes that provide binary snapshots of the agent's state.
 * This class provides a debounced, deadline-aware snapshotting mechanism to reduce
 * serialization overhead and heap occupancy.
 * <p>
 * <b>Reactive Snapshotting Strategy:</b>
 * <ul>
 * <li><b>Debounce Window:</b> 200ms of silence required before snapshot generation</li>
 * <li><b>Hard Deadline:</b> 5000ms maximum wait to prevent UI starvation</li>
 * <li><b>Change-Count Synchronization:</b> Snapshots only regenerated when state actually changes</li>
 * <li><b>Atomic Caching:</b> Compressed {@code byte[]} stored in {@link AtomicReference} for zero-copy serving</li>
 * </ul>
 *
 * @param <T> the type of DTO contained in the snapshot list
 * @since 12.0
 */
public abstract class AbstractSnapshotAdmin<T> {

    protected static final long DEBOUNCE_DELAY_MS = 200;
    protected static final long MAX_WAIT_MS       = 5000;

    protected final BinaryCodec                         codec;
    protected final SnapshotDecoder                     decoder;
    protected final ScheduledExecutorService            executor;
    protected final AtomicReference<byte[]>             snapshot      = new AtomicReference<>();
    protected final AtomicReference<List<T>>            cachedDtos    = new AtomicReference<>(Collections.emptyList());
    protected final AtomicReference<ScheduledFuture<?>> scheduledTask = new AtomicReference<>();

    protected final AtomicLong lastEventTime      = new AtomicLong(0);
    protected final AtomicLong deadline           = new AtomicLong(0);
    protected final AtomicLong lastChangeCount    = new AtomicLong(-1);
    protected final AtomicLong pendingChangeCount = new AtomicLong(0);

    protected final FluentLogger logger = LoggerFactory.getFluentLogger(getClass());

    protected AbstractSnapshotAdmin(final BinaryCodec codec,
                                    final SnapshotDecoder decoder,
                                    final ScheduledExecutorService executor) {
        this.codec    = codec;
        this.decoder  = decoder;
        this.executor = executor;
    }

    /**
     * Returns the current snapshot. If no snapshot exists or if an update is pending,
     * one is generated synchronously.
     *
     * @return the binary snapshot data
     */
    public byte[] snapshot() {
        if (snapshot.get() == null || pendingChangeCount.get() > lastChangeCount.get()) {
            performSnapshot(pendingChangeCount.get());
        }
        final byte[] current = snapshot.get();
        return current != null ? current : new byte[0];
    }

    /**
     * Schedules a snapshot update with a specific change count.
     *
     * @param changeCount the change count to associated with this update
     */
    protected void scheduleUpdate(final long changeCount) {
        final long now = System.currentTimeMillis();
        lastEventTime.set(now);
        pendingChangeCount.set(changeCount);

        // 1. Initialize Deadline if not set (CAS ensures we only set it on the FIRST event of a burst)
        deadline.compareAndSet(0, now + MAX_WAIT_MS);

        // 2. Ensure a Checker Task is running
        scheduledTask.updateAndGet(current -> {
            if (current != null && !current.isDone()) {
                return current;
            }
            return executor.schedule(this::checkAndRun, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
        });
    }

    /**
     * Checker task that decides whether to run the snapshot now or reschedule.
     */
    private void checkAndRun() {
        final long now             = System.currentTimeMillis();
        final long lastEvent       = lastEventTime.get();
        final long currentDeadline = deadline.get();

        final long silence   = now - lastEvent;
        final long remaining = currentDeadline - now;
        final long delay     = DEBOUNCE_DELAY_MS - silence;

        if (delay <= 0 || remaining <= 0) {
            performSnapshot(pendingChangeCount.get());
            // Race check: if more events came in during snapshot
            if (lastEventTime.get() > now) {
                scheduledTask.set(executor.schedule(this::checkAndRun, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS));
            }
        } else {
            final long wait = Math.max(1, Math.min(delay, remaining));
            scheduledTask.set(executor.schedule(this::checkAndRun, wait, TimeUnit.MILLISECONDS));
        }
    }

    /**
     * Generates and stores the snapshot.
     */
    protected synchronized void performSnapshot(final long changeCount) {
        // changeCount -1 means "ignore change count and snapshot anyway"
        if (changeCount != -1 && changeCount <= lastChangeCount.get()) {
            return;
        }
        try {
            final List<T> dtos = map();
            if (dtos == null) {
                return;
            }
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            final byte[] encoded    = codec.encode(dtos);
            final byte[] compressed = Lz4Codec.compressWithLength(encoded);
            this.snapshot.set(compressed);
            this.cachedDtos.set(dtos);
            this.lastChangeCount.set(changeCount);
        } catch (final Exception e) {
            logger.atError().msg("Snapshot failed for " + getClass().getSimpleName()).throwable(e).log();
        } finally {
            deadline.set(0);
        }
    }

    /**
     * Generates a snapshot on-demand without caching or scheduling.
     * Useful for "cheap" data where background tracking is overkill.
     *
     * @return the encoded and compressed snapshot
     */
    protected byte[] liveSnapshot() {
        try {
            final List<T> dtos = map();
            if (dtos == null) {
                return new byte[0];
            }
            final byte[] encoded = codec.encode(dtos);
            return Lz4Codec.compressWithLength(encoded);
        } catch (final Exception e) {
            logger.atError().msg("Live snapshot failed for " + getClass().getSimpleName()).throwable(e).log();
            return new byte[0];
        }
    }

    /**
     * Maps the current domain state to a list of DTOs.
     *
     * @return the list of DTOs
     */
    protected abstract List<T> map() throws Exception;

    /**
     * Invalidate the current snapshot and change count.
     */
    public void invalidate() {
        snapshot.set(null);
        cachedDtos.set(Collections.emptyList());
        lastChangeCount.set(-1);
    }

    /**
     * Returns the cached DTOs. If an update is pending, a new snapshot is generated synchronously.
     *
     * @return the list of DTOs
     */
    public List<T> get() {
        if (snapshot.get() == null || pendingChangeCount.get() > lastChangeCount.get()) {
            performSnapshot(pendingChangeCount.get());
        }
        return cachedDtos.get();
    }

    public void stop() {
        final ScheduledFuture<?> task = scheduledTask.get();
        if (task != null) {
            task.cancel(true);
        }
    }
}
