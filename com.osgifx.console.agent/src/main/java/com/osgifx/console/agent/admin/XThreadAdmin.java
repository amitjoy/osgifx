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

import static java.util.stream.Collectors.toList;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.agent.provider.PackageWirings;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class XThreadAdmin extends AbstractSnapshotAdmin<XThreadDTO> {

    private final PackageWirings wirings;

    @Inject
    public XThreadAdmin(final PackageWirings wirings,
                        final BinaryCodec codec,
                        final SnapshotDecoder decoder,
                        final ScheduledExecutorService executor) {
        super(codec, decoder, executor);
        this.wirings = wirings;
    }

    public void init() {
        // No background polling needed for threads; cheap to get live.
    }

    @Override
    public byte[] snapshot() {
        return liveSnapshot();
    }

    @Override
    public List<XThreadDTO> get() {
        final byte[] current = snapshot();
        if (current == null || current.length == 0) {
            return Collections.emptyList();
        }
        try {
            return decoder.decodeList(current, XThreadDTO.class);
        } catch (final Exception e) {
            logger.atError().msg("Failed to decode thread snapshot").throwable(e).log();
            return Collections.emptyList();
        }
    }

    @Override
    protected List<XThreadDTO> map() throws Exception {
        final Map<Thread, StackTraceElement[]> threads    = Thread.getAllStackTraces();
        final List<Thread>                     threadList = new ArrayList<>(threads.keySet());
        final long[]                           deadlocks  = getDeadlockedThreads();

        return threadList.stream().map(t -> toDTO(t, deadlocks)).collect(toList());
    }

    private XThreadDTO toDTO(final Thread thread, final long[] deadlocks) {
        final XThreadDTO dto = new XThreadDTO();

        dto.name          = thread.getName();
        dto.id            = thread.getId();
        dto.priority      = thread.getPriority();
        dto.state         = thread.getState().name();
        dto.isDeadlocked  = isDeadlocked(thread.getId(), deadlocks);
        dto.isInterrupted = thread.isInterrupted();
        dto.isAlive       = thread.isAlive();
        dto.isDaemon      = thread.isDaemon();

        return dto;
    }

    private boolean isDeadlocked(final long id, final long[] deadlocks) {
        if (deadlocks == null) {
            return false;
        }
        return Arrays.stream(deadlocks).anyMatch(e -> e == id);
    }

    private long[] getDeadlockedThreads() {
        final boolean isJmxWired = wirings.isJmxWired();
        if (isJmxWired) {
            final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            return bean.findDeadlockedThreads();
        }
        logger.atDebug().msg("JMX unavailable to check for deadlocked threads").log();
        return null;
    }

}
