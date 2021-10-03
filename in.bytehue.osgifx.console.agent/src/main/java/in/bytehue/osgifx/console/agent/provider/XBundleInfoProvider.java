package in.bytehue.osgifx.console.agent.provider;

import java.util.List;

import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;

public final class XBundleInfoProvider {

    private XBundleInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XBundleDTO> get(final BundleContext context) {
        return null;
    }

}
