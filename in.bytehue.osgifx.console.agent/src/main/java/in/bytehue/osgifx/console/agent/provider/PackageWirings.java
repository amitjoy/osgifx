package in.bytehue.osgifx.console.agent.provider;

import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public final class PackageWirings {

    private PackageWirings() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static boolean isWired(final String packageName, final BundleContext context) {
        final BundleWiring wiring = context.getBundle().adapt(BundleWiring.class);
        for (final BundleWire wire : wiring.getRequiredWires(PACKAGE_NAMESPACE)) {
            final String       pkg            = (String) wire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            final BundleWiring providerWiring = wire.getProviderWiring();
            if (pkg.startsWith(packageName) && providerWiring != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isScrWired(final BundleContext context) {
        return PackageWirings.isWired("org.osgi.service.component.runtime", context);
    }

    public static boolean isConfigAdminWired(final BundleContext context) {
        return PackageWirings.isWired("org.osgi.service.cm", context);
    }

    public static boolean isMetatypeWired(final BundleContext context) {
        return PackageWirings.isWired("org.osgi.service.metatype", context);
    }

    public static boolean isEventAdminWired(final BundleContext context) {
        return PackageWirings.isWired("org.osgi.service.event", context);
    }

    public static boolean isLogWired(final BundleContext context) {
        return PackageWirings.isWired("org.osgi.service.log", context);
    }

}
