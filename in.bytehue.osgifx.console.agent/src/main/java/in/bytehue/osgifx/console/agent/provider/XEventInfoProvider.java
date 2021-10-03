package in.bytehue.osgifx.console.agent.provider;

import java.util.List;

import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XEventDTO;

public final class XEventInfoProvider {

    private XEventInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XEventDTO> get(final BundleContext context) {
        return null;
    }

}
