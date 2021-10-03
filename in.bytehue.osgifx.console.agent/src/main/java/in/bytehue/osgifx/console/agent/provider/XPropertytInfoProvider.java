package in.bytehue.osgifx.console.agent.provider;

import java.util.List;

import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;

public final class XPropertytInfoProvider {

    private XPropertytInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XPropertyDTO> get(final BundleContext context) {
        return null;
    }

}
