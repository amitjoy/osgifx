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
package com.osgifx.console.executor.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.lang3.concurrent.BasicThreadFactory.Builder;
import org.apache.commons.lang3.time.DurationUtils;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.osgifx.console.executor.Executor;
import com.osgifx.console.executor.provider.ExecutorProvider.Configuration;

@Component
@Designate(ocd = Configuration.class)
public final class ExecutorProvider implements Executor {

    @ObjectClassDefinition(name = "Executor Configuration")
    public @interface Configuration {
        @AttributeDefinition(description = "The minimum number of threads allocated to this pool", required = false)
        int coreSize() default 30;

        @AttributeDefinition(description = " If this flag is set to true the containing thread pool will use daemon threads.", required = false)
        boolean daemon() default true;
    }

    @Reference
    private LoggerFactory            factory;
    private FluentLogger             logger;
    private ScheduledExecutorService executor;

    @Activate
    void activate(final Configuration config) {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));

        final var coreSize      = config.coreSize();
        final var threadFactory = new Builder().namingPattern("osgifx-%d").daemon(config.daemon()).build();

        executor = new ScheduledThreadPoolExecutor(coreSize, threadFactory);
    }

    @Deactivate
    void deactivate() {
        final var running = executor.shutdownNow();
        if (!running.isEmpty()) {
            logger.atWarning().log("Shutting down while %s tasks are running", running.size());
        }
    }

    @Override
    public CompletableFuture<Void> runAsync(final Runnable command) {
        checkNotNull(command, "Task cannot be null");
        return CompletableFuture.runAsync(command, executor);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command,
                                                  final Duration initialDelay,
                                                  final Duration period) {
        checkNotNull(command, "Task cannot be null");
        checkNotNull(initialDelay, "The initial delay cannot be null");
        checkNotNull(period, "The period cannot be null");
        checkArgument(!initialDelay.isNegative(), "The initial delay must not be negative");
        checkArgument(DurationUtils.isPositive(period), "The period must be positive and more than zero");

        return executor.scheduleAtFixedRate(command, initialDelay.toMillis(), period.toMillis(), MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command,
                                                     final Duration initialDelay,
                                                     final Duration delay) {
        checkNotNull(command, "Task cannot be null");
        checkNotNull(initialDelay, "The initial delay cannot be null");
        checkNotNull(delay, "The delay cannot be null");
        checkArgument(!initialDelay.isNegative(), "The initial delay must not be negative");
        checkArgument(DurationUtils.isPositive(delay), "The delay must be positive and more than zero");

        return executor.scheduleWithFixedDelay(command, initialDelay.toMillis(), delay.toMillis(), MILLISECONDS);
    }

}