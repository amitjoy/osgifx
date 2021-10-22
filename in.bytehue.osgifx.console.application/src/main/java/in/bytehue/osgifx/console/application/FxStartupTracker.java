package in.bytehue.osgifx.console.application;

import org.eclipse.fx.core.app.ApplicationContext;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.eclipse.fx.ui.services.startup.StartupProgressTrackerService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public final class FxStartupTracker implements StartupProgressTrackerService {

    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;

    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public OSGiRV applicationLaunched(final ApplicationContext applicationContext) {
        return StartupProgressTrackerService.OSGiRV.EXIT;
    }

    @Override
    public void stateReached(final ProgressState state) {
        logger.atInfo().log("Current state: %s", state.toString());
    }

}
