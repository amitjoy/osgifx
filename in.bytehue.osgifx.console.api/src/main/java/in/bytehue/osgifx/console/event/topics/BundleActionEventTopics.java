package in.bytehue.osgifx.console.event.topics;

public final class BundleActionEventTopics {

    private BundleActionEventTopics() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static final String BUNDLE_ACTION_EVENT_TOPICS     = "osgi/fx/console/bundle/*";
    public static final String BUNDLE_STARTED_EVENT_TOPIC     = "osgi/fx/console/bundle/started";
    public static final String BUNDLE_STOPPED_EVENT_TOPIC     = "osgi/fx/console/bundle/stopped";
    public static final String BUNDLE_INSTALLED_EVENT_TOPIC   = "osgi/fx/console/bundle/installed";
    public static final String BUNDLE_UNINSTALLED_EVENT_TOPIC = "osgi/fx/console/bundle/uninstalled";

}
