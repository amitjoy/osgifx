/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.log;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Interface regarding application diagnostics (log files)
 */
@ProviderType
public interface DiagnosticsAdmin {

    /**
     * Returns the directory where all the application log files directory
     *
     * @return the directory of the application log files
     */
    File getLogFilesDirectory();

    /**
     * Retrieves the list of persisted log files ordered by modified time in
     * descending order and file name as secondary criteria.
     *
     * @return Non-null read-only collection of log files ordered by most recent one
     *         first and file name
     */
    Collection<File> getLogFiles();

    /**
     * Fills the given output-stream with content of a requested log file. If
     * unsuccessful, does not write to the stream.
     *
     * @param logFileName  file name of a log file without path, with extension
     * @param outputstream stream where content of the requested log file is written
     *                     to
     */
    void getLogFileContent(final String logFileName, final OutputStream outputstream);

    /**
     * Immediately empty the buffered log messages into the log file. In case of
     * error a warning is logged.
     */
    void flush();

    /**
     * Returns the number of discarded log entries since service start. Log entries
     * are discarded if the outgoing queue is full.
     *
     * @return the number of discarded log entries since service start
     */
    long getDiscardedLogEntriesCount();

    /**
     * Returns the current size of the outgoing queue
     *
     * @return The current size of the outgoing queue
     */
    long getQueueSize();

    /**
     * Checks if the specified file is a compressed archive
     *
     * @param fileName the file name to check
     * @return {@code true} if the specified file is a compressed file, otherwise
     *         {@code false}
     */
    boolean isCompressedFile(String fileName);
}
