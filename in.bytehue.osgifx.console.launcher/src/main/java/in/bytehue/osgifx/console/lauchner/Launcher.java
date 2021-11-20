package in.bytehue.osgifx.console.lauchner;

import static java.util.Collections.emptyMap;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import in.bytehue.osgifx.console.propertytypes.MainThread;

@Component
@MainThread
public final class Launcher implements Runnable {

    private static final String APPLICATION_ID = "in.bytehue.osgifx.console.application.osgifx";

    @Reference(target = "(" + SERVICE_PID + "=" + APPLICATION_ID + ")", cardinality = OPTIONAL)
    private volatile ApplicationDescriptor applicationDescriptor;

    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public void run() {
        try {
            if (applicationDescriptor == null) {
                logger.atError().log("Application descriptor '%s' not found", APPLICATION_ID);
            }
            logger.atInfo().log("Application descriptor '%s' found", APPLICATION_ID);
            final ApplicationHandle handle = applicationDescriptor.launch(emptyMap());
            handle.getExitValue(0);
        } catch (final ApplicationException e) {
            logger.atError().withException(e).log(e.getMessage());
            throw new RuntimeException(e);
        } catch (final InterruptedException e) {
            logger.atError().withException(e).log(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}