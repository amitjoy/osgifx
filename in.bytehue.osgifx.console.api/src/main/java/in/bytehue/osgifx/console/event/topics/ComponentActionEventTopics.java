package in.bytehue.osgifx.console.event.topics;

public final class ComponentActionEventTopics {

    private ComponentActionEventTopics() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static final String COMPONENT_ACTION_EVENT_TOPICS  = "osgi/fx/console/component/*";
    public static final String COMPONENT_ENABLED_EVENT_TOPIC  = "osgi/fx/console/component/enabled";
    public static final String COMPONENT_DISABLED_EVENT_TOPIC = "osgi/fx/console/component/disabled";

}
