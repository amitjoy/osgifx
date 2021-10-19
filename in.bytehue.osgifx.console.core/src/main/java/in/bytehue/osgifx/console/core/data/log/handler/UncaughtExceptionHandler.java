package in.bytehue.osgifx.console.core.data.log.handler;

import org.eclipse.fx.core.ExceptionHandler;
import org.eclipse.fx.core.log.Logger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public final class UncaughtExceptionHandler implements ExceptionHandler {

    @Reference
    private LoggerFactory factory;
    private Logger        logger;

    void activate() {
        logger = factory.createLogger(getClass().getName());
    }

    @Override
    public void handleException(final ExceptionData data) {
        logger.error("Uncaught Exception", data.throwable());
    }

}
