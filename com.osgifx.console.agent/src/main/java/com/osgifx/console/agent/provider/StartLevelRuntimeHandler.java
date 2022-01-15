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
package com.osgifx.console.agent.provider;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;

import aQute.lib.bundles.BundleIdentity;
import aQute.lib.startlevel.Trace;
import aQute.libg.parameters.ParameterMap;

/**
 * Support to handle start levels in a launcher. This code is related to code in
 * the Project Launcher. It is in aQute.lib so it can be included easily in the
 * Launcher, the Remote launcher, and Launchpad.
 * <p>
 * This class is not threadsafe!
 */
public class StartLevelRuntimeHandler implements Closeable {

    /**
     * If this property is set we take on start levels, if this property is not
     * set we ignore the startlevels completely. This is defined in
     * aQute.bnd.osgi.Constants
     */
    public static String LAUNCH_STARTLEVEL_DEFAULT = "launch.startlevel.default";
    public static String LAUNCH_RUNBUNDLES_ATTRS   = "launch.runbundles.attrs";

    /**
     * Indicate if this class supports start levels or not.
     *
     * @return true if this class supports startlevels
     */
    public boolean hasStartLevels() {
        return false;
    }

    /**
     * Set the start level of a bundle
     *
     * @param b the bundle
     */
    public void setStartLevel(final Bundle b) {
    }

    /**
     * Answer the current framework start level
     *
     * @param framework the framework
     * @return the current start level of the framework
     */
    public int getFrameworkStartLevel(final Framework framework) {
        return framework.adapt(FrameworkStartLevel.class).getStartLevel();
    }

    /**
     * Set the default start level of newly installed bundles
     *
     * @param framework the framework
     * @param level the default start level
     */
    public void setDefaultStartlevel(final Framework framework, final int level) {
        framework.adapt(FrameworkStartLevel.class).setInitialBundleStartLevel(level);
    }

    /**
     * Set the framework start level and return previous
     *
     * @param framework the framework
     * @param startlevel the start level to set
     * @param ls listeners
     * @return the previous start level of the framework
     */
    public int setFrameworkStartLevel(final Framework framework, final int startlevel, final FrameworkListener... ls) {
        final int previous = getFrameworkStartLevel(framework);
        framework.adapt(FrameworkStartLevel.class).setStartLevel(startlevel, ls);
        return previous;
    }

    /**
     * Get a bundle's start level
     *
     * @param bundle the bundle to query
     * @return the start level > 0
     */
    public int getBundleStartLevel(final Bundle bundle) {
        return bundle.adapt(BundleStartLevel.class).getStartLevel();
    }

    /**
     * Set a bundle's start level
     *
     * @param bundle the bundle to query
     * @param startlevel start level to set, > 0
     */
    public void setBundleStartLevel(final Bundle bundle, final int startlevel) {
        bundle.adapt(BundleStartLevel.class).setStartLevel(startlevel);
    }

    /**
     * Must be called before the framework is started.
     * <p>
     * ensure systemBundle.getState() == INIT and startlevel systemBundle == 0
     *
     * @param systemBundle the framework
     */
    public void beforeStart(final Framework systemBundle) {
    }

    /**
     * When the configuration properties have been updated
     *
     * @param configuration the configuration properties
     */
    public void updateConfiguration(final Map<String, ?> configuration) {
    }

    /**
     * Called after the framework is started and the launcher is ready
     */
    public void afterStart() {
    }

    /**
     * Wait for the framework to reach its start level. Must be called after the
     * {@link #afterStart()} method. Will return when the framework has
     * traversed all start levels.
     */
    public void sync() {
    }

    /**
     * Close this object
     */

    @Override
    public void close() {
    }

