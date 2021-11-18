package in.bytehue.osgifx.console.application.handler;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import in.bytehue.osgifx.console.application.dialog.FeaturesViewDialog;

public final class ViewFeaturesHandler {

    @Log
    @Inject
    private FluentLogger    logger;
    @Inject
    private IEclipseContext context;

    @Execute
    public void execute() {
        final FeaturesViewDialog dialog = new FeaturesViewDialog();
        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected features view dialog to eclipse context");
        dialog.init();
        dialog.show();
    }

}