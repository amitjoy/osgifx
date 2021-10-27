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
import in.bytehue.osgifx.console.agent.dto.XResultDTO;
import in.bytehue.osgifx.console.supervisor.Supervisor;

public final class ConfigurationUpdateHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private IEventBroker eventBroker;
    @Inject
    private Supervisor   supervisor;

    @Execute
    public void execute(@Named("pid") final String pid, @Named("properties") final String properties) {
        final Agent agent = supervisor.getAgent();
        if (supervisor.getAgent() == null) {
            logger.atWarning().log("Remote agent cannot be connected");
            return;
        }
        try {
            final Map<String, Object> props  = new Gson().fromJson(properties, new TypeToken<Map<String, Object>>() {
                                             }.getType());
            final XResultDTO          result = agent.createOrUpdateConfiguration(pid, props);
            if (result.result == XResultDTO.SUCCESS) {
                logger.atInfo().log(result.response);
                eventBroker.send(CONFIGURATION_UPDATED_EVENT_TOPIC, pid);
            } else if (result.result == XResultDTO.SKIPPED) {
                logger.atWarning().log(result.response);
            } else {
                logger.atError().log(result.response);
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Configuration with PID '%s' cannot be updated", pid);
        }
    }

}