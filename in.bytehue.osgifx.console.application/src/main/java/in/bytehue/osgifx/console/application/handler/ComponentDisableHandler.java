package in.bytehue.osgifx.console.application.handler;

import static in.bytehue.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_DISABLED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.supervisor.Supervisor;

public final class ComponentDisableHandler {

    @Log
    @Inject
    private Logger       logger;
    @Inject
    private IEventBroker eventBroker;
    @Inject
    private Supervisor   supervisor;

    @Execute
    public void execute(@Named("id") final String id) {
        final Agent agent = supervisor.getAgent();
        if (supervisor.getAgent() == null) {
            logger.error("Remote agent cannot be connected");
            return;
        }
        try {
            final String error = agent.disableComponent(Long.parseLong(id));
            if (error == null) {
                logger.info("Component with ID " + id + " has been disabled");
                eventBroker.send(COMPONENT_DISABLED_EVENT_TOPIC, id);
            } else {
                logger.error(error);
            }
        } catch (final Exception e) {
            logger.error("Service component with ID '" + id + "' cannot be disabled", e);
        }
    }

}