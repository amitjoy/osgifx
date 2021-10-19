package in.bytehue.osgifx.console.core.data.log.handler;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public final class FxConsoleHandler extends Handler {

    private final ConsoleHandler consoleHandler;

    @Activate
    public FxConsoleHandler() {
        consoleHandler = new ConsoleHandler();
    }

    @Override
    public void publish(final LogRecord record) {
        consoleHandler.publish(record);
    }

    @Override
    public void flush() {
        consoleHandler.flush();
    }

    @Override
    public void close() throws SecurityException {
        consoleHandler.close();
    }

}
