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
package com.osgifx.console.agent.admin;

import static java.util.stream.Collectors.toList;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.agent.provider.PackageWirings;

import jakarta.inject.Inject;

public final class XThreadAdmin {

    private final PackageWirings wirings;
    private final FluentLogger   logger = LoggerFactory.getFluentLogger(getClass());

    @Inject
    public XThreadAdmin(final PackageWirings wirings) {
        this.wirings = wirings;
    }

    public List<XThreadDTO> get() {
        try {
            final Map<Thread, StackTraceElement[]> threads    = Thread.getAllStackTraces();
            final List<Thread>                     threadList = new ArrayList<>(threads.keySet());
            return threadList.stream().map(this::toDTO).collect(toList());
        } catch (final Exception e) {
            logger.atError().msg("Error occurred while retrieving threads").throwable(e).log();
            return Collections.emptyList();
        }
    }

    private XThreadDTO toDTO(final Thread thread) {
        final XThreadDTO dto = new XThreadDTO();

        dto.name          = thread.getName();
        dto.id            = thread.getId();
        dto.priority      = thread.getPriority();
        dto.state         = thread.getState().name();
        dto.isDeadlocked  = isDeadlocked(thread.getId());
        dto.isInterrupted = thread.isInterrupted();
        dto.isAlive       = thread.isAlive();
        dto.isDaemon      = thread.isDaemon();

        return dto;
    }

    private boolean isDeadlocked(final long id) {
        final boolean isJmxWired = wirings.isJmxWired();
        if (isJmxWired) {
            final ThreadMXBean bean      = ManagementFactory.getThreadMXBean();
            final long[]       deadlocks = bean.findDeadlockedThreads();
            if (deadlocks == null) {
                return false;
            }
            return Arrays.stream(deadlocks).anyMatch(e -> e == id);
        }
        logger.atDebug().msg("JMX unavailable to check if thread [id: '%s'] is deadlocked").arg(id).log();
        return false;
    }

}
