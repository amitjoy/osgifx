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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO;
import com.osgifx.console.agent.dto.XHealthCheckResultDTO.ResultDTO;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;

public class XHcAdmin {

    private final BundleContext       context;
    private final HealthCheckExecutor felixHcExecutor;

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
        return results.stream().map(this::toResultDTO).collect(Collectors.toList());
    }

    private List<XHealthCheckDTO> findAllHealthChecks() throws InvalidSyntaxException {
        final Collection<ServiceReference<HealthCheck>> refs = context.getServiceReferences(HealthCheck.class, null);
        return refs.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private XHealthCheckDTO toDTO(final ServiceReference<HealthCheck> reference) {
        final XHealthCheckDTO dto = new XHealthCheckDTO();

        dto.serviceID              = Long.parseLong(reference.getProperty(Constants.SERVICE_ID).toString());
        dto.name                   = Optional.ofNullable(reference.getProperty("hc.name")).map(Object::toString).orElse(null);
        dto.tags                   = Optional.ofNullable(reference.getProperty("hc.tags")).map(e -> cnv(new TypeReference<List<String>>() {
                                   }, e)).orElse(null);
        dto.mbeanName              = Optional.ofNullable(reference.getProperty("hc.mbean.name")).map(Object::toString).orElse(null);
        dto.cronExpression         = Optional.ofNullable(reference.getProperty("hc.async.cronExpression")).map(Object::toString)
                .orElse(null);
        dto.interval               = Optional.ofNullable(reference.getProperty("")).map(e -> cnv(Long.class, e)).orElse(null);
        dto.resultTTL              = Optional.ofNullable(reference.getProperty("")).map(e -> cnv(Long.class, e)).orElse(null);
        dto.resultCacheTTL         = Optional.ofNullable(reference.getProperty("")).map(e -> cnv(Long.class, e)).orElse(null);
        dto.keepNonOkResultsSticky = Optional.ofNullable(reference.getProperty("")).map(e -> cnv(Long.class, e)).orElse(null);

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
