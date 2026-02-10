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
package com.osgifx.console.executor.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Supplier;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
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
        @AttributeDefinition(description = "The minimum number of scheduler threads (default 2 for virtual thread architecture)", required = false)
        int coreSize() default 2;

        @AttributeDefinition(description = "If this flag is set to true the containing thread pool will use daemon threads.", required = false)
        boolean daemon() default true;
    }

    @Reference
    private LoggerFactory               factory;
    private FluentLogger                logger;
    private ScheduledThreadPoolExecutor scheduler;       // Platform threads for scheduling
    private ExecutorService             virtualExecutor; // Virtual threads for execution

    @Activate
    void activate(final Configuration config) {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));

        // Create virtual thread executor for async operations
        final var virtualThreadFactory = Thread.ofVirtual().name("fx-virtual-worker-", 0).factory();
        virtualExecutor = Executors.newThreadPerTaskExecutor(virtualThreadFactory);

        // Create small platform thread pool for scheduling (timing-critical operations)
        final var coreSize      = config.coreSize();
        final var threadFactory = BasicThreadFactory.builder().namingPattern("fx-scheduler-%d").daemon(config.daemon())
                .build();

        scheduler = new ScheduledThreadPoolExecutor(coreSize, threadFactory);
        scheduler.setRemoveOnCancelPolicy(true);

        logger.atInfo().log("Executor initialized with virtual threads (scheduler threads: %d)", coreSize);
    }

    @Deactivate
    void deactivate() {
        // Shutdown virtual executor first
        if (virtualExecutor != null) {
            virtualExecutor.shutdown();
            try {
                if (!virtualExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    final var remaining = virtualExecutor.shutdownNow();
                    if (!remaining.isEmpty()) {
                        logger.atWarning().log("Virtual executor shut down with %s tasks remaining", remaining.size());
                    }
                }
            } catch (InterruptedException e) {
                virtualExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Shutdown scheduler
        if (scheduler != null) {
            final var running = scheduler.shutdownNow();
            if (!running.isEmpty()) {
                logger.atWarning().log("Scheduler shut down while %s tasks are running", running.size());
            }
        }
    }

    @Override
    public CompletableFuture<Void> runAsync(final Runnable command) {
        checkNotNull(command, "Task cannot be null");
        // Execute on virtual thread executor
        return CompletableFuture.runAsync(command, virtualExecutor);
    }

    @Override
    public <U> CompletableFuture<U> supplyAsync(final Supplier<U> supplier) {
        checkNotNull(supplier, "Supplier cannot be null");
        // Execute on virtual thread executor
        return CompletableFuture.supplyAsync(supplier, virtualExecutor);
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

        // Wrap command to execute on virtual thread executor
        final Runnable wrappedCommand = () -> virtualExecutor.execute(command);

        return scheduler.scheduleAtFixedRate(wrappedCommand, initialDelay.toMillis(), period.toMillis(), MILLISECONDS);
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

        // Wrap command to execute on virtual thread executor
        final Runnable wrappedCommand = () -> virtualExecutor.execute(command);

        return scheduler.scheduleWithFixedDelay(wrappedCommand, initialDelay.toMillis(), delay.toMillis(),
                MILLISECONDS);
    }

}