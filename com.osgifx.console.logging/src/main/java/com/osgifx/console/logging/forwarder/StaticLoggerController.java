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

import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.RESOLVED;
import static org.osgi.framework.Bundle.STARTING;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;

/**
 * Central controller for switching the {@link Slf4jLoggerFacade} delegates to
 * either a queue as long as the OSGi LogService is not available, or if the
 * OSGi LogService is available, switch to forward log messages to the OSGi
 * Logger directly and furthermore drain the former queued log messages to it.
 *
 * We use a Map of LoggerId to WeakReferences and check at a fixed rate (in
 * {@link #purgeStaleLoggerReferences()} if a WeakReference has been set to
 * null, then remove the map entry.
 */
final class StaticLoggerController {

    private static LogService                                                       logService;
    private static Lock                                                             lock          = new ReentrantLock();
    private static final Map<Bundle, Map<String, WeakReference<Slf4jLoggerFacade>>> loggerFacades = new HashMap<>();

    private StaticLoggerController() {
    }

    /**
     * Retrieve a {@link Logger} instance depending on given bundle and logger name.
     *
     * @param bundle bundle that wants to log
     * @param name   name of the logger
     * @return a newly created or existing {@link Logger} instance
     */
    static Logger createLogger(final Bundle bundle, final String name) {
        lock.lock();
        try {
            final var bundleMap       = loggerFacades.computeIfAbsent(bundle, b -> new HashMap<>());
            final var loggerFacadeRef = bundleMap.get(name);

            // If mapping is present
            if (loggerFacadeRef != null) {
                // If the referent was gc'ed, the WeakReference holds a null entry
                final Logger cachedLogger = loggerFacadeRef.get();
                if (cachedLogger != null) {
                    return cachedLogger;
                }
            }

            // Either no mapping was present or the entry was gc'ed
            final var loggerFacade = new Slf4jLoggerFacade(bundle, name);
            /*
             * Apache Felix will throw an IllegalArgumentException if a non-active bundle
             * tries to log, e.g. in its bundle-activator method, and a NPE if the
             * bundleContext is not yet set but the bundle is active
             */
            if (isBundleInvalid(bundle)) {
                loggerFacade.delegate = logService.getLogger(name);
            } else {
                loggerFacade.delegate = logService.getLogger(bundle, name, org.osgi.service.log.Logger.class);
                if (loggerFacade.delegate == null) {
                    // may happen if LogService for the bundle has been already deactivated, but not
                    // the bundle
                    // itself
                    loggerFacade.delegate = logService.getLogger(name);
                }
            }
            // Create new entry or overwrite gc'ed entry
            bundleMap.put(name, new WeakReference<>(loggerFacade));
            return loggerFacade;
        } finally {
            lock.unlock();
        }
    }

    private static boolean isBundleInvalid(final Bundle bundle) {
        return bundle == null || bundle.getBundleContext() == null || (bundle.getState() & ACTIVE) != ACTIVE
                && (bundle.getState() & STARTING) != STARTING && (bundle.getState() & RESOLVED) != RESOLVED;
    }

    /**
     * This may only be called pair-wise with LogService and {@code null}, not
     * two-times not {@code null} consecutively.
     *
     * @param logService logService to set, or {@code null}
     */
    static void setLogService(final LogService logService) {
        lock.lock();
        try {
            StaticLoggerController.logService = logService;
            getLoggerFacades().forEach(lf -> {
                /*
                 * Apache Felix will throw an IllegalArgumentException if a non-active bundle
                 * tries to log, e.g. in its bundle-activator method, and a NPE if the
                 * bundleContext is not yet set but the bundle is active
                 */
                if (isBundleInvalid(lf.getBundle())) {
                    lf.delegate = logService.getLogger(lf.getName());
                } else {
                    lf.delegate = logService.getLogger(lf.getBundle(), lf.getName(), org.osgi.service.log.Logger.class);
                }
            });
        } finally {
            lock.unlock();
        }
    }

    static void removeBundle(final Bundle bundle) {
        lock.lock();
        try {
            loggerFacades.remove(bundle);
        } finally {
            lock.unlock();
        }
    }

    static void addStartedBundle(final Bundle bundle) {
        lock.lock();
        try {
            if (logService != null) {
                getLoggerFacadesForBundle(bundle).forEach(lf -> {
                    lf.delegate = logService.getLogger(lf.getBundle(), lf.getName(), org.osgi.service.log.Logger.class);
                });
            }
        } finally {
            lock.unlock();
        }
    }

    private static Stream<Slf4jLoggerFacade> getLoggerFacades() {
        return loggerFacades.values().stream().flatMap(c -> c.values().stream()).map(WeakReference::get).filter(Objects::nonNull);
    }

    private static Stream<Slf4jLoggerFacade> getLoggerFacadesForBundle(final Bundle bundle) {
        final var bundleLoggerFacades = loggerFacades.get(bundle);
        if (bundleLoggerFacades != null) {
            return bundleLoggerFacades.values().stream().map(WeakReference::get).filter(Objects::nonNull);
        }
        return Stream.empty();
    }

    static void purgeStaleLoggerReferences() {
        lock.lock();
        try {
            loggerFacades.values().stream().forEach(
                    bundleMap -> bundleMap.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().get() == null));
            loggerFacades.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        } finally {
            lock.unlock();
        }
    }

}
