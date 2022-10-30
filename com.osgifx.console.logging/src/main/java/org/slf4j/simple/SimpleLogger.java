/**
 * Copyright (c) 2004-2012 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.slf4j.simple;

import static java.lang.Integer.MIN_VALUE;
import static org.slf4j.simple.OutputChoice.OutputChoiceType.SYS_OUT;
import static org.slf4j.spi.LocationAwareLogger.DEBUG_INT;
import static org.slf4j.spi.LocationAwareLogger.ERROR_INT;
import static org.slf4j.spi.LocationAwareLogger.INFO_INT;
import static org.slf4j.spi.LocationAwareLogger.TRACE_INT;
import static org.slf4j.spi.LocationAwareLogger.WARN_INT;

import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.simple.LogEntryHelper.SimpleLogEntry;

public class SimpleLogger extends MarkerIgnoringBase {

    private static final long serialVersionUID = 8730968637749950758L;

    public static final int LOG_LEVEL_TRACE = TRACE_INT;
    public static final int LOG_LEVEL_DEBUG = DEBUG_INT;
    public static final int LOG_LEVEL_INFO  = INFO_INT;
    public static final int LOG_LEVEL_WARN  = WARN_INT;
    public static final int LOG_LEVEL_ERROR = ERROR_INT;

    public static final int LOG_LEVEL_AUDIT = MIN_VALUE;

    private static boolean initialized;

    static void lazyInit() {
        if (initialized) {
            return;
        }
        initialized = true;
    }

    /** The current log level */
    protected int currentLogLevel = LOG_LEVEL_INFO;

    private final transient long bundleId;

    /**
     * Package access allows only {@link SimpleLoggerFactory} to instantiate
     * SimpleLogger instances.
     */
    SimpleLogger(final String name, final long bundleId) {
        this.name       = name;
        this.bundleId   = bundleId;
        currentLogLevel = LOG_LEVEL_INFO;
    }

    /**
     * This is our internal implementation for logging regular (non-parameterized)
     * log messages.
     *
     * @param level One of the LOG_LEVEL_XXX constants defining the log level
     * @param message The message itself
     * @param t The exception whose stack trace should be logged
     */
    private void log(final int level, final String message, final Throwable t) {
        if (!isLevelEnabled(level)) {
            return;
        }
        final var logEntry = new SimpleLogEntry(name, bundleId, level, message, t);
        write(logEntry);
    }

    private void write(final SimpleLogEntry entry) {
        final var targetStream = new OutputChoice(SYS_OUT).getTargetPrintStream();
        targetStream.print(LogEntryHelper.createLogMessage(entry));
        targetStream.flush();
    }

    /**
     * For formatted messages, first substitute arguments and then log.
     *
     * @param level
     * @param format
     * @param arg1
     * @param arg2
     */
    private void formatAndLog(final int level, final String format, final Object arg1, final Object arg2) {
        if (!isLevelEnabled(level)) {
            return;
        }
        final var tp = MessageFormatter.format(format, arg1, arg2);
        log(level, tp.getMessage(), tp.getThrowable());
    }

    /**
     * For formatted messages, first substitute arguments and then log.
     *
     * @param level
     * @param format
     * @param arguments a list of 3 ore more arguments
     */
    private void formatAndLog(final int level, final String format, final Object... arguments) {
        if (!isLevelEnabled(level)) {
            return;
        }
        final var tp = MessageFormatter.arrayFormat(format, arguments);
        log(level, tp.getMessage(), tp.getThrowable());
    }

    /**
     * Is the given log level currently enabled?
     *
     * @param logLevel is this level enabled?
     * @return whether the logger is enabled for the given level
     */
    protected boolean isLevelEnabled(final int logLevel) {
        // log level are numerically ordered so can use simple numeric
        // comparison
        return logLevel >= currentLogLevel;
    }

    /** Are {@code trace} messages currently enabled? */
    @Override
    public boolean isTraceEnabled() {
        return isLevelEnabled(LOG_LEVEL_TRACE);
    }

    /**
     * A simple implementation which logs messages of level TRACE according to the
     * format outlined above.
     */
    @Override
    public void trace(final String msg) {
        log(LOG_LEVEL_TRACE, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * TRACE according to the format outlined above.
     */
    @Override
    public void trace(final String format, final Object param1) {
        formatAndLog(LOG_LEVEL_TRACE, format, param1, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * TRACE according to the format outlined above.
     */
    @Override
    public void trace(final String format, final Object param1, final Object param2) {
        formatAndLog(LOG_LEVEL_TRACE, format, param1, param2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * TRACE according to the format outlined above.
     */
    @Override
    public void trace(final String format, final Object... argArray) {
        formatAndLog(LOG_LEVEL_TRACE, format, argArray);
    }

    /** Log a message of level TRACE, including an exception. */
    @Override
    public void trace(final String msg, final Throwable t) {
        log(LOG_LEVEL_TRACE, msg, t);
    }

    /** Are {@code debug} messages currently enabled? */
    @Override
    public boolean isDebugEnabled() {
        return isLevelEnabled(LOG_LEVEL_DEBUG);
    }

    /**
     * A simple implementation which logs messages of level DEBUG according to the
     * format outlined above.
     */
    @Override
    public void debug(final String msg) {
        log(LOG_LEVEL_DEBUG, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * DEBUG according to the format outlined above.
     */
    @Override
    public void debug(final String format, final Object param1) {
        formatAndLog(LOG_LEVEL_DEBUG, format, param1, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * DEBUG according to the format outlined above.
     */
    @Override
    public void debug(final String format, final Object param1, final Object param2) {
        formatAndLog(LOG_LEVEL_DEBUG, format, param1, param2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * DEBUG according to the format outlined above.
     */
    @Override
    public void debug(final String format, final Object... argArray) {
        formatAndLog(LOG_LEVEL_DEBUG, format, argArray);
    }

    /** Log a message of level DEBUG, including an exception. */
    @Override
    public void debug(final String msg, final Throwable t) {
        log(LOG_LEVEL_DEBUG, msg, t);
    }

    /** Are {@code info} messages currently enabled? */
    @Override
    public boolean isInfoEnabled() {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    /**
     * A simple implementation which logs messages of level INFO according to the
     * format outlined above.
     */
    @Override
    public void info(final String msg) {
        log(LOG_LEVEL_INFO, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * INFO according to the format outlined above.
     */
    @Override
    public void info(final String format, final Object arg) {
        formatAndLog(LOG_LEVEL_INFO, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * INFO according to the format outlined above.
     */
    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        formatAndLog(LOG_LEVEL_INFO, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * INFO according to the format outlined above.
     */
    @Override
    public void info(final String format, final Object... argArray) {
        formatAndLog(LOG_LEVEL_INFO, format, argArray);
    }

    /** Log a message of level INFO, including an exception. */
    @Override
    public void info(final String msg, final Throwable t) {
        log(LOG_LEVEL_INFO, msg, t);
    }

    /** Are {@code warn} messages currently enabled? */
    @Override
    public boolean isWarnEnabled() {
        return isLevelEnabled(LOG_LEVEL_WARN);
    }

    /**
     * A simple implementation which always logs messages of level WARN according to
     * the format outlined above.
     */
    @Override
    public void warn(final String msg) {
        log(LOG_LEVEL_WARN, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * WARN according to the format outlined above.
     */
    @Override
    public void warn(final String format, final Object arg) {
        formatAndLog(LOG_LEVEL_WARN, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * WARN according to the format outlined above.
     */
    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        formatAndLog(LOG_LEVEL_WARN, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * WARN according to the format outlined above.
     */
    @Override
    public void warn(final String format, final Object... argArray) {
        formatAndLog(LOG_LEVEL_WARN, format, argArray);
    }

    /** Log a message of level WARN, including an exception. */
    @Override
    public void warn(final String msg, final Throwable t) {
        log(LOG_LEVEL_WARN, msg, t);
    }

    /** Are {@code error} messages currently enabled? */
    @Override
    public boolean isErrorEnabled() {
        return isLevelEnabled(LOG_LEVEL_ERROR);
    }

    /**
     * A simple implementation which always logs messages of level ERROR according
     * to the format outlined above.
     */
    @Override
    public void error(final String msg) {
        log(LOG_LEVEL_ERROR, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * ERROR according to the format outlined above.
     */
    @Override
    public void error(final String format, final Object arg) {
        formatAndLog(LOG_LEVEL_ERROR, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * ERROR according to the format outlined above.
     */
    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        formatAndLog(LOG_LEVEL_ERROR, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * ERROR according to the format outlined above.
     */
    @Override
    public void error(final String format, final Object... argArray) {
        formatAndLog(LOG_LEVEL_ERROR, format, argArray);
    }

    /** Log a message of level ERROR, including an exception. */
    @Override
    public void error(final String msg, final Throwable t) {
        log(LOG_LEVEL_ERROR, msg, t);
    }

    /**
     * A simple implementation which always logs messages of level AUDIT according
     * to the format outlined above.
     */
    public void audit(final String msg) {
        log(LOG_LEVEL_AUDIT, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * AUDIT according to the format outlined above.
     */
    public void audit(final String format, final Object arg) {
        formatAndLog(LOG_LEVEL_AUDIT, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * AUDIT according to the format outlined above.
     */
    public void audit(final String format, final Object arg1, final Object arg2) {
        formatAndLog(LOG_LEVEL_AUDIT, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * AUDIT according to the format outlined above.
     */
    public void audit(final String format, final Object... argArray) {
        formatAndLog(LOG_LEVEL_AUDIT, format, argArray);
    }

    /** Log a message of level AUDIT, including an exception. */
    public void audit(final String msg, final Throwable t) {
        log(LOG_LEVEL_AUDIT, msg, t);
    }

    public void log(final LoggingEvent event) {
        final var levelInt = event.getLevel().toInt();

        if (!isLevelEnabled(levelInt)) {
            return;
        }
        final var tp = MessageFormatter.arrayFormat(event.getMessage(), event.getArgumentArray(), event.getThrowable());
        log(levelInt, tp.getMessage(), event.getThrowable());
    }

}