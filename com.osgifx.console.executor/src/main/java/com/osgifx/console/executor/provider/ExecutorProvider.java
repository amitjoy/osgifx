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
package com.osgifx.console.executor.provider;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.lang3.concurrent.BasicThreadFactory.Builder;
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
    }

    @Reference
    private LoggerFactory            factory;
    private FluentLogger             logger;
    private ScheduledExecutorService executor;

    @Activate
    void activate(final Configuration config) {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));

        final var coreSize      = config.coreSize();
        final var threadFactory = new Builder().namingPattern("osgi-fx-%d").daemon(true).build();

        executor = new ScheduledThreadPoolExecutor(coreSize, threadFactory);
    }

    @Deactivate
    void deactivate() {
        final var running = executor.shutdownNow();
        if (!running.isEmpty()) {
            logger.atWarning().log("Shutting down while tasks %s are running", running);
        }
    }

    @Override
    public CompletableFuture<Void> runAsync(final Runnable command) {
        return CompletableFuture.runAsync(command, executor);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command,
                                                  final Duration initialDelay,
                                                  final Duration period) {
        return executor.scheduleAtFixedRate(command, initialDelay.toMillis(), period.toMillis(), MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command,
                                                     final Duration initialDelay,
                                                     final Duration delay) {
        return executor.scheduleWithFixedDelay(command, initialDelay.toMillis(), delay.toMillis(), MILLISECONDS);
    }

}