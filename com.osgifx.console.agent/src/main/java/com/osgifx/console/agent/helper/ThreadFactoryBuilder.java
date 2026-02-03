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

import static java.lang.Thread.MAX_PRIORITY;
import static java.lang.Thread.MIN_PRIORITY;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A ThreadFactory builder, providing any combination of these features:
 *
 * <ul>
 * <li>whether threads should be daemon threads
 * <li>a naming format for threads
 * <li>a priority
 * <li>the {@link ThreadGroup} the created threads should be bound to
 * </ul>
 */
public final class ThreadFactoryBuilder {

    private static final AtomicLong INSTANCE_NUMBER = new AtomicLong(-1);

    /**
     * {@link ThreadGroup} the created {@link Thread}s will bound to.
     */
    private ThreadGroup threadGroup;

    /**
     * A {@link ThreadFactory} specific prefix for the name of created
     * {@link Thread}s.
     */
    private String threadFactoryName;

    /**
     * Default thread name format
     */
    private String threadNameFormat = "thread-%d";

    /**
     * We create daemon threads by default.
     */
    private boolean daemon = true;

    /**
     * Priority created threads will be set to.
     */
    private Integer priority;

    public ThreadFactoryBuilder() {
        threadGroup = getThreadGroupToBeUsed();
    }

    public ThreadFactoryBuilder setThreadFactoryName(final String threadFactoryName) {
        this.threadFactoryName = requireNonNull(threadFactoryName);
        return this;
    }

    /**
     * Sets the naming format to use when naming threads (supports only {@code %d}
     * and its formatting variants).
     *
     * @param threadNameFormat a {@link String#format(String, Object...)}-compatible
     *            format String, which supports only {@code %d} and its
     *            formatting variants as the single parameter. This
     *            integer will be unique to the built instance of the
     *            ThreadFactory
     */
    public ThreadFactoryBuilder setThreadNameFormat(final String threadNameFormat) {
        // just testing the if it's possible to create a name for a thread using this
        // name format
        final String sampleFormattedThreadName = String.format(threadNameFormat, 7);

        if (threadNameFormat.equals(sampleFormattedThreadName)) {
            throw new IllegalArgumentException("'threadNameFormat' does not create a distinctive name");
        }

        this.threadNameFormat = threadNameFormat;
        return this;
    }

    public ThreadFactoryBuilder setDaemon(final boolean daemon) {
        this.daemon = daemon;

        return this;
    }

    public ThreadFactoryBuilder setThreadGroup(final ThreadGroup threadGroup) {
        this.threadGroup = requireNonNull(threadGroup, "'threadGroup' must not be null");
        return this;
    }

    public ThreadFactoryBuilder setPriority(final int priority) {
        if (priority < MIN_PRIORITY) {
            final String msg = String.format("Thread priority (%d) must not be smaller than MIN_PRIORITY (%d)",
                    priority, MIN_PRIORITY);
            throw new IllegalArgumentException(msg);
        }
        if (MAX_PRIORITY < priority) {
            final String msg = String.format("Thread priority (%d) must not be larger than than MAX_PRIORITY (%d)",
                    priority, MAX_PRIORITY);
            throw new IllegalArgumentException(msg);
        }
        this.priority = priority;
        return this;
    }

    public ThreadFactory build() {
        return new CustomizedThreadFactory(this);
    }

    private static ThreadGroup getThreadGroupToBeUsed() {
        final SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            return securityManager.getThreadGroup();
        }
        return Thread.currentThread().getThreadGroup();
    }

    private String getThreadFactoryName() {
        return getThreadFactoryName(threadFactoryName);
    }

    private static String getThreadFactoryName(final String factoryName) {
        if (factoryName != null && !factoryName.trim().isEmpty()) {
            return factoryName;
        }
        return createDistinctiveThreadFactoryName();
    }

    private static String createDistinctiveThreadFactoryName() {
        return "mqtt-messaging-pool" + INSTANCE_NUMBER.incrementAndGet() + "-";
    }

    private static class CustomizedThreadFactory implements ThreadFactory {
        private final AtomicLong createdThreadsCount = new AtomicLong(-1);

        private final boolean     daemon;
        private final String      threadFactoryName;
        private final String      threadNameFormat;
        private final ThreadGroup threadGroup;
        private final Integer     threadPriority;

        public CustomizedThreadFactory(final ThreadFactoryBuilder builder) {
            // in case there is no thread factory name set
            threadFactoryName = builder.getThreadFactoryName();
            threadNameFormat  = builder.threadNameFormat;
            daemon            = builder.daemon;
            threadPriority    = builder.priority;
            threadGroup       = builder.threadGroup;
        }

        @Override
        public Thread newThread(final Runnable runnable) {
            requireNonNull(runnable, "'runnable' must not be null");

            final String threadName = threadFactoryName
                    + String.format(threadNameFormat, createdThreadsCount.incrementAndGet());
            final Thread thread     = new Thread(threadGroup, runnable, threadName);

            adjustThreadPriority(thread);
            thread.setDaemon(daemon);

            return thread;
        }

        private void adjustThreadPriority(final Thread thread) {
            if (threadPriority != null && thread.getPriority() != threadPriority.intValue()) {
                thread.setPriority(threadPriority);
            }
        }

        @Override
        public String toString() {
            // @formatter:off
            return getClass().getSimpleName() +
                    "(name:" + threadFactoryName +
                    ",created threads:" + (createdThreadsCount.get() + 1) + ")";
            // @formatter:on
        }
    }

}
