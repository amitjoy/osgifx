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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.osgifx.console.executor.Executor;

@Component
public final class ExecutorProvider implements Executor {

    @Reference
    private LoggerFactory   factory;
    private FluentLogger    logger;
    private ExecutorService executor;

    @Activate
    @SuppressWarnings("preview")
    void activate() {
        logger   = FluentLogger.of(factory.createLogger(getClass().getName()));
        executor = Executors.newVirtualThreadPerTaskExecutor();
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
    public <U> CompletableFuture<U> supplyAsync(final Supplier<U> supplier) {
        checkNotNull(supplier, "Supplier cannot be null");
        return CompletableFuture.supplyAsync(supplier, executor);
    }

}