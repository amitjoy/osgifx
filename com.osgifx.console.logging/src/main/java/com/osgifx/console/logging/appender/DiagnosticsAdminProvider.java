/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.logging.appender;

import static java.util.Objects.requireNonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.RequireMetaTypeExtender;

import com.osgifx.console.log.DiagnosticsAdmin;

/**
 * Implements a {@link LogListener} service that persists {@link LogEvent}s to a
 * disk file and can be configured to perform a file roll over strategy based
 * upon a maximum file size (in MiB) policy. The maximum number of persisted log
 * files can also be specified. When the file size threshold has been reached
 * for the current log file a new log file will be created to replace it. After
 * file roll over, the previous log file will be closed and archived according
 * to the maximum number of log files policy parameter.
 */
@RequireMetaTypeExtender
@Designate(ocd = Configuration.class)
@Component(immediate = true, configurationPid = Configuration.PID)
public class DiagnosticsAdminProvider implements LogListener, DiagnosticsAdmin {

    // Count of Logs to be kept in memory queue. (log count)
    private static final int MAX_QUEUE_LENGTH = 10_000;

    // The size of chars to be buffered in memory before writing to log file
    private static final int DEFAULT_CHAR_BUFFER_SIZE = 8_192;

    // Pattern also specifies a named group "nr" containing the number, e.g.
    // log<nr>.txt.gz
    private static final String LOG_FILE_NAMES_GZIP    = "log(?<nr>\\d+)\\.txt\\.gz";
    private static final String LOG_FILE_NAMES_LOG_TXT = "log\\.txt";

    // Pattern matches e.g. log1.txt.gz, Log0.txt.gz, etc.
    private static final Pattern LOG_FILE_NAMES_GZIP_PATTERN = Pattern.compile(LOG_FILE_NAMES_GZIP);
    // Pattern matches e.g. log.txt, log1.txt.gz, Log0.txt.gz, etc.
    private static final Pattern LOG_FILE_NAMES_ALL_PATTERN = Pattern
            .compile("(" + LOG_FILE_NAMES_GZIP + ")|(" + LOG_FILE_NAMES_LOG_TXT + ")");

    private static final Comparator<String> STRING_WITH_NUMBER = Comparator
            .comparing(DiagnosticsAdminProvider::getNumberInLogFileName);

    // Use a named thread
    private static final String           THREAD_NAME              = "rolling-logger";
    private final BlockingQueue<LogEntry> logEntryQueue            = new LinkedBlockingQueue<>(MAX_QUEUE_LENGTH);
    private Thread                        workerThread;
    private Writer                        outputWriter;
    private File                          root;
    private Configuration                 configuration;
    private long                          lastFlushTimeNanos       = System.nanoTime();
    private final AtomicLong              discardedLogEntriesCount = new AtomicLong();
    private StringBuilder                 formatBuilder;
    private Formatter                     formatter;

    @Activate
    private BundleContext context;

    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;

    @Reference
    private LogReaderService logReaderService;

