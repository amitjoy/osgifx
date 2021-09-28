package in.bytehue.osgifx.console.ui.addon;

import javax.annotation.PostConstruct;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class BundleContextAddon {

    @PostConstruct
    public void init(final IEclipseContext eclipseContext) {
        final Bundle        bundle        = FrameworkUtil.getBundle(getClass());
        final BundleContext bundleContext = bundle.getBundleContext();
        eclipseContext.set(bundle.getSymbolicName(), bundleContext);
    }

}
