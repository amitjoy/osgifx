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
package com.osgifx.console.logging.forwarder;

import org.osgi.framework.Bundle;
import org.osgi.service.log.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;

/**
 * Facade for forwarding log messages to a delegate set by
 * {@link StaticLoggerController}.
 */
final class Slf4jLoggerFacade extends MarkerIgnoringBase {

    private static final long serialVersionUID = -3302804462587828985L;

    /**
     * All {@link org.slf4j.Logger} calls to this class are forwarded to this
     * delegate.
     *
     * delegate can be assigned and read from different threads and places in
     * package-private scope, therefore needs to be volatile
     */
    volatile Logger delegate; // NOSONAR

    private final transient Bundle bundle;

    public Slf4jLoggerFacade(final Bundle bundle, final String name) {
        this.name   = name;
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public void debug(final String format, final Object... arguments) {
        if (checkIfArgumentsNullOrEmpty(arguments)) {
            delegate.debug(format);
        } else {
            delegate.debug(format, arguments);
        }
    }

    @Override
    public void debug(final String message) {
        delegate.debug(message);
    }

    @Override
    public void debug(final String format, final Object arguments) {
        delegate.debug(format, arguments);
    }

    @Override
    public void debug(final String message, final Throwable t) {
        delegate.debug(message, t);
    }

    @Override
    public void debug(final String format, final Object a, final Object b) {
        delegate.debug(format, a, b);
    }

    @Override
    public void error(final String format, final Object... arguments) {
        if (checkIfArgumentsNullOrEmpty(arguments)) {
            delegate.error(format);
        } else {
            delegate.error(format, arguments);
        }
    }

    @Override
    public void error(final String message) {
        delegate.error(message);
    }

    @Override
    public void error(final String format, final Object arguments) {
        delegate.error(format, arguments);
    }

    @Override
    public void error(final String message, final Throwable t) {
        delegate.error(message, t);
    }

    @Override
    public void error(final String format, final Object a, final Object b) {
        delegate.error(format, a, b);
    }

    @Override
    public void info(final String format, final Object... arguments) {
        if (checkIfArgumentsNullOrEmpty(arguments)) {
            delegate.info(format);
        } else {
            delegate.info(format, arguments);
        }
    }

    @Override
    public void info(final String message) {
        delegate.info(message);
    }

    @Override
    public void info(final String format, final Object arguments) {
        delegate.info(format, arguments);
    }

    @Override
    public void info(final String message, final Throwable t) {
        delegate.info(message, t);
    }

    @Override
    public void info(final String format, final Object a, final Object b) {
        delegate.info(format, a, b);
    }

    @Override
    public void warn(final String format, final Object... arguments) {
        if (checkIfArgumentsNullOrEmpty(arguments)) {
            delegate.warn(format);
        } else {
            delegate.warn(format, arguments);
        }
    }

    @Override
    public void warn(final String message) {
        delegate.warn(message);
    }

    @Override
    public void warn(final String format, final Object arguments) {
        delegate.warn(format, arguments);
    }

    @Override
    public void warn(final String message, final Throwable t) {
        delegate.warn(message, t);
    }

    @Override
    public void warn(final String format, final Object a, final Object b) {
        delegate.warn(format, a, b);
    }

    @Override
    public void trace(final String format, final Object... arguments) {
        if (checkIfArgumentsNullOrEmpty(arguments)) {
            delegate.trace(format);
        } else {
            delegate.trace(format, arguments);
        }
    }

    @Override
    public void trace(final String message) {
        delegate.trace(message);
    }

    @Override
    public void trace(final String format, final Object arguments) {
        delegate.trace(format, arguments);
    }

    @Override
    public void trace(final String message, final Throwable t) {
        delegate.trace(message, t);
    }

    @Override
    public void trace(final String format, final Object a, final Object b) {
        delegate.trace(format, a, b);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    /**
     * Checks whether the specified varargs is empty or null. This is primarily used
     * as a hack to avoid NPE in special cases.
     *
     * @param arguments the arguments to check
     * @return {@code true} if {@code null} or empty, otherwise {@code false}
     *
     * @see https://issues.apache.org/jira/browse/FELIX-6088
     */
    private boolean checkIfArgumentsNullOrEmpty(final Object... arguments) {
        return arguments == null || arguments.length == 0;
    }

}
