/*******************************************************************************
 * COPYRIGHT 2021-2024 AMIT KUMAR MONDAL
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.executor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Defines an executor for running submitted {@link Runnable} tasks. This interface decouples task submission from the 
 * mechanics of how each task will be executed, including thread usage, scheduling, and other details. An {@code Executor} 
 * is typically used instead of directly managing threads.
 */
@ProviderType
public interface Executor {

    /**
     * Executes the specified command at some point in the future. The command may execute in a new thread, a pooled thread, 
     * or in the calling thread, depending on the {@code Executor} implementation.
     *
     * @param command the runnable task to be executed
     * @return a CompletableFuture representing the pending completion of the task
     * @throws RejectedExecutionException if the task cannot be accepted for execution
     * @throws NullPointerException if the command is null
     */
    CompletableFuture<Void> runAsync(Runnable command);

    /**
     * Returns a new CompletableFuture that is asynchronously completed by a task running in the {@link ForkJoinPool#commonPool()} 
     * with the value obtained by calling the given Supplier.
     *
     * @param <U> the function's return type
     * @param supplier a function returning the value to be used to complete the returned CompletableFuture
     * @return the new CompletableFuture
     */
    <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier);

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently 
     * with the given period. Executions commence after {@code initialDelay} then {@code initialDelay + period}, 
     * {@code initialDelay + 2 * period}, and so on. If any execution of the task encounters an exception, subsequent executions 
     * are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor. If any execution 
     * of this task takes longer than its period, subsequent executions may start late but will not execute concurrently.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @return a ScheduledFuture representing pending completion of the task, and whose {@code get()} method will throw an exception upon cancellation
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if any of the arguments is null
     * @throws IllegalArgumentException if the period is less than or equal to zero
     */
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, Duration initialDelay, Duration period);

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently 
     * with the given delay between the termination of one execution and the commencement of the next. If any execution 
     * of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate 
     * via cancellation or termination of the executor.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one execution and the commencement of the next
     * @return a ScheduledFuture representing pending completion of the task, and whose {@code get()} method will throw an exception upon cancellation
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException if any of the arguments is null
     * @throws IllegalArgumentException if the delay is less than or equal to zero
     */
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, Duration initialDelay, Duration delay);
}
