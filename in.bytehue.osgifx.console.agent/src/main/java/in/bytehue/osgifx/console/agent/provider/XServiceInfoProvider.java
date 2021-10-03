package in.bytehue.osgifx.console.agent.provider;

import java.util.List;

import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XServiceDTO;

public final class XServiceInfoProvider {

    private XServiceInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XServiceDTO> get(final BundleContext context) {
        return null;
    }

}
