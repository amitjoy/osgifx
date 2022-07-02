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
package com.osgifx.console.agent.admin;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.Constants.BUNDLE_CATEGORY;
import static org.osgi.framework.Constants.BUNDLE_DESCRIPTION;
import static org.osgi.framework.Constants.BUNDLE_DOCURL;
import static org.osgi.framework.Constants.BUNDLE_VENDOR;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;
import static org.osgi.framework.wiring.BundleRevision.PACKAGE_NAMESPACE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;

import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleInfoDTO;
import com.osgifx.console.agent.dto.XPackageDTO;
import com.osgifx.console.agent.dto.XPackageDTO.XpackageType;
import com.osgifx.console.agent.dto.XServiceInfoDTO;
import com.osgifx.console.agent.provider.BundleStartTimeCalculator;
import com.osgifx.console.agent.provider.BundleStartTimeCalculator.BundleStartDuration;

public class XBundleAdmin {

    private XBundleAdmin() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XBundleDTO> get(final BundleContext context, final BundleStartTimeCalculator bundleStartTimeCalculator) {
        requireNonNull(context);
        try {
            return Stream.of(context.getBundles()).map(b -> XBundleAdmin.toDTO(b, bundleStartTimeCalculator)).collect(toList());
        } catch (final Exception e) {
            return Collections.emptyList();
        }
    }

    public static XBundleDTO toDTO(final Bundle bundle, final BundleStartTimeCalculator bundleStartTimeCalculator) {
        final XBundleDTO dto = new XBundleDTO();

        dto.id            = bundle.getBundleId();
        dto.state         = findState(bundle.getState());
        dto.symbolicName  = bundle.getSymbolicName();
        dto.version       = bundle.getVersion().toString();
        dto.location      = bundle.getLocation();
        dto.category      = getHeader(bundle, BUNDLE_CATEGORY);
        dto.isFragment    = getHeader(bundle, FRAGMENT_HOST) != null;
        dto.lastModified  = bundle.getLastModified();
        dto.documentation = getHeader(bundle, BUNDLE_DOCURL);
        dto.vendor        = getHeader(bundle, BUNDLE_VENDOR);
        dto.description   = getHeader(bundle, BUNDLE_DESCRIPTION);
        dto.startLevel    = bundle.adapt(BundleStartLevel.class).getStartLevel();
        // @formatter:off
        dto.startDurationInMillis  = bundleStartTimeCalculator.getBundleStartDurations()
                                                              .stream()
                                                              .filter(b -> b.getSymbolicName().equals(dto.symbolicName))
                                                              .map(BundleStartDuration::getStartedAfter)
                                                              .map(Duration::toMillis)
                                                              .findAny()
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

    private static boolean getActivationPolicyUsed(final Bundle bundle) {
        final BundleStartLevel startLevel = bundle.adapt(BundleStartLevel.class);
        if (startLevel != null) {
            return startLevel.isActivationPolicyUsed();
        }
        return false;
    }

    private static boolean getPeristentlyStarted(final Bundle bundle) {
        final BundleStartLevel startLevel = bundle.adapt(BundleStartLevel.class);
        if (startLevel != null) {
            return startLevel.isPersistentlyStarted();
        }
        return false;
    }

    private static int getBundleRevisions(final Bundle bundle) {
        final BundleRevisions revisions = bundle.adapt(BundleRevisions.class);
        return revisions.getRevisions().size();
    }

    private static List<XBundleInfoDTO> getHostBundles(final Bundle bundle) {
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
    }

    private static List<XBundleInfoDTO> getAttachedFragements(final Bundle bundle) {
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
    }

    private static List<XServiceInfoDTO> getUsedServices(final Bundle bundle) {
        final List<XServiceInfoDTO> services     = new ArrayList<>();
        final ServiceReference<?>[] usedServices = bundle.getServicesInUse();

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
        final List<XServiceInfoDTO> services           = new ArrayList<>();
        final ServiceReference<?>[] registeredServices = bundle.getRegisteredServices();

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

    private static List<XServiceInfoDTO> prepareServiceInfo(final ServiceReference<?> service, final List<String> objectClazz) {
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
    }

    private static List<XBundleInfoDTO> getWiredBundlesAsRequirer(final Bundle bundle) {
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
    }

    private static boolean containsWire(final List<XBundleInfoDTO> bundles, final String bsn, final long id) {
        return bundles.stream().anyMatch(b -> b.symbolicName.equals(bsn) && b.id == id);
    }

    private static List<XPackageDTO> getImportedPackages(final Bundle bundle) {
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
    }

    private static List<XPackageDTO> getExportedPackages(final Bundle bundle) {
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
    }

    private static boolean hasPackage(final List<XPackageDTO> packages, final String name, final String version) {
        return packages.stream().anyMatch(o -> o.name.equals(name) && o.version.equals(version));
    }

    private static String findState(final int state) {
        switch (state) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.STARTING:
            return "STARTING";
        case Bundle.STOPPING:
            return "STOPPING";
        case Bundle.UNINSTALLED:
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
