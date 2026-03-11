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
package com.osgifx.console.agent.admin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

import org.osgi.framework.BundleContext;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.helper.AgentHelper;

/**
 * Admin class for creating thread dumps.
 *
 * @since 12.0
 */
public final class XThreadDumpAdmin {

    private static final FluentLogger      logger           = LoggerFactory.getFluentLogger(XThreadDumpAdmin.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private final BundleContext context;

    public XThreadDumpAdmin(final BundleContext context) {
        this.context = context;
    }

    /**
     * Creates a thread dump, GZIP-compresses it, and saves it to the specified path.
     *
     * @param outputPath the absolute path where the thread dump should be saved
     * @return the absolute path to the created thread dump file
     * @throws Exception if the thread dump creation fails
     */
    public String createThreadDump(String outputPath) throws Exception {
        outputPath = AgentHelper.substituteVariables(outputPath, context);
        logger.atInfo().msg("Starting thread dump creation with outputPath: {}").arg(outputPath).log();

        final File outputFile = new File(outputPath);
        final File parentDir  = outputFile.getParentFile();

        // Create parent directory if it doesn't exist
        if (parentDir != null && !parentDir.exists()) {
            logger.atInfo().msg("Creating parent directory: {}").arg(parentDir.getAbsolutePath()).log();
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        // Generate filename if outputPath is a directory
        final File dumpFile;
        if (outputFile.isDirectory() || outputPath.endsWith("/") || outputPath.endsWith("\\")) {
            final String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            final String filename  = "threaddump-" + timestamp + ".tdump.gz";
            dumpFile = new File(outputFile, filename);
            logger.atInfo().msg("Generated thread dump filename: {}").arg(dumpFile.getAbsolutePath()).log();
        } else {
            dumpFile = outputFile;
        }

        // Delete existing file if it exists
        if (dumpFile.exists() && !dumpFile.delete()) {
            logger.atWarn().msg("Failed to delete existing thread dump file: {}").arg(dumpFile.getAbsolutePath()).log();
        }

        // Generate and compress the thread dump
        final byte[] dumpText = generateThreadDumpBytes();
        logger.atInfo().msg("Thread dump generated: {} bytes (uncompressed)").arg(dumpText.length).log();

        try (final FileOutputStream fos = new FileOutputStream(dumpFile);
                final GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            gzos.write(dumpText);
        }

        logger.atInfo().msg("Thread dump saved to: {}").arg(dumpFile.getAbsolutePath()).log();
        logger.atDebug().msg("Compressed thread dump size: {} bytes").arg(dumpFile.length()).log();

        return dumpFile.getAbsolutePath();
    }

    /**
     * Creates a GZIP-compressed thread dump and returns it as a byte array.
     *
     * @return GZIP-compressed thread dump bytes
     * @throws Exception if the thread dump creation fails
     */
    public byte[] threadDump() throws Exception {
        final byte[] dumpText = generateThreadDumpBytes();
        logger.atInfo().msg("Thread dump generated: {} bytes (uncompressed)").arg(dumpText.length).log();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(dumpText);
        }
        return baos.toByteArray();
    }

    /**
     * Estimates the uncompressed thread dump size based on the current thread count.
     * <p>
     * Uses a conservative per-thread budget: ~500 bytes for name/state/header
     * plus ~150 bytes per average stack frame (assuming ~20 frames per thread).
     *
     * @return estimated uncompressed size in bytes
     */
    public long estimateThreadDumpSize() {
        final int threadCount        = Thread.activeCount();
        final int avgFramesPerThread = 20;
        final int bytesPerFrame      = 150;
        final int bytesPerThreadBase = 500;
        return (long) threadCount * (bytesPerThreadBase + (long) avgFramesPerThread * bytesPerFrame);
    }

    private byte[] generateThreadDumpBytes() {
        final ThreadMXBean  threadMXBean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[]  threadInfos  = threadMXBean.dumpAllThreads(true, true);
        final long[]        deadlocks    = threadMXBean.findDeadlockedThreads();
        final StringBuilder sb           = new StringBuilder(4096);

        // Build a map of id -> Thread for daemon/priority lookup
        final java.util.Map<Long, Thread> threadMap = new java.util.HashMap<>();
        for (final Thread t : Thread.getAllStackTraces().keySet()) {
            threadMap.put(t.getId(), t);
        }

        sb.append("Thread Dump \u2014 ").append(LocalDateTime.now()).append('\n');
        sb.append("Total threads: ").append(threadInfos.length).append('\n');
        if (deadlocks != null && deadlocks.length > 0) {
            sb.append("*** DEADLOCK DETECTED \u2014 ").append(deadlocks.length).append(" thread(s) involved ***\n");
        }
        sb.append('\n');

        for (final ThreadInfo info : threadInfos) {
            appendThreadInfo(sb, info, deadlocks, threadMap.get(info.getThreadId()));
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private void appendThreadInfo(final StringBuilder sb,
                                  final ThreadInfo info,
                                  final long[] deadlocks,
                                  final Thread liveThread) {
        sb.append('"').append(info.getThreadName()).append('"');
        sb.append(" #").append(info.getThreadId());
        if (liveThread != null) {
            sb.append(" prio=").append(liveThread.getPriority());
            if (liveThread.isDaemon()) {
                sb.append(" daemon");
            }
        }
        sb.append(" tid=").append(info.getThreadId());
        if (isDeadlocked(info.getThreadId(), deadlocks)) {
            sb.append(" *** DEADLOCKED ***");
        }
        sb.append('\n');
        sb.append("   java.lang.Thread.State: ").append(info.getThreadState()).append('\n');

        if (info.getLockName() != null) {
            sb.append("   - waiting on <").append(info.getLockName()).append('>');
            if (info.getLockOwnerName() != null) {
                sb.append(" owned by \"").append(info.getLockOwnerName()).append("\" #").append(info.getLockOwnerId());
            }
            sb.append('\n');
        }

        for (final StackTraceElement element : info.getStackTrace()) {
            sb.append("\tat ").append(element).append('\n');
        }

        sb.append('\n');
    }

    private boolean isDeadlocked(final long id, final long[] deadlocks) {
        if (deadlocks == null) {
            return false;
        }
        for (final long deadlockedId : deadlocks) {
            if (deadlockedId == id) {
                return true;
            }
        }
        return false;
    }

}