    @Activate
    protected void activate(final Configuration configuration) {
        logReaderService.addLogListener(this);
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));

        configure(configuration);

        formatter = new Formatter(formatBuilder = new StringBuilder());

        workerThread = new Thread(this::logMessageConsumerWorker, THREAD_NAME);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Deactivate
    protected void deactivate() throws InterruptedException {
        logReaderService.removeLogListener(this);
        workerThread.interrupt();
        workerThread.join(10_000L);
    }

    /**
     * Called when the config was changed, e.g. the log level, the location, the
     * format, etc. In order to not miss any log messages, we adjust the values but
     * keep the logListener and consumer worker running.
     *
     * @param context bundle context
     * @param configuration new/changed configuration
     */
    @Modified
    protected void modified(final Configuration configuration) {
        configure(configuration);
    }

    /**
     * Listener method called for each LogEntry object created.
     *
     * @see org.osgi.service.log.LogListener#logged(org.osgi.service.log.LogEntry)
     * @param entry the LogEntry instance to log
     */
    @Override
    public void logged(final LogEntry entry) {
        if (entry == null) {
            return;
        }
        if (logEntryQueue.size() >= MAX_QUEUE_LENGTH / 2) {
            // give logging thread a chance to run
            Thread.yield();
        }
        if (isLogLevelToBeLogged(entry.getLogLevel()) && !logEntryQueue.offer(entry)) {
            discardedLogEntriesCount.incrementAndGet();
            System.err.println(String.format("%s failed to offer log entry '%s' to queue", // NOSONAR
                    DiagnosticsAdminProvider.class.getName(), entry));
        }
    }

    @Override
    public long getDiscardedLogEntriesCount() {
        return discardedLogEntriesCount.get();
    }

    @Override
    public long getQueueSize() {
        return logEntryQueue.size();
    }

    @Override
    public File getLogFilesDirectory() {
        return root;
    }

    @Override
    public Collection<File> getLogFiles() {
        requireNonNull(root, "root log files folder not properly configured!");
        return Stream.of(root.listFiles((dir, name) -> LOG_FILE_NAMES_ALL_PATTERN.matcher(name).matches()))
                .sorted(Comparator.comparingLong(File::lastModified).reversed()
                        .thenComparing(compareStringWithNumber(File::getName)))
                .toList();
    }

    @Override
    public void getLogFileContent(final String logFileName, final OutputStream outputstream) {
        requireNonNull(logFileName, "Requested log file name must not be null!");
        // Match file via requested filename
        final var requestedFileOpt = getLogFiles().stream().filter(file -> file.getName().equals(logFileName))
                .findAny();
        if (!requestedFileOpt.isPresent()) {
            logger.atError().log("Requested log file '%s' could not be found", logFileName);
            return;
        }
        try (final InputStream fis = new FileInputStream(requestedFileOpt.get())) {
            final var buffer = new byte[DEFAULT_CHAR_BUFFER_SIZE];
            for (int length; (length = fis.read(buffer)) != -1;) {
                outputstream.write(buffer, 0, length);
            }
            outputstream.flush();
        } catch (final IOException e) {
            logger.atError().log("Exception while reading log file '%s'", logFileName, e);
        }
    }

    @Override
    public void flush() {
        try {
            if (outputWriter != null) {
                outputWriter.flush();
            }
        } catch (final Exception e) {
            // This may also happen during service component shutdown when writer is already
            // closed and/or null
            logger.atWarning().log("Flushing of log file writer failed!", e.getMessage());
        } finally {
            lastFlushTimeNanos = System.nanoTime();
        }

    }

    @Override
    public boolean isCompressedFile(final String fileName) {
        return LOG_FILE_NAMES_GZIP_PATTERN.matcher(fileName).matches();
    }

    private void configure(final Configuration configuration) {
        this.configuration = configuration;
        final var rootFolder = replace(configuration.root());
        setupRootFolder(rootFolder);
    }

    private String replace(final String input) {
        final var area   = context.getProperty("osgi.instance.area.default");
        final var prefix = "file:";

        final var substring = area.substring(area.indexOf(prefix) + prefix.length(), area.length() - 1);
        final var storage   = substring.substring(substring.lastIndexOf('/') + 1);

        final Map<String, String> substitutors = Map.of("storage", storage);
        return substitutors.entrySet().stream().reduce(input,
                (s, e) -> s.replace("${" + e.getKey() + "}", e.getValue()), (s, s2) -> s);
    }

    private void setupRootFolder(final String rootFolder) {
        root = new File(rootFolder);
        root.mkdirs();
        if (!root.isDirectory()) {
            throw new IllegalStateException("Cannot create directory path " + root.getPath());
        }
        logger.atInfo().log("Created log file directory at '%s'", root.getAbsolutePath());
    }

    /**
     * Used to compare log files with respect to the number in their file name in
     * ascending order, file names without number first. E.g.
     * <code>log.txt, log0.txt.gz .. log10.txt.gz, log11.txt.gz</code>, etc.
     */
    private static final <T> Comparator<T> compareStringWithNumber(final Function<? super T, String> keyExtractor) {
        requireNonNull(keyExtractor);
        return (c1, c2) -> STRING_WITH_NUMBER.compare(keyExtractor.apply(c1), keyExtractor.apply(c2));
    }

    private boolean isLogLevelToBeLogged(final LogLevel logEntryLogLevel) {
        // If no configuration is present, log all messages
        if (configuration == null || configuration.level() == null) {
            return true;
        }
        return configuration.level() != null && configuration.level().implies(logEntryLogLevel);
    }

    /**
     * This is the log message consumer consuming the log queue, writing to log
     * files and doing rolling, runs in a separate thread
     */
    private void logMessageConsumerWorker() {
        try {
            while (true) {
                final var limit            = configuration.maxLogSizeInMB() * 1024 * 1024;
                var       fileLimitReached = false;

                // (Re-)create a new recent log.txt
                final var recentLogFile = new File(root, "log.txt");
                recentLogFile.setWritable(true);

                try (final Writer fw = new FileWriter(recentLogFile, true);
                        final Writer bw = new BufferedWriter(fw, DEFAULT_CHAR_BUFFER_SIZE);
                        final Writer out = new PrintWriter(bw)) {

                    outputWriter = out;
                    while (!fileLimitReached) {
                        // Conditionally blocking wait for element to become available
                        // as we don't wait forever for a buffered log message we use a timeout
                        final var entry = logEntryQueue.poll(configuration.maxBufferTimeInSeconds(), TimeUnit.SECONDS);
                        if (entry != null) {
                            final var logString = createLogMessage(entry);
                            out.write(logString);
                        }
                        if (shouldFlushLogs()) {
                            try {
                                out.flush();
                            } finally {
                                lastFlushTimeNanos = System.nanoTime();
                            }
                        }
                        fileLimitReached = recentLogFile.length() > limit;
                    }
                }
                outputWriter = null;
                /*
                 * Here the log file is closed already, either due to the file limit was reached
                 * or the worker was interrupted: If interrupted, the outer loop will
                 * fall-through If file limit was reached, we do a roll-over and remain in the
                 * loop
                 */
                if (fileLimitReached) {
                    // Roll the log files, archive and delete log files exceeding the configured
                    // limit
                    rolloverLogfiles();
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.atWarning().log("Worker thread '%s' was interrupted!", THREAD_NAME, e);
        } catch (final Exception e) {
            logger.atError().log("Exception in worker thread '%s' occurred!", THREAD_NAME, e);
        }
    }

    /** Always align with {@link Configuration#format()}. */
    private final String createLogMessage(final LogEntry entry) {
        // reuse the string builder
        formatBuilder.setLength(0);
        return formatter.format(configuration.format(), entry.getTime(),
                entry.getBundle() != null ? entry.getBundle().getBundleId() : null, entry.getLogLevel().name(),
                entry.getThreadInfo(), entry.getLoggerName(), entry.getMessage(),
                entry.getServiceReference() != null ? " [sref=" + entry.getServiceReference() + "]" : "",
                extractStackTrace(entry.getException())).toString();
    }

    private static String extractStackTrace(final Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        try (final var stringWriter = new StringWriter(); final var printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
            return stringWriter.toString();
        } catch (final IOException e) {
            // Should never happen - return at least the exception message
            return "extractStackTrace failed: throwable.message=" + throwable.getMessage();
        }
    }

    private void rolloverLogfiles() throws IOException {
        // Roll-over already compressed log files so that log4.txt.gz -> log5.txt.gz,
        // ... log0.txt.gz -> log1.txt.gz
        moveCompressedFilesAndDeleteExceeding();
        final var recentLogFile       = new File(root, "log.txt");
        final var targetZippedLogFile = new File(root, "log0.txt.gz");
        // Compress log.txt -> log0.txt.gz
        compress(recentLogFile, targetZippedLogFile);

        // Instead of deleting and re-creating log.txt, simply reset its content instead
        new PrintWriter(recentLogFile).close();
    }

    /**
     * Move existing compressed log files one up. e.g. log0.txt.gz to log1.txt.gz
     */
    private void moveCompressedFilesAndDeleteExceeding() {
        Stream.of(root.listFiles((dir, name) -> LOG_FILE_NAMES_GZIP_PATTERN.matcher(name).matches()))
                // Sorted inversely to return file name with highest number first, but respect
                // log10.txt.gz > log2.txt.gz
                .sorted((a, b) -> Integer.compare(getNumberInLogFileName(b.getName()),
                        getNumberInLogFileName(a.getName())))
                .forEachOrdered(compressedLogFile -> {
                    final var number = getNumberInLogFileName(compressedLogFile.getName());
                    // We subtract 2, because one for the 0-based index, second for log.txt which is
                    // not included
                    if (number + 2 >= configuration.maxRetainedLogs()) {
                        deleteFile(compressedLogFile);
                    } else {
                        moveLogFile(compressedLogFile, number);
                    }
                });
    }

    private void deleteFile(final File file) {
        try {
            Files.delete(file.toPath());
        } catch (final IOException e) {
            logger.atError().log("Unable to delete log file '%s'", file.getName(), e);
        }
    }

    private void moveLogFile(final File compressedLogFile, final int number) {
        final var targetFile = new File(root, "log" + (number + 1) + ".txt.gz");
        logger.atDebug().log("Renaming '%s' to '%s'", compressedLogFile.getAbsolutePath(),
                targetFile.getAbsolutePath());
        if (!compressedLogFile.renameTo(targetFile)) {
            logger.atError().log("Renaming of '%s' to '%s' failed!", compressedLogFile.getAbsolutePath(),
                    targetFile.getAbsolutePath());
        }
    }

    /**
     * Returns a number in the given input string, e.g. for {@code log10.txt.gz}
     * yields {@code 10}. Negative numbers are not supported.
     *
     * @param filename file name to be checked for a contained number, may not be
     *            {@code null}
     * @return the number contained in the string, or {@code -1} if no number could
     *         be obtained
     */
    private static int getNumberInLogFileName(final String filename) {
        requireNonNull(filename, "File name must not be null!");
        final var matcher = LOG_FILE_NAMES_GZIP_PATTERN.matcher(filename);
        if (matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group("nr"));
            } catch (final NumberFormatException nfe) {
                // nothing to do
            }
        }
        return -1;
    }

    /**
     * Compress a given file into the given output file.
     *
     * @param input plain-text input file handle
     * @param output resulting output file handle, e.g.
     *            {@code new File(root, "log0.txt.gz")}
     *
     * @throws IOException in case something went wrong
     */
    private void compress(final File input, final File output) throws IOException {
        try (final var fileOutputStream = new FileOutputStream(output);
                final var gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {
            Files.copy(input.toPath(), gzipOutputStream);
        }
    }

    /**
     * Decide whether the logs should be flushed to file.
     *
     * <Ul>
     * <li>Buffering disabled => {@code true}</li>
     * <li>Buffering enabled and buffer time exceeded => {@code true}</li>
     * <li>otherwise => {@code false}</li>
     * </ul>
     *
     * @return {@code true} if logs should be flushed, {@code false} otherwise
     */
    private boolean shouldFlushLogs() {
        if (!configuration.bufferEnabled()) {
            return true;
        }
        final var bufferTimeNanos      = Duration.ofSeconds(configuration.maxBufferTimeInSeconds()).toNanos();
        final var nextTimeToFlushNanos = lastFlushTimeNanos + bufferTimeNanos;
        return nextTimeToFlushNanos <= System.nanoTime();
    }

}
