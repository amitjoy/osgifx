package in.bytehue.osgifx.console.application.handler;

import static in.bytehue.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_UPDATED_EVENT_TOPIC;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.supervisor.Supervisor;

public final class FactoryConfigurationCreateHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private IEventBroker eventBroker;
    @Inject
    private Supervisor   supervisor;

    @Execute
    public void execute(@Named("factoryPID") final String factoryPID, @Named("properties") final String properties) {
        final Agent agent = supervisor.getAgent();
        if (supervisor.getAgent() == null) {
            logger.atWarning().log("Remote agent cannot be connected");
            return;
        }
        try {
            final Map<String, Object> props = new Gson().fromJson(properties, new TypeToken<Map<String, Object>>() {
            }.getType());
            agent.createFactoryConfiguration(factoryPID, props);
            logger.atInfo().log("Factory configuration with factory PID '%s' has been created", factoryPID);
            eventBroker.send(CONFIGURATION_UPDATED_EVENT_TOPIC, factoryPID);
        } catch (final Exception e) {
            logger.atError().log("Factory Configuration with factory PID '%s' cannot be created", factoryPID);
        }
    }

}