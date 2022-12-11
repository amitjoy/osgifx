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
package com.osgifx.console.agent.admin;

import static java.util.stream.Collectors.toList;
import static org.apache.felix.hc.api.HealthCheck.ASYNC_CRON_EXPRESSION;
import static org.apache.felix.hc.api.HealthCheck.ASYNC_INTERVAL_IN_SEC;
import static org.apache.felix.hc.api.HealthCheck.KEEP_NON_OK_RESULTS_STICKY_FOR_SEC;
import static org.apache.felix.hc.api.HealthCheck.MBEAN_NAME;
import static org.apache.felix.hc.api.HealthCheck.NAME;
import static org.apache.felix.hc.api.HealthCheck.RESULT_CACHE_TTL_IN_MS;
import static org.apache.felix.hc.api.HealthCheck.TAGS;
import static org.osgi.framework.Constants.SERVICE_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO.ResultDTO;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import jakarta.inject.Inject;

public final class XHcAdmin {

    private final BundleContext       context;
    private final HealthCheckExecutor felixHcExecutor;

    @Inject
    public XHcAdmin(final BundleContext context, final Object felixHcExecutor) {
        this.context         = context;
        this.felixHcExecutor = (HealthCheckExecutor) felixHcExecutor;
    }

    public List<XHealthCheckDTO> getHealthchecks() {
        if (context == null) {
            return Collections.emptyList();
        }
        try {
            return findAllHealthChecks();
        } catch (final Exception e) {
            return Collections.emptyList();
        }
    }

    public List<XHealthCheckResultDTO> executeHealthChecks(List<String> tags, List<String> names) {
        if (felixHcExecutor == null) {
            return Collections.emptyList();
        }
        HealthCheckSelector selector;
        if (tags == null) {
            tags = Collections.emptyList();
        }
        if (names == null) {
            names = Collections.emptyList();
        }
        selector = HealthCheckSelector.tags(tags.toArray(new String[0]));
        selector.withNames(names.toArray(new String[0]));

        final HealthCheckExecutionOptions options = new HealthCheckExecutionOptions();
        options.setCombineTagsWithOr(true);

        final List<HealthCheckExecutionResult> results = felixHcExecutor.execute(selector, options);
        return results.stream().map(this::toResultDTO).collect(toList());
    }

    private List<XHealthCheckDTO> findAllHealthChecks() throws InvalidSyntaxException {
        final Collection<ServiceReference<HealthCheck>> refs = context.getServiceReferences(HealthCheck.class, null);
        return refs.stream().map(this::toDTO).collect(toList());
    }

    private XHealthCheckDTO toDTO(final ServiceReference<HealthCheck> reference) {
        final XHealthCheckDTO dto = new XHealthCheckDTO();

        // @formatter:off
        dto.serviceID              = Long.parseLong(reference.getProperty(SERVICE_ID).toString());
        dto.name                   = Optional.ofNullable(reference.getProperty(NAME))
                                             .map(Object::toString)
                                             .orElse(null);
        dto.tags                   = Optional.ofNullable(reference.getProperty(TAGS))
                                             .map(e -> cnv(new TypeReference<List<String>>() {}, e))
                                             .orElse(null);
        dto.mbeanName              = Optional.ofNullable(reference.getProperty(MBEAN_NAME))
                                             .map(Object::toString)
                                             .orElse(null);
        dto.cronExpression         = Optional.ofNullable(reference.getProperty(ASYNC_CRON_EXPRESSION))
                                             .map(Object::toString)
                                             .orElse(null);
        dto.interval               = Optional.ofNullable(reference.getProperty(ASYNC_INTERVAL_IN_SEC))
                                             .map(e -> cnv(Long.class, e))
                                             .orElse(null);
        dto.resultTTL              = Optional.ofNullable(reference.getProperty(RESULT_CACHE_TTL_IN_MS))
                                             .map(e -> cnv(Long.class, e))
                                             .orElse(null);
        dto.keepNonOkResultsSticky = Optional.ofNullable(reference.getProperty(KEEP_NON_OK_RESULTS_STICKY_FOR_SEC))
                                             .map(e -> cnv(Long.class, e))
                                             .orElse(null);
        // @formatter:on

        return dto;
    }

    private static <T> T cnv(final TypeReference<T> tr, final Object source) {
        try {
            return Converter.cnv(tr, source);
        } catch (final Exception e) {
            return null;
        }
    }

    private static <T> T cnv(final Class<T> tr, final Object source) {
        try {
            return Converter.cnv(tr, source);
        } catch (final Exception e) {
            return null;
        }
    }

    private XHealthCheckResultDTO toResultDTO(final HealthCheckExecutionResult result) {
        final XHealthCheckResultDTO dto = new XHealthCheckResultDTO();

        dto.healthCheckName = result.getHealthCheckMetadata().getName();
        dto.healthCheckTags = result.getHealthCheckMetadata().getTags();
        dto.elapsedTime     = result.getElapsedTimeInMs();
        dto.finishedAt      = result.getFinishedAt().getTime();
        dto.isTimedOut      = result.hasTimedOut();
        dto.results         = initResults(result.getHealthCheckResult());

        return dto;
    }

    private List<ResultDTO> initResults(final Result result) {
        final List<ResultDTO> results = new ArrayList<>();
        result.forEach(r -> {
            final ResultDTO res = toResult(r);
            results.add(res);
        });
        return results;
    }

    private ResultDTO toResult(final ResultLog.Entry resultEntry) {
        final ResultDTO dto = new ResultDTO();

        dto.status   = resultEntry.getStatus().name();
        dto.message  = resultEntry.getMessage();
        dto.logLevel = resultEntry.getLogLevel();

        final Exception exception = resultEntry.getException();
        dto.exception = exception == null ? null : Exceptions.toString(exception);

        return dto;

    }

}
