package org.slf4j.simple;

import static com.osgifx.console.log.LoggerConstants.CONFIDENTIAL_PREFIX;
import static org.slf4j.simple.SimpleLogger.LOG_LEVEL_AUDIT;
import static org.slf4j.simple.SimpleLogger.LOG_LEVEL_DEBUG;
import static org.slf4j.simple.SimpleLogger.LOG_LEVEL_ERROR;
import static org.slf4j.simple.SimpleLogger.LOG_LEVEL_INFO;
import static org.slf4j.simple.SimpleLogger.LOG_LEVEL_TRACE;
import static org.slf4j.simple.SimpleLogger.LOG_LEVEL_WARN;

import com.google.common.base.Throwables;
import com.osgifx.console.logging.PasswordHelper;

public final class LogEntryHelper {

    private static final String FORMAT = "%tY-%<tm-%<tdT%<tH:%<tM:%<tS.%<tL%<tz %4s %-5s [%s,%s] %s%n%s";

    private LogEntryHelper() {
        throw new IllegalAccessError("Non-instantiable");
    }

    // @formatter:off
    public static String createLogMessage(final SimpleLogEntry entry) {
        return String.format(FORMAT,
                entry.time,
                entry.bundleId,
                entry.logLevel(),
                entry.threadName,
                entry.name,
                entry.message,
                entry.exception);
    }
    // @formatter:on

    public static final class SimpleLogEntry {

        final String name;
        final long   bundleId;
        final int    level;
        final String message;
        final String exception;
        final long   time;
        final String threadName;

        SimpleLogEntry(final String name, final long bundleId, final int level, final String message, final Throwable exception) {

            this.name     = name;
            this.level    = level;
            this.bundleId = bundleId;

            this.message   = applyMasking(message);
            this.exception = Throwables.getStackTraceAsString(exception);

            time       = System.currentTimeMillis();
            threadName = Thread.currentThread().getName();
        }

        private String applyMasking(final String message) {
            if (message == null) {
                return "";
            }
            if (message.startsWith(CONFIDENTIAL_PREFIX)) {
                final var mask = PasswordHelper.mask(message);
                return mask.substring(CONFIDENTIAL_PREFIX.length()); // remove the prefix from log
            }
            return message;
        }

        public String logLevel() {
            switch (level) {
            case LOG_LEVEL_TRACE:
                return "TRACE";
            case LOG_LEVEL_DEBUG:
                return "DEBUG";
            case LOG_LEVEL_INFO:
                return "INFO";
            case LOG_LEVEL_WARN:
                return "WARN";
            case LOG_LEVEL_ERROR:
                return "ERROR";
            case LOG_LEVEL_AUDIT:
                return "AUDIT";
            default:
                throw new IllegalStateException("Unrecognized level [" + level + "]");
            }
        }
    }

}
