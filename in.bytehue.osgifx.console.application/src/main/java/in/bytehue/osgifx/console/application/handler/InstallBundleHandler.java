package in.bytehue.osgifx.console.application.handler;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;

import com.google.common.io.Files;

import in.bytehue.osgifx.console.agent.Agent;
import in.bytehue.osgifx.console.application.dialog.InstallBundleDTO;
import in.bytehue.osgifx.console.application.dialog.InstallBundleDialog;
import in.bytehue.osgifx.console.supervisor.Supervisor;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXMLLoader;

public final class InstallBundleHandler {

    @Inject
    @Named("in.bytehue.osgifx.console.application")
    private BundleContext context;

    @Inject
    private Supervisor supervisor;

    @Execute
    public void execute(@LocalInstance final FXMLLoader loader) {
        final InstallBundleDialog        dialog        = new InstallBundleDialog(loader, context);
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