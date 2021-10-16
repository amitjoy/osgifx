package in.bytehue.osgifx.console.application.handler;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;

import in.bytehue.osgifx.console.application.dialog.AboutApplicationDialog;

public final class AboutApplicationHandler {

    @Inject
    private IEclipseContext context;

    @Execute
    public void execute() {
        final AboutApplicationDialog dialog = new AboutApplicationDialog();
        ContextInjectionFactory.inject(dialog, context);
        dialog.init();
        dialog.show();
    }

}