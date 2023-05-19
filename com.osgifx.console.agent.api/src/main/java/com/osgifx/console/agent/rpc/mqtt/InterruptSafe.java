/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.rpc.mqtt;

import java.util.concurrent.Callable;

public final class InterruptSafe {

    private InterruptSafe() {
        throw new IllegalAccessError("Non-instantiable");
    }

    /**
     * Executes task with interrupt flag reset. Interrupt flag will be set again, if
     * it was set before
     */
    public static <V> V execute(final Callable<V> task) {
        boolean   interrupted  = false;
        final int currPriority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(Math.min(currPriority + 1, Thread.MAX_PRIORITY));

        try {
            Thread.sleep(0);
        } catch (final InterruptedException e) { // NOSONAR
            interrupted = true;
        }

        try {
            return task.call();
        } catch (final Exception e) {
            return null;
        } finally {
            Thread.currentThread().setPriority(currPriority);
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Executes task with interrupt flag reset. Interrupt flag will be set again, if
     * it was set before
     */
    public static void execute(final Runnable task) {
        execute(() -> {
            task.run();
            return null;
        });
    }
}