/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
package com.osgifx.console.agent.admin;

import static java.util.function.Function.identity;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.INSTALLED;
import static org.osgi.framework.Bundle.RESOLVED;
import static org.osgi.framework.Bundle.STARTING;
import static org.osgi.framework.Bundle.STOPPING;
import static org.osgi.framework.Bundle.UNINSTALLED;
import static org.osgi.framework.Constants.BUNDLE_CATEGORY;
import static org.osgi.framework.Constants.BUNDLE_DESCRIPTION;
import static org.osgi.framework.Constants.BUNDLE_DOCURL;
import static org.osgi.framework.Constants.BUNDLE_VENDOR;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;
import static org.osgi.framework.Constants.VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;
import static org.osgi.framework.wiring.BundleRevision.PACKAGE_NAMESPACE;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.util.tracker.BundleTracker;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleInfoDTO;
import com.osgifx.console.agent.dto.XPackageDTO;
import com.osgifx.console.agent.dto.XPackageDTO.XpackageType;
import com.osgifx.console.agent.dto.XServiceInfoDTO;
import com.osgifx.console.agent.provider.BundleStartTimeCalculator;
import com.osgifx.console.agent.provider.BundleStartTimeCalculator.BundleStartDuration;

import jakarta.inject.Inject;

public final class XBundleAdmin {

    private final Map<Long, XBundleDTO>     bundles = new ConcurrentHashMap<>();
    private final BundleContext             context;
    private final BundleStartTimeCalculator bundleStartTimeCalculator;
    private BundleTracker<Future<?>>        bundleTracker;
    private ExecutorService                 executor;

    private static final FluentLogger logger = LoggerFactory.getFluentLogger(XBundleAdmin.class);

    @Inject
    public XBundleAdmin(final BundleContext context,
                        final BundleStartTimeCalculator bundleStartTimeCalculator,
                        final ExecutorService executor) {
        this.context                   = context;
        this.bundleStartTimeCalculator = bundleStartTimeCalculator;
        this.executor                  = executor;
    }

