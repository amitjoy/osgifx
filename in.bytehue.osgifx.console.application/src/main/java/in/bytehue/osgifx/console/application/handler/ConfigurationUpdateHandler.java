package in.bytehue.osgifx.console.application.handler;

import static in.bytehue.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURAION_DELETED_EVENT_TOPIC;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.supervisor.Supervisor;

public final class ConfigurationUpdateHandler {

    @Log
    @Inject
    private Logger logger;

    @Inject
    private IEventBroker eventBroker;

    @Inject
    private Supervisor supervisor;

    @Execute
    public void execute(@Named("pid") final String pid, @Named("properties") final String properties) {
        final Agent agent = supervisor.getAgent();
        if (supervisor.getAgent() == null) {
            logger.error("Remote agent cannot be connected");
            return;
        }
        try {
            final Map<String, Object> props = new Gson().fromJson(properties, new TypeToken<Map<String, Object>>() {
            }.getType());
            agent.updateConfiguration(pid, props);
            eventBroker.send(CONFIGURAION_DELETED_EVENT_TOPIC, pid);
        } catch (final Exception e) {
            logger.error("Configuration with PID " + pid + "cannot be updated", e);
        }
    }

}