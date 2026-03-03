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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

import javax.management.MBeanServer;

import org.osgi.framework.BundleContext;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.helper.AgentHelper;

/**
 * Admin class for creating heap dumps.
 *
 * @since 11.0
 */
public final class XHeapDumpAdmin {

    private static final FluentLogger      logger           = LoggerFactory.getFluentLogger(XHeapDumpAdmin.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private final BundleContext context;

    public XHeapDumpAdmin(final BundleContext context) {
        this.context = context;
    }

    /**
     * Creates a heap dump and saves it to the specified path.
     * The heap dump is compressed with GZIP.
     *
     * @param outputPath the absolute path where the heap dump should be saved
     * @return the absolute path to the created heap dump file
     * @throws Exception if the heap dump creation fails
     */
    public String createHeapdump(String outputPath) throws Exception {
        outputPath = AgentHelper.substituteVariables(outputPath, context);
        logger.atInfo().msg("Starting heapdump creation with outputPath: {}").arg(outputPath).log();

        final File outputFile = new File(outputPath);
        logger.atDebug().msg("Output file object created: {}").arg(outputFile.getAbsolutePath()).log();
        logger.atDebug().msg("Output file exists: {}, isDirectory: {}, isFile: {}").arg(outputFile.exists())
                .arg(outputFile.isDirectory()).arg(outputFile.isFile()).log();

        final File parentDir = outputFile.getParentFile();
        logger.atDebug().msg("Parent directory: {}").arg(parentDir != null ? parentDir.getAbsolutePath() : "null")
                .log();

        // Create parent directory if it doesn't exist
        if (parentDir != null && !parentDir.exists()) {
            logger.atInfo().msg("Creating parent directory: {}").arg(parentDir.getAbsolutePath()).log();
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
            logger.atInfo().msg("Parent directory created successfully").log();
        }

        // Generate filename if outputPath is a directory
        final File heapdumpFile;
        if (outputFile.isDirectory() || outputPath.endsWith("/") || outputPath.endsWith("\\")) {
            logger.atInfo().msg("Output path is a directory, generating filename").log();
            final String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            final String filename  = "heapdump-" + timestamp + ".hprof.gz";
            heapdumpFile = new File(outputFile, filename);
            logger.atInfo().msg("Generated heapdump filename: {}").arg(heapdumpFile.getAbsolutePath()).log();
        } else {
            logger.atInfo().msg("Output path is a file path, using as-is").log();
            heapdumpFile = outputFile;
        }

        logger.atInfo().msg("Final heapdump file path: {}").arg(heapdumpFile.getAbsolutePath()).log();
        logger.atDebug().msg("Heapdump file exists before deletion: {}").arg(heapdumpFile.exists()).log();

        // Delete existing file if it exists to prevent "File exists" error
        if (heapdumpFile.exists()) {
            logger.atInfo().msg("Deleting existing heapdump file: {}").arg(heapdumpFile.getAbsolutePath()).log();
            if (!heapdumpFile.delete()) {
                logger.atWarn().msg("Failed to delete existing heapdump file: {}").arg(heapdumpFile.getAbsolutePath())
                        .log();
            } else {
                logger.atInfo().msg("Successfully deleted existing heapdump file").log();
            }
        }

        // Create temporary uncompressed heap dump
        logger.atInfo().msg("Creating temporary heap dump file").log();
        final File   tempHprof = File.createTempFile("heapdump-", ".hprof");
        final String tempPath  = tempHprof.getAbsolutePath();
        logger.atInfo().msg("Temporary heap dump file created: {}").arg(tempPath).log();

        // Delete the temp file immediately - HotSpot's dumpHeap requires the file to NOT exist
        if (!tempHprof.delete()) {
            logger.atWarn().msg("Failed to delete temp file before heap dump").log();
        } else {
            logger.atInfo().msg("Deleted temp file so HotSpot can create it fresh").log();
        }

        try {
            // Use HotSpot diagnostic MBean to create heap dump
            final MBeanServer server       = ManagementFactory.getPlatformMBeanServer();
            final Object      hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(server,
                    "com.sun.management:type=HotSpotDiagnostic",
                    Class.forName("com.sun.management.HotSpotDiagnosticMXBean"));

            final Method dumpHeap = hotspotMBean.getClass().getMethod("dumpHeap", String.class, boolean.class);
            logger.atInfo().msg("Invoking HotSpot dumpHeap method to: {}").arg(tempPath).log();
            dumpHeap.invoke(hotspotMBean, tempPath, true); // true = live objects only

            // Recreate File object since we deleted it before dumpHeap
            final File createdHprof = new File(tempPath);
            logger.atInfo().msg("Heap dump created at: {}").arg(createdHprof.getAbsolutePath()).log();
            logger.atDebug().msg("Temp heap dump size: {} bytes").arg(createdHprof.length()).log();

            // Compress the heap dump
            logger.atInfo().msg("Starting compression from {} to {}").arg(createdHprof.getAbsolutePath())
                    .arg(heapdumpFile.getAbsolutePath()).log();
            compressFile(createdHprof, heapdumpFile);

            logger.atInfo().msg("Compressed heap dump saved to: {}").arg(heapdumpFile.getAbsolutePath()).log();
            logger.atDebug().msg("Compressed heap dump size: {} bytes").arg(heapdumpFile.length()).log();

            return heapdumpFile.getAbsolutePath();
        } finally {
            // Clean up temporary file
            final File tempFile = new File(tempPath);
            if (tempFile.exists()) {
                logger.atDebug().msg("Cleaning up temporary heap dump file: {}").arg(tempFile.getAbsolutePath()).log();
                tempFile.delete();
            }
        }
    }

    private void compressFile(final File source, final File destination) throws IOException {
        logger.atDebug().msg("Compression: source exists={}, destination exists={}").arg(source.exists())
                .arg(destination.exists()).log();
        logger.atDebug().msg("Compression: source path={}, destination path={}").arg(source.getAbsolutePath())
                .arg(destination.getAbsolutePath()).log();

        if (destination.exists()) {
            logger.atWarn().msg("Destination file already exists before compression: {}")
                    .arg(destination.getAbsolutePath()).log();
        }

        try (final FileInputStream fis = new FileInputStream(source);
                final FileOutputStream fos = new FileOutputStream(destination);
                final GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            logger.atDebug().msg("Compression streams opened successfully").log();
            final byte[] buffer     = new byte[8192];
            int          len;
            long         totalBytes = 0;
            while ((len = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
                totalBytes += len;
            }
            logger.atInfo().msg("Compression completed: {} bytes read from source").arg(totalBytes).log();
        } catch (final IOException e) {
            logger.atError().msg("Compression failed: {}").arg(e.getMessage()).throwable(e).log();
            throw e;
        }
    }
}
