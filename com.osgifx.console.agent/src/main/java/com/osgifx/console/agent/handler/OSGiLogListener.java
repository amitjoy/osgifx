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
package com.osgifx.console.agent.handler;

import static com.osgifx.console.agent.provider.AgentServer.PROPERTY_ENABLE_LOGGING;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

import com.osgifx.console.agent.admin.XBundleAdmin;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.agent.provider.BinaryLogBuffer;
import com.osgifx.console.agent.provider.BundleStartTimeCalculator;
import com.osgifx.console.supervisor.Supervisor;

import aQute.bnd.exceptions.Exceptions;
import jakarta.inject.Inject;

public final class OSGiLogListener implements LogListener {

    // --- Optimized Reflection Handles (Init Once, Use Forever) ---
    private static final MethodHandle GET_THREAD_INFO;
    private static final MethodHandle GET_LOGGER_NAME;

    static {
        final MethodHandles.Lookup lookup        = MethodHandles.publicLookup();
        MethodHandle               getThreadInfo = null;
        MethodHandle               getLoggerName = null;
        try {
            getThreadInfo = lookup.findVirtual(LogEntry.class, "getThreadInfo", MethodType.methodType(String.class));
            getLoggerName = lookup.findVirtual(LogEntry.class, "getLoggerName", MethodType.methodType(String.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // Methods not present (Older OSGi R6 environment), handles remain null
        }
        GET_THREAD_INFO = getThreadInfo;
        GET_LOGGER_NAME = getLoggerName;
    }

    private final BundleStartTimeCalculator bundleStartTimeCalculator;
    private BinaryLogBuffer                 logBuffer;
    private Supervisor                      supervisor;

    @Inject
    public OSGiLogListener(final BundleStartTimeCalculator bundleStartTimeCalculator) {
        this.bundleStartTimeCalculator = bundleStartTimeCalculator;
    }

    public void setSupervisor(final Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public void setLogBuffer(final BinaryLogBuffer logBuffer) {
        this.logBuffer = logBuffer;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void logged(final LogEntry entry) {
        if (logBuffer != null) {
            final String exception = entry.getException() == null ? null : Exceptions.toString(entry.getException());
            logBuffer.write(entry.getTime(), entry.getBundle().getBundleId(), entry.getLevel(), entry.getMessage(),
                    exception);
        }
        if (supervisor != null) {
            final boolean isLoggingEnabled = Boolean.getBoolean(PROPERTY_ENABLE_LOGGING);
            if (isLoggingEnabled) {
                supervisor.logged(toDTO(entry));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private XLogEntryDTO toDTO(final LogEntry entry) {
        final XLogEntryDTO dto = new XLogEntryDTO();

        dto.bundle  = XBundleAdmin.toDTO(entry.getBundle(), bundleStartTimeCalculator);
        dto.message = entry.getMessage();

        // must not use OSGi R7 reference to getLogLevel()
        dto.level     = getLevel(entry.getLevel());
        dto.exception = Optional.ofNullable(entry.getException()).map(Exceptions::toString).orElse(null);
        dto.loggedAt  = entry.getTime();

        dto.threadInfo = safeInvoke(entry, GET_THREAD_INFO);
        dto.logger     = safeInvoke(entry, GET_LOGGER_NAME);

        return dto;
    }

    private String getLevel(final int level) {
        switch (level) {
            case 0:
                return "AUDIT";
            case 1:
                return "ERROR";
            case 2:
                return "WARN";
            case 3:
                return "INFO";
            case 4:
                return "DEBUG";
            case 5:
                return "TRACE";
            default:
                return "INFO";
        }
    }

    private String safeInvoke(final LogEntry entry, final MethodHandle handle) {
        if (handle == null) {
            return null;
        }
        try {
            return (String) handle.invoke(entry);
        } catch (final Throwable e) {
            return null;
        }
    }

}
