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
package com.osgifx.console.logging;

import java.util.Map;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Logger.Level;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import com.google.common.collect.Maps;

@Component
public final class FluentLoggerListener implements LogListener {

    @Reference
    private LoggerFactory                   factory;
    @Reference
    private LogReaderService                logReader;
    private final Map<String, FluentLogger> loggers = Maps.newConcurrentMap();

    @Activate
    void activate() {
        logReader.addLogListener(this);
    }

    @Override
    public void logged(final LogEntry entry) {
        final var logger = loggers.computeIfAbsent(entry.getLoggerName(),
                e -> FluentLogger.of(factory.createLogger(entry.getLoggerName())));
        log(entry, logger);
    }

    private void log(final LogEntry entry, final FluentLogger logger) {
        final var context   = logger.at(findLogLevel(entry));
        final var exception = entry.getException();
        if (exception != null) {
            context.withException(exception);
        }
        context.log(entry.getMessage());
    }

    private Level findLogLevel(final LogEntry entry) {
        return switch (entry.getLogLevel()) {
        case DEBUG -> Level.DEBUG;
        case ERROR -> Level.ERROR;
        case INFO -> Level.INFO;
        case TRACE -> Level.TRACE;
        case WARN -> Level.WARNING;
        case AUDIT -> throw new RuntimeException("AUDIT severity cannot be handled");
        default -> throw new RuntimeException("Log severity cannot be handled");
        };
    }

}
