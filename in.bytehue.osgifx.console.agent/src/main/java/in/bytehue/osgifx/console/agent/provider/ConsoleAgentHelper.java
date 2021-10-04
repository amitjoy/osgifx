package in.bytehue.osgifx.console.agent.provider;

import static java.util.function.Function.identity;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public final class ConsoleAgentHelper {

    private ConsoleAgentHelper() {
        throw new IllegalAccessError("Cannot be instanntiated");
    }

    public static String bsn(final long id, final BundleContext context) {
        for (final Bundle b : context.getBundles()) {
            if (b.getBundleId() == id) {
                return b.getSymbolicName();
            }
        }
        return null;
    }

    public static Map<String, String> toMap(final Dictionary<String, Object> dictionary) {
        final List<String> keys = Collections.list(dictionary.keys());
        return keys.stream().collect(Collectors.toMap(identity(), v -> dictionary.get(v).toString()));
    }

}
