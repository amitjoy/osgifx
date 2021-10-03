package in.bytehue.osgifx.console.agent.provider;

import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;

import in.bytehue.osgifx.console.agent.dto.XComponentDTO;

public final class XComponentInfoProvider {

    private XComponentInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XComponentDTO> get(final BundleContext context, final ServiceComponentRuntime scr) {
        return null;
    }

}
