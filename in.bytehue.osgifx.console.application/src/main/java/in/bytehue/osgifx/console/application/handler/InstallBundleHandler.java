package in.bytehue.osgifx.console.application.handler;

import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;
import org.osgi.framework.dto.BundleDTO;

import com.google.common.io.Files;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.application.dialog.InstallBundleDTO;
import in.bytehue.osgifx.console.application.dialog.InstallBundleDialog;
import in.bytehue.osgifx.console.supervisor.Supervisor;
import in.bytehue.osgifx.console.util.fx.Fx;

public final class InstallBundleHandler {

    @Log
    @Inject
    private Logger          logger;
    @Inject
    private IEclipseContext context;
    @Inject
    private Supervisor      supervisor;

    @Execute
    public void execute() {
        final InstallBundleDialog dialog = new InstallBundleDialog();

        ContextInjectionFactory.inject(dialog, context);
        logger.debug("Injected install bundle dialog to eclipse context");
        dialog.init();

        final Optional<InstallBundleDTO> remoteInstall = dialog.showAndWait();
        if (remoteInstall.isPresent()) {
            try {
                final InstallBundleDTO dto = remoteInstall.get();
                if (dto.file == null) {
                    return;
                }
                final Agent     agent  = supervisor.getAgent();
                final BundleDTO bundle = agent.installWithData(null, Files.toByteArray(dto.file));
                if (dto.startBundle) {
                    logger.info("Bundle has been started: " + bundle);
                    agent.start(bundle.id);
                }
                Fx.showSuccessNotification("Remote Bundle Install", bundle.symbolicName + " successfully installed/updated",
                        getClass().getClassLoader());
            } catch (final Exception e) {
                Fx.showErrorNotification("Remote Bundle Install", "Bundle cannot be installed/updated", getClass().getClassLoader());
            }
        }
    }

}