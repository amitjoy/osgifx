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

import org.osgi.service.log.LogLevel;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Define the configuration of the rolling file log reader.
 */
@ObjectClassDefinition( //
        id = Configuration.PID, //
        name = "File logging configuration", //
        description = "Configures the service that listens to OSGi log events and writes to files")
public @interface Configuration {

    String PID = "com.osgifx.diagnostics";

    /**
     * We use var/log/java as relative path to folder where we have been started.
     */
    @AttributeDefinition(name = "Log files folder", description = "Folder for log files, may be absolute or relative to JVM starting path", required = false)
    String root() default "./${storage}/log";

    @AttributeDefinition(name = "Max log file size", description = "Maximum filesize of a single logfile in MiB", required = false)
    int maxLogSizeMb() default 5;

    @AttributeDefinition(name = "Max retained log files", description = "Maximum number of retained log files", required = false)
    int maxRetainedLogs() default 10;

    @AttributeDefinition(name = "Max buffer time", description = "Logs will be flushed latest after this time", required = false)
    int maxBufferTimeInSeconds() default 60;

    /**
     * Format of log message in the log files, denotes the following:
     *
     * Custom date-time format with 28 digits: yyyy-MM-dd'T'HH:mm:ss.SSSZ (see
     * https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html), single
     * space, 4-digit bundle id, single space, left-aligned 5-digit log level,
     * single space, opening brace [, thread info, comma, logger name, closing brace
     * ], single space, log message, optional: [sref=serviceReference] (in case sref
     * is available) newline, optional multi-line stack-trace containing new-line at
     * the end
     *
     * Samples:
     *
     * <pre>
     * 2019-06-10T19:16:37.314+0000   30 DEBUG [main,a.b.c.d.E] some message [sref=[javax.servlet.Servlet]]
     * 2019-06-12T23:17:37.982+0200   32 INFO  [FelixDispatchQueue,Events.Bundle] BundleEvent STARTED [bundle=a.b.c.d]
     * </pre>
     *
     * @return format as described
     */
    @AttributeDefinition(name = "Format of log messages", description = "Format of log entries to write", required = false)
    String format() default "%tY-%<tm-%<tdT%<tH:%<tM:%<tS.%<tL%<tz %4s %-5s [%s,%s] %s%s%n%s";

    @AttributeDefinition(name = "Log level", description = "The minimum level to log out to the files", required = false)
    LogLevel level() default LogLevel.DEBUG;

    @AttributeDefinition(name = "Buffer enabled", description = "Buffer log messages before flushing to disk", required = false)
    boolean bufferEnabled() default true;

}
