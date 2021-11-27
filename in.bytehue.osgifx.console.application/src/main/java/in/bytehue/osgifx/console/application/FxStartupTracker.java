package in.bytehue.osgifx.console.application;

import static org.eclipse.fx.ui.services.startup.StartupProgressTrackerService.DefaultProgressState.DI_SYSTEM_INITIALIZED;
import static org.eclipse.fx.ui.services.startup.StartupProgressTrackerService.DefaultProgressState.JAVAFX_INITIALIZED;
import static org.eclipse.fx.ui.services.startup.StartupProgressTrackerService.DefaultProgressState.JAVAFX_INITIALIZED_LAUNCHER_THREAD;
import static org.eclipse.fx.ui.services.startup.StartupProgressTrackerService.DefaultProgressState.LOCATION_CHECK_FAILED;
import static org.eclipse.fx.ui.services.startup.StartupProgressTrackerService.DefaultProgressState.POST_CONTEXT_LF_FINISHED;
import static org.eclipse.fx.ui.services.startup.StartupProgressTrackerService.DefaultProgressState.WORKBENCH_GUI_SHOWING;
import static org.eclipse.fx.ui.services.startup.StartupProgressTrackerService.DefaultProgressState.WORKBENCH_GUI_SHOWN;

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
        if (state == JAVAFX_INITIALIZED) {
            logger.atInfo().log("[StartUp] The JavaFX subsystem has been initialized. This state reached on JavaFX event thread");
        }
        if (state == JAVAFX_INITIALIZED_LAUNCHER_THREAD) {
            logger.atInfo().log("[StartUp] The JavaFX subsystem has been initialized. This state reached on launcher thread.");
        }
        if (state == DI_SYSTEM_INITIALIZED) {
            logger.atInfo().log("[StartUp] The DI-System has been initialized");
        }
        if (state == POST_CONTEXT_LF_FINISHED) {
            logger.atInfo().log("[StartUp] The lifecycle has been finished");
        }
        if (state == WORKBENCH_GUI_SHOWING) {
            logger.atInfo().log("[StartUp] The workbench UI is showing");
        }
        if (state == WORKBENCH_GUI_SHOWN) {
            logger.atInfo().log("[StartUp] The workbench UI is shown");
        }
        if (state == LOCATION_CHECK_FAILED) {
            logger.atInfo().log("[StartUp] State reached when check for workspace could not be locked");
        }
    }

}
