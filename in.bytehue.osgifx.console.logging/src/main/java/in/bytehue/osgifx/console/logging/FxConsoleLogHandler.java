package in.bytehue.osgifx.console.logging;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(service = Handler.class)
public final class FxConsoleLogHandler extends Handler {

    private static final String LOG_FILE_NAME = "log.txt";

    private final FileHandler        fileHandler;
    private final CustomLogFormatter formatter;

    @Activate
    public FxConsoleLogHandler(final BundleContext context) throws SecurityException, IOException, URISyntaxException {
        String area = context.getProperty("osgi.instance.area.default");

        // remove the prefix
        final String prefix = "file:";
        area = area.substring(area.indexOf(prefix) + prefix.length());

        final String path         = new URI(area).getPath();
        final File   parent       = new File(path);
        final File   logDirectory = new File(parent, "log");

        // create missing directories
        logDirectory.mkdirs();

        formatter   = new CustomLogFormatter();
        fileHandler = new FileHandler(logDirectory.getPath() + "/" + LOG_FILE_NAME, true);
        fileHandler.setFormatter(formatter);
    }

    @Override
    public void publish(final LogRecord logRecord) {
        fileHandler.publish(logRecord);
    }

    @Override
    public void flush() {
        fileHandler.flush();
    }

    @Override
    public void close() throws SecurityException {
        fileHandler.close();
    }

}
