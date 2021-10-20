package in.bytehue.osgifx.console.core.log.handler;

import org.eclipse.fx.core.ExceptionHandler;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public final class UncaughtExceptionHandler implements ExceptionHandler {

    @Reference
    private LoggerFactory factory;
    private FluentLogger  logger;

    @Activate
    void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Override
    public void handleException(final ExceptionData data) {
        logger.atError().withException(data.throwable()).log("Uncaught Exception");
    }

}
