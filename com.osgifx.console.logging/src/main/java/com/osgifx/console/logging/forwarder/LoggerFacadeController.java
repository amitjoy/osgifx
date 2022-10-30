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

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.osgi.framework.BundleEvent.STARTED;
import static org.osgi.framework.BundleEvent.STOPPED;
import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.configurator.annotations.RequireConfigurator;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This acts as controller for the {@link Slf4jLoggerFacade} instances to notify
 * them if the {@link LogService} became available or unavailable.
 * <p/>
 * If the {@link LogService} is registered to the OSGi ServiceRegistry, the
 * reference gets satisfied and this service component will activate. If the
 * {@link LogService} is not registered to the OSGi ServiceRegistry, the
 * reference cannot be satisfied and this service component will either not
 * activate, or if already active, will deactivate.
 * <p/>
 * NOTE: Do not make this class package-private, as OSGi will be unable to
 * instantiate it
 */
@RequireConfigurator
@Header(name = BUNDLE_ACTIVATOR, value = "${@class}")
public final class LoggerFacadeController implements BundleActivator {

    private static final String CLEANUP_STALE_LOGGER_REFERENCES_THREAD_NAME = "logging-cleanup-stale-refs";

    private ServiceTracker<LogService, LogService> tracker;
    private SynchronousBundleListener              bundleListener;
    private ScheduledExecutorService               cleanupStaleLoggerReferencesExecutor;

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        tracker = new ServiceTracker<>(bundleContext, LogService.class, null) {
            @Override
            public LogService addingService(final ServiceReference<LogService> reference) {
                init(bundleContext.getService(reference));
                return super.addingService(reference);
            }

            @Override
            public void removedService(final ServiceReference<LogService> reference, final LogService agent) {
                StaticLoggerController.setLogService(null);
                if (cleanupStaleLoggerReferencesExecutor != null) {
                    cleanupStaleLoggerReferencesExecutor.shutdownNow();
                }
                cleanupStaleLoggerReferencesExecutor = null;
            }
        };
        tracker.open();
        bundleListener = event -> {
            if (event.getType() == STARTED) {
                StaticLoggerController.addStartedBundle(event.getBundle());
            } else if (event.getType() == STOPPED) {
                StaticLoggerController.removeBundle(event.getBundle());
            }
        };
        bundleContext.addBundleListener(bundleListener);
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        bundleContext.removeBundleListener(bundleListener);
        tracker.close();
    }

    private void init(final LogService logService) {
        StaticLoggerController.setLogService(logService);
        cleanupStaleLoggerReferencesExecutor = Executors
                .newSingleThreadScheduledExecutor(r -> new Thread(r, CLEANUP_STALE_LOGGER_REFERENCES_THREAD_NAME));
        cleanupStaleLoggerReferencesExecutor.scheduleAtFixedRate(StaticLoggerController::purgeStaleLoggerReferences, 10,
                10, MINUTES);
    }

}