    /**
     * Create a start level handler. If the {@link #LAUNCH_STARTLEVEL_DEFAULT}
     * property is set we create an active handler that will direct the
     * framework properly according to the settings in Project Launcher. If not
     * set, a dummy is returned that does not do anything
     *
     * @param outerConfiguration the properties as set by the Project Launcher
     * @return an active or dummy {@link StartLevelRuntimeHandler}
     */
    static public StartLevelRuntimeHandler create(final Trace logger, final Map<String, String> outerConfiguration) {

        final String defaultStartlevelString = outerConfiguration.get(LAUNCH_STARTLEVEL_DEFAULT);
        if (defaultStartlevelString == null) {
            logger.trace("startlevel: not handled");
            return absent();
        }

        final int defaultStartlevel   = toInt(defaultStartlevelString, 1);
        final int beginningStartlevel = toInt(outerConfiguration.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL), 1);
        outerConfiguration.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "1");

        logger.trace("startlevel: handled begin=%s default=%s", beginningStartlevel, defaultStartlevel);

        //
        // We need to remove it otherwise the framework reacts to it
        //

        return new StartLevelRuntimeHandler() {
            CountDownLatch                             latch       = new CountDownLatch(1);
            private Framework                          systemBundle;
            private final Map<BundleIdentity, Integer> startlevels = new HashMap<>();
            private final Map<Bundle, BundleIdentity>  installed   = new HashMap<>();

            @Override
            public void beforeStart(final Framework systemBundle) {
                assert getFrameworkStartLevel(systemBundle) == 0 : "Expects the framework to be in init mode, not yet started";

                this.systemBundle = systemBundle;

                updateConfiguration(outerConfiguration);

                setDefaultStartlevel(this.systemBundle, defaultStartlevel);

                systemBundle.getBundleContext().addBundleListener((SynchronousBundleListener) event -> {
                    final Bundle bundle = event.getBundle();
                    if (bundle.getBundleId() == 0) {
                        return;
                    }

                    if (bundle.getSymbolicName() == null) {
                        logger.trace("Found bundle without a bsn %s, ignoring", bundle);
                        return;
                    }

                    final BundleIdentity id = installed.computeIfAbsent(bundle, BundleIdentity::new);
                    if (event.getType() == BundleEvent.INSTALLED || event.getType() == BundleEvent.UPDATED) {
                        setStartlevel(bundle, id);
                    } else if (event.getType() == BundleEvent.UNINSTALLED) {
                        installed.remove(bundle);
                    }
                });
                logger.trace("startlevel: default=%s, beginning=%s", defaultStartlevel, beginningStartlevel);

            }

            @Override
            public void afterStart() {
                setFrameworkStartLevel(systemBundle, beginningStartlevel, event -> {
                    logger.trace("startlevel: notified reached final level %s : %s", beginningStartlevel, event);
                    latch.countDown();
                });
                logger.trace("startlevel change begin: beginning level %s", beginningStartlevel);
            }

            @Override
            public void sync() {
                try {
                    latch.await();
                } catch (final InterruptedException ie) {
                    Thread.interrupted();
                    throw new RuntimeException(ie);
                }
            }

            @Override
            public boolean hasStartLevels() {
                return true;
            }

            @Override
            public void updateConfiguration(final Map<String, ?> configuration) {
                new ParameterMap((String) configuration.get(LAUNCH_RUNBUNDLES_ATTRS)).entrySet().forEach(entry -> {
                    final String         bsn     = ParameterMap.removeDuplicateMarker(entry.getKey());
                    final String         version = entry.getValue().getVersion();
                    final BundleIdentity id      = new BundleIdentity(bsn, version);

                    final int startlevel = toInt(entry.getValue().get("startlevel"), -1);
                    if (startlevel > 0) {
                        startlevels.put(id, startlevel);
                    }
                });

                installed.forEach(this::setStartlevel);
            }

            private void setStartlevel(final Bundle bundle, final BundleIdentity id) {
                if (bundle.getState() != Bundle.UNINSTALLED) {
                    int level = startlevels.getOrDefault(id, -1);
                    if (level == -1) {
                        level = defaultStartlevel;
                    }

                    setBundleStartLevel(bundle, level);
                    logger.trace("startlevel: %s <- %s", bundle, level);
                }
            }

        };
    }

    static int toInt(final Object object, final int defltValue) {
        if (object == null) {
            return defltValue;
        }

        final String s = object.toString().trim();
        try {
            return Integer.parseInt(s);
        } catch (final NumberFormatException nfe) {
            return defltValue;
        }
    }

    public static StartLevelRuntimeHandler absent() {
        return new StartLevelRuntimeHandler() {
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static StartLevelRuntimeHandler create(final Trace reporter, final Properties properties) {
        return create(reporter, (Map) properties);
    }
}
