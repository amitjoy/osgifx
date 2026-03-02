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
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.RuntimeDTO;

import aQute.lib.json.JSONCodec;

/**
 * Admin class for creating runtime snapshots.
 *
 * @since 11.0
 */
public final class XSnapshotAdmin {

    private static final FluentLogger logger = LoggerFactory.getFluentLogger(XSnapshotAdmin.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    private final XDtoAdmin dtoAdmin;

    public XSnapshotAdmin(final XDtoAdmin dtoAdmin) {
        this.dtoAdmin = dtoAdmin;
    }

    /**
     * Creates a runtime snapshot and saves it to the specified path.
     * The snapshot is saved as JSON.
     *
     * @param outputPath the absolute path where the snapshot should be saved
     * @return the absolute path to the created snapshot file
     * @throws Exception if the snapshot creation fails
     */
    public String createSnapshot(final String outputPath) throws Exception {
        final File outputFile = new File(outputPath);
        final File parentDir = outputFile.getParentFile();

        // Create parent directory if it doesn't exist
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        // Generate filename if outputPath is a directory
        final File snapshotFile;
        if (outputFile.isDirectory() || outputPath.endsWith("/") || outputPath.endsWith("\\")) {
            final String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            final String filename = "snapshot-" + timestamp + ".json";
            snapshotFile = new File(outputFile, filename);
        } else {
            snapshotFile = outputFile;
        }

        // Create runtime snapshot
        final RuntimeDTO snapshot = dtoAdmin.runtime();

        // Serialize to JSON using JSONCodec
        final JSONCodec codec = new JSONCodec();
        final String json = codec.enc().indent("  ").put(snapshot).toString();

        try (final FileWriter writer = new FileWriter(snapshotFile)) {
            writer.write(json);
        }

        logger.atInfo().msg("Snapshot saved to: {}").arg(snapshotFile.getAbsolutePath()).log();

        return snapshotFile.getAbsolutePath();
    }
}
