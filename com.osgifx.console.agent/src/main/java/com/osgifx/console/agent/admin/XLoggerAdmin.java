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
package com.osgifx.console.agent.admin;

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.agent.helper.AgentHelper.createResult;
import static com.osgifx.console.agent.helper.AgentHelper.serviceUnavailable;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.LOGGER_ADMIN;
import static java.util.stream.Collectors.toMap;
import static org.osgi.service.log.Logger.ROOT_LOGGER_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.provider.PackageWirings;

import jakarta.inject.Inject;

public final class XLoggerAdmin {

    private final BundleContext context;
    private final LoggerAdmin   loggerAdmin;
    private final boolean       isConfigAdminWired;
    private final FluentLogger  logger = LoggerFactory.getFluentLogger(getClass());

    @Inject
    public XLoggerAdmin(final Object loggerAdmin, final PackageWirings packageWirings, final BundleContext context) {
        this.context       = context;
        this.loggerAdmin   = (LoggerAdmin) loggerAdmin;
        isConfigAdminWired = packageWirings.isConfigAdminWired();
    }

    public List<XBundleLoggerContextDTO> getLoggerContexts() {
        if (loggerAdmin == null) {
            logger.atInfo().msg("Logger admin is unavailable").log();
            return Collections.emptyList();
        }
        final List<XBundleLoggerContextDTO> loggerContexts = new ArrayList<>();
        for (final Bundle bundle : context.getBundles()) {
            final String        bsn           = bundle.getSymbolicName();
            final LoggerContext loggerContext = loggerAdmin.getLoggerContext(bsn);

            final XBundleLoggerContextDTO bundleLoggerContext = new XBundleLoggerContextDTO();

            bundleLoggerContext.name         = bsn;
            bundleLoggerContext.rootLogLevel = loggerAdmin.getLoggerContext(null).getLogLevels().get(ROOT_LOGGER_NAME);
            bundleLoggerContext.logLevels    = loggerContext.getLogLevels();

            loggerContexts.add(bundleLoggerContext);
        }
        return loggerContexts;
    }

    public XResultDTO updateLoggerContext(final String bsn, final Map<String, String> logLevels) {
        if (loggerAdmin == null) {
            return createResult(SKIPPED, serviceUnavailable(LOGGER_ADMIN));
        }
        try {
            if (isConfigAdminWired) {
                return updateLoggerContextPersistently(bsn, logLevels);
            }
            return updateLoggerContextNonPersistently(bsn, logLevels);
        } catch (final Exception e) {
            return createResult(ERROR, "The logger context '" + bsn + "' could not be updated");
        }
    }

    private XResultDTO updateLoggerContextNonPersistently(final String bsn, final Map<String, String> logLevels) {
        final Map<String, LogLevel> levels = toLogLevels(logLevels);
        loggerAdmin.getLoggerContext(bsn).setLogLevels(levels);
        return createResult(SUCCESS,
                "The logger context '" + bsn + "' has been updated (non-persistently) successfully");
    }

    private XResultDTO updateLoggerContextPersistently(final String bsn,
                                                       final Map<String, String> logLevels) throws Exception {
        final Map<String, LogLevel> levels           = toLogLevels(logLevels);
        final String                pid              = "org.osgi.service.log.admin|" + bsn;
        final Map<String, Object>   configProperties = levels.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().name()));

        final ServiceReference<ConfigurationAdmin> serviceReference = context
                .getServiceReference(ConfigurationAdmin.class);
        final ConfigurationAdmin                   service          = context.getService(serviceReference);
        final Configuration                        configuration    = service.getConfiguration(pid, "?");

        configuration.update(new Hashtable<>(configProperties));
        return createResult(SUCCESS, "The logger context '" + bsn + "' has been updated (persistently) successfully");
    }

    private Map<String, LogLevel> toLogLevels(final Map<String, String> logLevels) {
        final Map<String, LogLevel> levels = new HashMap<>();
        for (final Entry<String, String> entry : logLevels.entrySet()) {
            final String key   = entry.getKey();
            final String value = entry.getValue();

            final LogLevel level = fromString(value);
            if (level != null) {
                levels.put(key, level);
            }
        }
        return levels;
    }

    private static LogLevel fromString(final String logLevel) {
        for (final LogLevel level : LogLevel.values()) {
            if (level.name().equalsIgnoreCase(logLevel)) {
                return level;
            }
        }
        return null;
    }

}
