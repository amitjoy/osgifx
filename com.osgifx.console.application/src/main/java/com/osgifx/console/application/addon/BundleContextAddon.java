/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.application.addon;

import static org.osgi.framework.Bundle.ACTIVE;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.BundleTracker;

public final class BundleContextAddon {

    @Log
    @Inject
    private FluentLogger          logger;
    private BundleTracker<Bundle> bundleTracker;

    @PostConstruct
    public void init(final IEclipseContext eclipseContext) {
        final BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        bundleTracker = new BundleTracker<Bundle>(bundleContext, ACTIVE, null) {
            @Override
            public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
                final String bsn = bundle.getSymbolicName();
                eclipseContext.set(bsn, bundle.getBundleContext());
                logger.atDebug().log("Bundle context with BSN '%s' has been added to global eclipse context", bsn);
                return bundle;
            }

            @Override
            public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
                addingBundle(bundle, event);
            }

            @Override
            public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
                final String bsn = bundle.getSymbolicName();
                eclipseContext.remove(bsn);
                logger.atDebug().log("Bundle context with BSN '%s' has been removed from global eclipse context", bsn);
            }

        };
        bundleTracker.open();
        logger.atInfo().log("Bundle context addon has been initialized");
    }

    @PreDestroy
    public void destroy() {
        bundleTracker.close();
        logger.atInfo().log("Bundle context addon has been destroyed");
    }

}
