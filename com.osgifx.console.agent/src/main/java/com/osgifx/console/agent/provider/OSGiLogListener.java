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
package com.osgifx.console.agent.provider;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.supervisor.Supervisor;

public class OSGiLogListener implements LogListener {

    private final Supervisor supervisor;

    public OSGiLogListener(final Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public void logged(final LogEntry entry) {
        if (supervisor != null) {
            supervisor.logged(toDTO(entry));
        }
    }

    private XLogEntryDTO toDTO(final LogEntry entry) {
        final XLogEntryDTO dto = new XLogEntryDTO();

        dto.bundle     = XBundleAdmin.toDTO(entry.getBundle());
        dto.message    = entry.getMessage();
        dto.level      = entry.getLogLevel().name();
        dto.exception  = toExceptionString(entry.getException());
        dto.loggedAt   = entry.getTime();
        dto.threadInfo = entry.getThreadInfo();
        dto.logger     = entry.getLoggerName();

        return dto;
    }

    private String toExceptionString(final Throwable exception) {
        if (exception == null) {
            return null;
        }
        final StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}