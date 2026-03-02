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
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

import javax.management.MBeanServer;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;

/**
 * Admin class for creating heap dumps.
 *
 * @since 11.0
 */
public final class XHeapDumpAdmin {

    private static final FluentLogger logger = LoggerFactory.getFluentLogger(XHeapDumpAdmin.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    /**
     * Creates a heap dump and saves it to the specified path.
     * The heap dump is compressed with GZIP.
     *
     * @param outputPath the absolute path where the heap dump should be saved
     * @return the absolute path to the created heap dump file
     * @throws Exception if the heap dump creation fails
     */
    public String createHeapdump(final String outputPath) throws Exception {
        final File outputFile = new File(outputPath);
        final File parentDir = outputFile.getParentFile();

        // Create parent directory if it doesn't exist
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        // Generate filename if outputPath is a directory
        final File heapdumpFile;
        if (outputFile.isDirectory() || outputPath.endsWith("/") || outputPath.endsWith("\\")) {
            final String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            final String filename = "heapdump-" + timestamp + ".hprof.gz";
            heapdumpFile = new File(outputFile, filename);
        } else {
            heapdumpFile = outputFile;
        }

        // Create temporary uncompressed heap dump
        final File tempHprof = File.createTempFile("heapdump-", ".hprof");
        tempHprof.deleteOnExit();

        try {
            // Use HotSpot diagnostic MBean to create heap dump
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final Object hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(
                server,
                "com.sun.management:type=HotSpotDiagnostic",
                Class.forName("com.sun.management.HotSpotDiagnosticMXBean")
            );

            final Method dumpHeap = hotspotMBean.getClass().getMethod("dumpHeap", String.class, boolean.class);
            dumpHeap.invoke(hotspotMBean, tempHprof.getAbsolutePath(), true); // true = live objects only

            logger.atInfo().msg("Heap dump created at: {}").arg(tempHprof.getAbsolutePath()).log();

            // Compress the heap dump
            compressFile(tempHprof, heapdumpFile);

            logger.atInfo().msg("Compressed heap dump saved to: {}").arg(heapdumpFile.getAbsolutePath()).log();

            return heapdumpFile.getAbsolutePath();
        } finally {
            // Clean up temporary file
            if (tempHprof.exists()) {
                tempHprof.delete();
            }
        }
    }

    private void compressFile(final File source, final File destination) throws IOException {
        try (final var fis = new java.io.FileInputStream(source);
             final var fos = new FileOutputStream(destination);
             final var gzos = new GZIPOutputStream(fos)) {

            final byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
            }
        }
    }
}
