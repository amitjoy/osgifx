package in.bytehue.osgifx.console.agent.provider;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;
import static org.osgi.framework.wiring.BundleRevision.PACKAGE_NAMESPACE;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;
import in.bytehue.osgifx.console.agent.dto.XPackageDTO;
import in.bytehue.osgifx.console.agent.dto.XPackageDTO.XpackageType;
import in.bytehue.osgifx.console.agent.dto.XServiceInfoDTO;

public final class XBundleInfoProvider {

    private XBundleInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XBundleDTO> get(final BundleContext context) {
        return Stream.of(context.getBundles()).map(XBundleInfoProvider::toDTO).collect(toList());
    }

    private static XBundleDTO toDTO(final Bundle bundle) {
        final XBundleDTO dto = new XBundleDTO();

        dto.id                 = bundle.getBundleId();
        dto.state              = findState(bundle.getState());
        dto.symbolicName       = bundle.getSymbolicName();
        dto.version            = bundle.getVersion().toString();
        dto.location           = bundle.getLocation();
        dto.category           = getHeader(bundle, Constants.BUNDLE_CATEGORY);
        dto.isFragment         = getHeader(bundle, Constants.FRAGMENT_HOST) != null;
        dto.lastModified       = LocalDateTime.ofInstant(Instant.ofEpochMilli(bundle.getLastModified()), TimeZone.getDefault().toZoneId());
        dto.documentation      = getHeader(bundle, Constants.BUNDLE_DOCURL);
        dto.vendor             = getHeader(bundle, Constants.BUNDLE_VENDOR);
        dto.description        = getHeader(bundle, Constants.BUNDLE_DESCRIPTION);
        dto.startLevel         = bundle.adapt(BundleStartLevel.class).getStartLevel();
        dto.exportedPackages   = getExportedPackages(bundle);
        dto.importedPackages   = getImportedPackages(bundle);
        dto.wiredBundles       = getWiredBundles(bundle);
        dto.registeredServices = getRegisteredServices(bundle);
        dto.manifestHeaders    = toMap(bundle.getHeaders());
        dto.usedServices       = getUsedServices(bundle);
        dto.hostBundles        = getHostBundles(bundle);
        dto.fragmentsAttached  = getAttachedFragements(bundle);

        return dto;
    }

    private static List<XBundleInfoDTO> getHostBundles(final Bundle bundle) {
        final List<XBundleInfoDTO> attachedHosts = new ArrayList<>();
        final BundleWiring         wiring        = bundle.adapt(BundleWiring.class);

        for (final BundleWire wire : wiring.getRequiredWires(HOST_NAMESPACE)) {
            final Bundle         b   = wire.getProviderWiring().getBundle();
            final XBundleInfoDTO dto = new XBundleInfoDTO();

            dto.id  = b.getBundleId();
            dto.symbolicName = b.getSymbolicName();
            attachedHosts.add(dto);
        }
        return attachedHosts;
    }

    private static List<XBundleInfoDTO> getAttachedFragements(final Bundle bundle) {
        final List<XBundleInfoDTO> attachedFragments = new ArrayList<>();
        final BundleWiring         wiring            = bundle.adapt(BundleWiring.class);

        for (final BundleWire wire : wiring.getProvidedWires(HOST_NAMESPACE)) {
            final Bundle         b   = wire.getRequirerWiring().getBundle();
            final XBundleInfoDTO dto = new XBundleInfoDTO();

            dto.id  = b.getBundleId();
            dto.symbolicName = b.getSymbolicName();

            attachedFragments.add(dto);
        }
        return attachedFragments;
    }

    private static List<XServiceInfoDTO> getUsedServices(final Bundle bundle) {
        final List<XServiceInfoDTO> services     = new ArrayList<>();
        final ServiceReference<?>[] usedServices = bundle.getServicesInUse();

        for (final ServiceReference<?> service : usedServices) {
            final XServiceInfoDTO dto = new XServiceInfoDTO();

            dto.id          = Long.parseLong(service.getProperty(SERVICE_ID).toString());
            dto.objectClass = service.getProperty(OBJECTCLASS).toString();

            services.add(dto);
        }
        return services;
    }

    private static List<XServiceInfoDTO> getRegisteredServices(final Bundle bundle) {
        final List<XServiceInfoDTO> services           = new ArrayList<>();
        final ServiceReference<?>[] registeredServices = bundle.getRegisteredServices();

        for (final ServiceReference<?> service : registeredServices) {
            final XServiceInfoDTO dto = new XServiceInfoDTO();

            dto.id          = Long.parseLong(service.getProperty(SERVICE_ID).toString());
            dto.objectClass = service.getProperty(OBJECTCLASS).toString();

            services.add(dto);
        }
        return services;
    }

    private static List<XBundleInfoDTO> getWiredBundles(final Bundle bundle) {
        final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Collections.emptyList();
        }
        final List<XBundleInfoDTO> bundles       = new ArrayList<>();
        final List<BundleWire>     providedWires = bundleWiring.getProvidedWires(null);
        final List<BundleWire>     requierdWires = bundleWiring.getRequiredWires(null);

        for (final BundleWire wire : providedWires) {
            final BundleRevision requirer = wire.getRequirer();
            final XBundleInfoDTO dto      = new XBundleInfoDTO();

            dto.id  = requirer.getBundle().getBundleId();
            dto.symbolicName = requirer.getSymbolicName();

            bundles.add(dto);
        }
        for (final BundleWire wire : requierdWires) {
            final BundleRevision provider = wire.getProvider();
            final XBundleInfoDTO dto      = new XBundleInfoDTO();

            dto.id  = provider.getBundle().getBundleId();
            dto.symbolicName = provider.getSymbolicName();

            bundles.add(dto);
        }
        return bundles;
    }

    private static List<XPackageDTO> getImportedPackages(final Bundle bundle) {
        final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Collections.emptyList();
        }
        final List<BundleWire>  bundleWires      = bundleWiring.getRequiredWires(PACKAGE_NAMESPACE);
        final List<XPackageDTO> importedPackages = new ArrayList<>();

        for (final BundleWire bundleWire : bundleWires) {
            final String pkg     = (String) bundleWire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            final String version = bundleWire.getCapability().getRevision().getVersion().toString();

            final XPackageDTO dto = new XPackageDTO();

            dto.name    = pkg;
            dto.version = version;
            dto.type    = XpackageType.IMPORT;

            importedPackages.add(dto);
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
            final String pkg     = (String) bundleWire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            final String version = bundleWire.getCapability().getRevision().getVersion().toString();

            final XPackageDTO dto = new XPackageDTO();

            dto.name    = pkg;
            dto.version = version;
            dto.type    = XpackageType.EXPORT;

            exportedPackages.add(dto);
        }
        return exportedPackages;
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
