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

import java.util.Optional;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

import com.osgifx.console.agent.admin.XBundleAdmin;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.helper.Reflect;
import com.osgifx.console.agent.provider.BundleStartTimeCalculator;
import com.osgifx.console.supervisor.Supervisor;

import aQute.bnd.exceptions.Exceptions;
import jakarta.inject.Inject;

public final class OSGiLogListener implements LogListener {

    private final Supervisor                supervisor;
    private final BundleStartTimeCalculator bundleStartTimeCalculator;

    @Inject
    public OSGiLogListener(final Supervisor supervisor, final BundleStartTimeCalculator bundleStartTimeCalculator) {
        this.supervisor                = supervisor;
        this.bundleStartTimeCalculator = bundleStartTimeCalculator;
    }

    @Override
    public void logged(final LogEntry entry) {
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

        final XResultDTO threadInfoResult = executeR7method(entry, "getThreadInfo");
        final int        resultThreadInfo = threadInfoResult.result;

        if (resultThreadInfo == XResultDTO.SUCCESS) {
            dto.threadInfo = threadInfoResult.response;
        }

        final XResultDTO loggerNameResult = executeR7method(entry, "getLoggerName");
        final int        resultLoggerName = loggerNameResult.result;

        if (resultLoggerName == XResultDTO.SUCCESS) {
            dto.logger = threadInfoResult.response;
        }

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

    private XResultDTO executeR7method(final Object object, final String methodName) {
        final XResultDTO dto = new XResultDTO();
        try {
            dto.response = Reflect.on(object).call(methodName).get();
            dto.result   = XResultDTO.SUCCESS;
        } catch (final Exception e) {
            dto.result = XResultDTO.ERROR;
        }
        return dto;
    }

}
