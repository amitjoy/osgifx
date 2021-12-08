package in.bytehue.osgifx.console.application.handler;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

public final class OpenDiagnosticsHandler {

    @Log
    @Inject
    private FluentLogger  logger;
    @Inject
    @Named("in.bytehue.osgifx.console.application")
    private BundleContext context;

    @Execute
    public void execute() {
        String area = context.getProperty("osgi.instance.area.default");
        // remove the prefix
        final String prefix = "file:";
        area = area.substring(area.indexOf(prefix) + prefix.length());
        try {
            Desktop.getDesktop().open(new File(area, "./log/log.txt"));
        } catch (final IOException e) {
            logger.atError().withException(e).log("Cannot open diagnostics file");
        }
    }

}