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
package com.osgifx.console.agent.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.Test;

public class ThreadFactoryBuilderTest {

    private final Runnable emptyRunnable = () -> {};

    @Test
    public void defaultsCreateDaemonThread() {
        ThreadFactory factory = new ThreadFactoryBuilder().build();
        Thread thread = factory.newThread(emptyRunnable);
        assertTrue(thread.isDaemon());
    }

    @Test
    public void setDaemonFalseCreatesNonDaemon() {
        ThreadFactory factory = new ThreadFactoryBuilder().setDaemon(false).build();
        Thread thread = factory.newThread(emptyRunnable);
        assertFalse(thread.isDaemon());
    }

    @Test
    public void threadNameFormatIsApplied() {
        ThreadFactory factory = new ThreadFactoryBuilder().setThreadNameFormat("worker-%d").build();
        Thread thread = factory.newThread(emptyRunnable);
        assertTrue(thread.getName().contains("worker-0"));
    }

    @Test
    public void threadFactoryNameIsApplied() {
        ThreadFactory factory = new ThreadFactoryBuilder().setThreadFactoryName("pool-A-").build();
        Thread thread = factory.newThread(emptyRunnable);
        assertTrue(thread.getName().startsWith("pool-A-"));
    }

    @Test
    public void priorityIsSetOnThread() {
        ThreadFactory factory = new ThreadFactoryBuilder().setPriority(3).build();
        Thread thread = factory.newThread(emptyRunnable);
        assertEquals(3, thread.getPriority());
    }

    @Test
    public void priorityBelowMinThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ThreadFactoryBuilder().setPriority(Thread.MIN_PRIORITY - 1);
        });
    }

    @Test
    public void priorityAboveMaxThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ThreadFactoryBuilder().setPriority(Thread.MAX_PRIORITY + 1);
        });
    }

    @Test
    public void nonDistinctNameFormatThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ThreadFactoryBuilder().setThreadNameFormat("no-number");
        });
    }

    @Test
    public void nullRunnableThrows() {
        ThreadFactory factory = new ThreadFactoryBuilder().build();
        assertThrows(NullPointerException.class, () -> {
            factory.newThread(null);
        });
    }

    @Test
    public void threadGroupIsApplied() {
        ThreadGroup group = new ThreadGroup("custom-group");
        ThreadFactory factory = new ThreadFactoryBuilder().setThreadGroup(group).build();
        Thread thread = factory.newThread(emptyRunnable);
        assertEquals(group, thread.getThreadGroup());
    }
}