    public void init() {
        bundleTracker = new BundleTracker<Future<?>>(context,
                                                     Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING
                                                             | Bundle.ACTIVE | Bundle.STOPPING | Bundle.UNINSTALLED,
                                                     null) {
            @Override
            public Future<?> addingBundle(final Bundle bundle, final BundleEvent event) {
                return executor.submit(() -> processBundleSafely(bundle));
            }

            @Override
            public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Future<?> object) {
                executor.submit(() -> processBundleSafely(bundle));
            }

            @Override
            public void removedBundle(final Bundle bundle, final BundleEvent event, final Future<?> object) {
                if (object != null && !object.isDone()) {
                    object.cancel(true);
                }
                bundles.remove(bundle.getBundleId());
            }
        };
        bundleTracker.open();
    }

    public void stop() {
        if (bundleTracker != null) {
            bundleTracker.close();
        }
    }

    private void processBundleSafely(final Bundle bundle) {
        try {
            final XBundleDTO dto = toDTO(bundle, bundleStartTimeCalculator);
            bundles.put(bundle.getBundleId(), dto);
        } catch (final Exception e) {
            logger.atError().msg("Error processing bundle '{}'").arg(bundle.getSymbolicName()).throwable(e).log();
        }
    }

    public List<XBundleDTO> get() {
        if (context == null) {
            logger.atWarn().msg("Bundle context is null").log();
            return Collections.emptyList();
        }
        return new ArrayList<>(bundles.values());
    }

    public static XBundleDTO toDTO(final Bundle bundle, final BundleStartTimeCalculator bundleStartTimeCalculator) {
        final XBundleDTO dto = new XBundleDTO();

        dto.id                  = bundle.getBundleId();
        dto.state               = findState(bundle.getState());
        dto.symbolicName        = bundle.getSymbolicName();
        dto.version             = bundle.getVersion().toString();
        dto.location            = bundle.getLocation();
        dto.category            = getHeader(bundle, BUNDLE_CATEGORY);
        dto.isFragment          = getHeader(bundle, FRAGMENT_HOST) != null;
        dto.lastModified        = bundle.getLastModified();
        dto.dataFolderSize      = getStorageSize(bundle);
        dto.documentation       = getHeader(bundle, BUNDLE_DOCURL);
        dto.vendor              = getHeader(bundle, BUNDLE_VENDOR);
        dto.description         = getHeader(bundle, BUNDLE_DESCRIPTION);
        dto.startLevel          = getStartLevel(bundle);
        dto.frameworkStartLevel = getFrameworkStartLevel();
        // @formatter:off
        dto.startDurationInMillis  = bundleStartTimeCalculator.getBundleStartDuration(bundle.getBundleId())
                                                              .map(BundleStartDuration::getStartedAfter)
                                                              .map(Duration::toMillis)
                                                              .orElse(-1L);
        // @formatter:on
        dto.bundleRevision         = bundle.adapt(BundleRevisionDTO.class);
        dto.exportedPackages       = getExportedPackages(bundle);
        dto.importedPackages       = getImportedPackages(bundle);
        dto.wiredBundlesAsProvider = getWiredBundlesAsProvider(bundle);
        dto.wiredBundlesAsRequirer = getWiredBundlesAsRequirer(bundle);
        dto.registeredServices     = getRegisteredServices(bundle);
        dto.manifestHeaders        = toMap(bundle.getHeaders());
        dto.usedServices           = getUsedServices(bundle);
        dto.hostBundles            = getHostBundles(bundle);
        dto.fragmentsAttached      = getAttachedFragements(bundle);
        dto.revisions              = getBundleRevisions(bundle);
        dto.isPersistentlyStarted  = getPeristentlyStarted(bundle);
        dto.isActivationPolicyUsed = getActivationPolicyUsed(bundle);

        return dto;
    }

    private static long getStorageSize(final Bundle bundle) {
        final BundleContext bundleContext = bundle.getBundleContext();
        final String        bsn           = bundle.getSymbolicName();
        if (bundleContext == null) {
            logger.atDebug().msg("Bundle context is null for '{}'").arg(bsn).log();
            return -1L;
        }
        final File storage = bundleContext.getDataFile("");
        if (storage == null) {
            logger.atDebug().msg("Bundle storage is null for '{}'").arg(bsn).log();
            return -1L;
        }
        return storage.length();
    }

    /**
     * This has been introduced to catch any Exception that might occur while
     * adapting a bundle instance which is not valid anymore, for example, if the
     * bundle is recently uninstalled
     *
     * @param bundle the bundle to adapt
     * @return the start level or -1 if the bundle cannot be adapted
     */
    private static int getStartLevel(final Bundle bundle) {
        try {
            return bundle.adapt(BundleStartLevel.class).getStartLevel();
        } catch (final Exception e) {
            logger.atError().msg("The bundle '{}' cannot be adapted to retrieve start level")
                    .arg(bundle.getSymbolicName()).throwable(e).log();
            return -1;
        }
    }

    private static int getFrameworkStartLevel() {
        try {
            final BundleContext current = FrameworkUtil.getBundle(XBundleAdmin.class).getBundleContext();
            return current.getBundle(SYSTEM_BUNDLE_ID).adapt(FrameworkStartLevelDTO.class).startLevel;
        } catch (final Exception e) {
            logger.atError().msg("The system bundle cannot be adapted to retrieve framework start level").throwable(e)
                    .log();
            return -1;
        }
    }

    private static boolean getActivationPolicyUsed(final Bundle bundle) {
        final BundleStartLevel startLevel = bundle.adapt(BundleStartLevel.class);
        if (startLevel != null) {
            try {
                // In equinox, the following checks if a bundle is valid and if not, equinox
                // throws unchecked exception (for example, if the bundle is uninstalled)
                return startLevel.isActivationPolicyUsed();
            } catch (final Exception e) {
                logger.atError().msg("Invalid state '{}' for bundle '{}'").arg(bundle.getState())
                        .arg(bundle.getSymbolicName()).throwable(e).log();
            }
        }
        return false;
    }

    private static boolean getPeristentlyStarted(final Bundle bundle) {
        final BundleStartLevel startLevel = bundle.adapt(BundleStartLevel.class);
        if (startLevel != null) {
            try {
                // In equinox, the following checks if a bundle is valid and if not, equinox
                // throws unchecked exception (for example, if the bundle is uninstalled)
                return startLevel.isPersistentlyStarted();
            } catch (final Exception e) {
                logger.atError().msg("Invalid state '{}' for bundle '{}'").arg(bundle.getState())
                        .arg(bundle.getSymbolicName()).throwable(e).log();
            }
        }
        return false;
    }

    private static int getBundleRevisions(final Bundle bundle) {
        try {
            final BundleRevisions revisions = bundle.adapt(BundleRevisions.class);
            return revisions.getRevisions().size();
        } catch (final Exception e) {
            logger.atError().msg("The bundle '{}' cannot be adapted to retrieve the revisions")
                    .arg(bundle.getSymbolicName()).throwable(e).log();
            return -1;
        }
    }

    private static List<XBundleInfoDTO> getHostBundles(final Bundle bundle) {
        try {
            final List<XBundleInfoDTO> attachedHosts = new ArrayList<>();
            final BundleWiring         wiring        = bundle.adapt(BundleWiring.class);

            // wiring can be null for non-started installed bundles
            if (wiring == null) {
                return Collections.emptyList();
            }
            for (final BundleWire wire : wiring.getRequiredWires(HOST_NAMESPACE)) {
                final Bundle         b   = wire.getProviderWiring().getBundle();
                final XBundleInfoDTO dto = new XBundleInfoDTO();

                dto.id           = b.getBundleId();
                dto.symbolicName = b.getSymbolicName();

                attachedHosts.add(dto);
            }
            return attachedHosts;
        } catch (final Exception e) {
            logger.atError().msg("The bundle '{}' cannot be adapted to retrieve the host bundles")
                    .arg(bundle.getSymbolicName()).throwable(e).log();
            return Collections.emptyList();
        }
    }

    private static List<XBundleInfoDTO> getAttachedFragements(final Bundle bundle) {
        try {
            final List<XBundleInfoDTO> attachedFragments = new ArrayList<>();
            final BundleWiring         wiring            = bundle.adapt(BundleWiring.class);

            // wiring can be null for non-started installed bundles
            if (wiring == null) {
                return Collections.emptyList();
            }
            for (final BundleWire wire : wiring.getProvidedWires(HOST_NAMESPACE)) {
                final Bundle         b   = wire.getRequirerWiring().getBundle();
                final XBundleInfoDTO dto = new XBundleInfoDTO();

                dto.id           = b.getBundleId();
                dto.symbolicName = b.getSymbolicName();

                attachedFragments.add(dto);
            }
            return attachedFragments;
        } catch (final Exception e) {
            logger.atError().msg("The bundle '{}' cannot be adapted to retrieve the attached fragments")
                    .arg(bundle.getSymbolicName()).throwable(e).log();
            return Collections.emptyList();
        }
    }

    private static List<XServiceInfoDTO> getUsedServices(final Bundle bundle) {
        final List<XServiceInfoDTO> services = new ArrayList<>();
        ServiceReference<?>[]       usedServices;
        try {
            // In equinox, the following checks if a bundle is valid and if not, equinox
            // throws unchecked exception (for example, if the bundle is uninstalled)
            usedServices = bundle.getServicesInUse();
        } catch (final Exception e) {
            logger.atWarn().msg("Invalid state '{}' for bundle '{}'").arg(bundle.getState())
                    .arg(bundle.getSymbolicName()).log();
            usedServices = new ServiceReference<?>[0];
        }
        if (usedServices == null) {
            return Collections.emptyList();
        }
        for (final ServiceReference<?> service : usedServices) {
            final String[]     objectClass = (String[]) service.getProperty(OBJECTCLASS);
            final List<String> objectClazz = Arrays.asList(objectClass);

            services.addAll(prepareServiceInfo(service, objectClazz));
        }
        return services;
    }

    private static List<XServiceInfoDTO> getRegisteredServices(final Bundle bundle) {
        final List<XServiceInfoDTO> services = new ArrayList<>();
        ServiceReference<?>[]       registeredServices;
        try {
            // In equinox, the following checks if a bundle is valid and if not, equinox
            // throws unchecked exception (for example, if the bundle is uninstalled)
            registeredServices = bundle.getRegisteredServices();
        } catch (final Exception e) {
            logger.atWarn().msg("Invalid state '{}' for bundle '{}'").arg(bundle.getState())
                    .arg(bundle.getSymbolicName()).log();
            registeredServices = new ServiceReference<?>[0];
        }
        if (registeredServices == null) {
            return Collections.emptyList();
        }
        for (final ServiceReference<?> service : registeredServices) {
            final String[]     objectClasses = (String[]) service.getProperty(OBJECTCLASS);
            final List<String> objectClazz   = Arrays.asList(objectClasses);

            services.addAll(prepareServiceInfo(service, objectClazz));
        }
        return services;
    }

    private static List<XServiceInfoDTO> prepareServiceInfo(final ServiceReference<?> service,
                                                            final List<String> objectClazz) {
        final List<XServiceInfoDTO> serviceInfos = new ArrayList<>();
        for (final String clz : objectClazz) {
            final XServiceInfoDTO dto = new XServiceInfoDTO();

            dto.id          = Long.parseLong(service.getProperty(SERVICE_ID).toString());
            dto.objectClass = clz;

            serviceInfos.add(dto);
        }
        return serviceInfos;
    }

    private static List<XBundleInfoDTO> getWiredBundlesAsProvider(final Bundle bundle) {
        try {
            final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            if (bundleWiring == null) {
                return Collections.emptyList();
            }
            final List<XBundleInfoDTO> bundles       = new ArrayList<>();
            final List<BundleWire>     providedWires = bundleWiring.getProvidedWires(null);

            for (final BundleWire wire : providedWires) {
                final BundleRevision requirer = wire.getRequirer();
                final XBundleInfoDTO dto      = new XBundleInfoDTO();

                dto.id           = requirer.getBundle().getBundleId();
                dto.symbolicName = requirer.getSymbolicName();

                if (!containsWire(bundles, dto.symbolicName, dto.id)) {
                    bundles.add(dto);
                }
            }
            return bundles;
        } catch (final Exception e) {
            logger.atError().msg("The bundle '{}' cannot be adapted to retrieve the wired bundles as provider")
                    .arg(bundle.getSymbolicName()).throwable(e).log();
            return Collections.emptyList();
        }
    }

    private static List<XBundleInfoDTO> getWiredBundlesAsRequirer(final Bundle bundle) {
        try {
            final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            if (bundleWiring == null) {
                return Collections.emptyList();
            }
            final List<XBundleInfoDTO> bundles       = new ArrayList<>();
            final List<BundleWire>     requierdWires = bundleWiring.getRequiredWires(null);

            for (final BundleWire wire : requierdWires) {
                final BundleRevision provider = wire.getProvider();
                final XBundleInfoDTO dto      = new XBundleInfoDTO();

                dto.id           = provider.getBundle().getBundleId();
                dto.symbolicName = provider.getSymbolicName();

                if (!containsWire(bundles, dto.symbolicName, dto.id)) {
                    bundles.add(dto);
                }
            }
            return bundles;
        } catch (final Exception e) {
            logger.atError().msg("The bundle '{}' cannot be adapted to retrieve the wired bundles as requirer")
                    .arg(bundle.getSymbolicName()).throwable(e).log();
            return Collections.emptyList();
        }
    }

    private static boolean containsWire(final List<XBundleInfoDTO> bundles, final String bsn, final long id) {
        return bundles.stream().anyMatch(b -> b.symbolicName.equals(bsn) && b.id == id);
    }

    private static List<XPackageDTO> getImportedPackages(final Bundle bundle) {
        try {
            final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            if (bundleWiring == null) {
                return Collections.emptyList();
            }
            final List<BundleWire>  bundleWires      = bundleWiring.getRequiredWires(PACKAGE_NAMESPACE);
            final List<XPackageDTO> importedPackages = new ArrayList<>();

            for (final BundleWire bundleWire : bundleWires) {
                final Map<String, Object> attributes = bundleWire.getCapability().getAttributes();
                final String              pkg        = (String) attributes.get(PACKAGE_NAMESPACE);
                final String              version    = attributes.get(VERSION_ATTRIBUTE).toString();

                if (!hasPackage(importedPackages, pkg, version)) {
                    final XPackageDTO dto = new XPackageDTO();

                    dto.name    = pkg;
                    dto.version = version;
                    dto.type    = XpackageType.IMPORT;

                    importedPackages.add(dto);
                }
            }
            return importedPackages;
        } catch (final Exception e) {
            logger.atError().msg("The bundle '{}' cannot be adapted to retrieve the imported packages")
                    .arg(bundle.getSymbolicName()).throwable(e).log();
            return Collections.emptyList();
        }
    }

    private static List<XPackageDTO> getExportedPackages(final Bundle bundle) {
        try {
            final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            if (bundleWiring == null) {
                return Collections.emptyList();
            }
            final List<BundleWire>  bundleWires      = bundleWiring.getProvidedWires(PACKAGE_NAMESPACE);
            final List<XPackageDTO> exportedPackages = new ArrayList<>();

            for (final BundleWire bundleWire : bundleWires) {
                final Map<String, Object> attributes = bundleWire.getCapability().getAttributes();
                final String              pkg        = (String) attributes.get(PACKAGE_NAMESPACE);
                final String              version    = attributes.get(VERSION_ATTRIBUTE).toString();

                if (!hasPackage(exportedPackages, pkg, version)) {
                    final XPackageDTO dto = new XPackageDTO();

                    dto.name    = pkg;
                    dto.version = version;
                    dto.type    = XpackageType.EXPORT;

                    exportedPackages.add(dto);
                }
            }
            return exportedPackages;
        } catch (final Exception e) {
            logger.atError().msg("The bundle '{}' cannot be adapted to retrieve the exported packages")
                    .arg(bundle.getSymbolicName()).throwable(e).log();
            return Collections.emptyList();
        }
    }

    private static boolean hasPackage(final List<XPackageDTO> packages, final String name, final String version) {
        return packages.stream().anyMatch(o -> o.name.equals(name) && o.version.equals(version));
    }

    private static String findState(final int state) {
        switch (state) {
            case ACTIVE:
                return "ACTIVE";
            case INSTALLED:
                return "INSTALLED";
            case RESOLVED:
                return "RESOLVED";
            case STARTING:
                return "STARTING";
            case STOPPING:
                return "STOPPING";
            case UNINSTALLED:
                return "UNINSTALLED";
            default:
                break;
        }
        return null;
    }

    private static String getHeader(final Bundle bundle, final String header) {
        final Map<String, String> headers = toMap(bundle.getHeaders());
        return headers.get(header);
    }

    private static Map<String, String> toMap(final Dictionary<String, String> dictionary) {
        final List<String> keys = Collections.list(dictionary.keys());
        return keys.stream().collect(Collectors.toMap(identity(), dictionary::get));
    }

}
