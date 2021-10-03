package in.bytehue.osgifx.console.agent.provider;

import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;

public final class XConfigurationtInfoProvider {

    private XConfigurationtInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XConfigurationDTO> get(final BundleContext context, final ConfigurationAdmin configAdmin) {
        return null;
    }

}
