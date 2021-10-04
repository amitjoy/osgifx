package in.bytehue.osgifx.console.agent.provider;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;
import static org.osgi.framework.wiring.BundleRevision.PACKAGE_NAMESPACE;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        dto.lastModified       = bundle.getLastModified();
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

    private static Map<Long, String> getHostBundles(final Bundle bundle) {
        final Map<Long, String> attachedHosts = new HashMap<>();
        final BundleWiring      wiring        = bundle.adapt(BundleWiring.class);

        for (final BundleWire wire : wiring.getRequiredWires(HOST_NAMESPACE)) {
            final Bundle b = wire.getProviderWiring().getBundle();
            attachedHosts.put(b.getBundleId(), b.getSymbolicName());
        }
        return attachedHosts;
    }

    private static Map<Long, String> getAttachedFragements(final Bundle bundle) {
        final Map<Long, String> attachedFragments = new HashMap<>();
        final BundleWiring      wiring            = bundle.adapt(BundleWiring.class);

        for (final BundleWire wire : wiring.getProvidedWires(HOST_NAMESPACE)) {
            final Bundle b = wire.getRequirerWiring().getBundle();
            attachedFragments.put(b.getBundleId(), b.getSymbolicName());
        }
        return attachedFragments;
    }

    private static Map<Long, String> getUsedServices(final Bundle bundle) {
        final Map<Long, String>     services     = new HashMap<>();
        final ServiceReference<?>[] usedServices = bundle.getServicesInUse();

        for (final ServiceReference<?> service : usedServices) {
            services.put(Long.parseLong(service.getProperty(SERVICE_ID).toString()), service.getProperty(OBJECTCLASS).toString());
        }
        return services;
    }

    private static Map<Long, String> getRegisteredServices(final Bundle bundle) {
        final Map<Long, String>     services           = new HashMap<>();
        final ServiceReference<?>[] registeredServices = bundle.getRegisteredServices();

        for (final ServiceReference<?> service : registeredServices) {
            services.put(Long.parseLong(service.getProperty(SERVICE_ID).toString()), service.getProperty(OBJECTCLASS).toString());
        }
        return services;
    }

    private static Map<Long, String> getWiredBundles(final Bundle bundle) {
        final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Collections.emptyMap();
        }
        final Map<Long, String> bundles       = new HashMap<>();
        final List<BundleWire>  providedWires = bundleWiring.getProvidedWires(null);
        final List<BundleWire>  requierdWires = bundleWiring.getRequiredWires(null);

        for (final BundleWire wire : providedWires) {
            final BundleRevision requirer = wire.getRequirer();
            bundles.put(requirer.getBundle().getBundleId(), requirer.getSymbolicName());
        }
        for (final BundleWire wire : requierdWires) {
            final BundleRevision provider = wire.getProvider();
            bundles.put(provider.getBundle().getBundleId(), provider.getSymbolicName());
        }

        return bundles;
    }

    private static Map<String, String> getImportedPackages(final Bundle bundle) {
        final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Collections.emptyMap();
        }
        final List<BundleWire>    bundleWires      = bundleWiring.getRequiredWires(PACKAGE_NAMESPACE);
        final Map<String, String> importedPackages = new HashMap<>();

        for (final BundleWire bundleWire : bundleWires) {
            final String pkg     = (String) bundleWire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            final String version = bundleWire.getCapability().getRevision().getVersion().toString();
            importedPackages.put(pkg, version);
        }
        return importedPackages;
    }

    private static Map<String, String> getExportedPackages(final Bundle bundle) {
        final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return Collections.emptyMap();
        }
        final List<BundleWire>    bundleWires      = bundleWiring.getProvidedWires(PACKAGE_NAMESPACE);
        final Map<String, String> exportedPackages = new HashMap<>();

        for (final BundleWire bundleWire : bundleWires) {
            final String pkg     = (String) bundleWire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            final String version = bundleWire.getCapability().getRevision().getVersion().toString();
            exportedPackages.put(pkg, version);
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
        return keys.stream().collect(Collectors.toMap(identity(), v -> dictionary.get(v)));
    }

}
