package in.bytehue.osgifx.console.application.handler;

import static in.bytehue.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_DISABLED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.agent.dto.XResultDTO;
import in.bytehue.osgifx.console.supervisor.Supervisor;

public final class ComponentDisableHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private IEventBroker eventBroker;
    @Inject
    private Supervisor   supervisor;

    @Execute
    public void execute(@Named("id") final String id) {
        final Agent agent = supervisor.getAgent();
        if (supervisor.getAgent() == null) {
            logger.atWarning().log("Remote agent cannot be connected");
            return;
        }
        try {
            final XResultDTO result = agent.disableComponent(Long.parseLong(id));
            if (result.result == XResultDTO.SUCCESS) {
                logger.atInfo().log(result.response);
                eventBroker.send(COMPONENT_DISABLED_EVENT_TOPIC, id);
            } else if (result.result == XResultDTO.SKIPPED) {
                logger.atWarning().log(result.response);
            } else {
                logger.atError().log(result.response);
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Service component with ID '%s' cannot be disabled", id);
        }
    }

}