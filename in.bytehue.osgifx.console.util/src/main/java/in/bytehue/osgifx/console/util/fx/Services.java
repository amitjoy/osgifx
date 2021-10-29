package in.bytehue.osgifx.console.util.fx;

import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public final class Services {

    private Services() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static <T> ServiceRegistration<T> register(final Class<T> type, final T instance, final Object... props) {
        final Hashtable<String, Object> ht = new Hashtable<>();
        for (int i = 0; i < props.length; i += 2) {
            final String key   = (String) props[i];
            Object       value = null;
            if (i + 1 < props.length) {
                value = props[i + 1];
            }
            ht.put(key, value);
        }
        return bundleContext().registerService(type, instance, ht);
    }

    private static BundleContext bundleContext() {
        return FrameworkUtil.getBundle(Services.class).getBundleContext();
    }

}
