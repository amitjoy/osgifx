package in.bytehue.osgifx.console.application.handler;

import static in.bytehue.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_DELETED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.agent.dto.XResultDTO;
import in.bytehue.osgifx.console.supervisor.Supervisor;
import in.bytehue.osgifx.console.util.fx.FxDialog;

public final class ConfigurationDeleteHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private IEventBroker eventBroker;
    @Inject
    private Supervisor   supervisor;

    @Execute
    public void execute(@Named("pid") final String pid) {
        final Agent agent = supervisor.getAgent();
        if (supervisor.getAgent() == null) {
            logger.atWarning().log("Remote agent cannot be connected");
            return;
        }
        try {
            final XResultDTO result = agent.deleteConfiguration(pid);
            if (result.result == XResultDTO.SUCCESS) {
                logger.atInfo().log(result.response);
                eventBroker.send(CONFIGURATION_DELETED_EVENT_TOPIC, pid);
            } else if (result.result == XResultDTO.SKIPPED) {
                logger.atWarning().log(result.response);
            } else {
                logger.atError().log(result.response);
                FxDialog.showErrorDialog("Configuration Delete Error", result.response, getClass().getClassLoader());
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Configuration with PID '%s' cannot be deleted", pid);
            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
        }
    }

}