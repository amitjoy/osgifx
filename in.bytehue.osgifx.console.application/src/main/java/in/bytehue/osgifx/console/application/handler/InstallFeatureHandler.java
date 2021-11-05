package in.bytehue.osgifx.console.application.handler;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;

import in.bytehue.osgifx.console.application.dialog.InstallFeatureDialog;
import in.bytehue.osgifx.console.application.dialog.InstallFeatureDialog.SelectedFeaturesDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.concurrent.Task;
import javafx.stage.StageStyle;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=in.bytehue.osgifx.console.update.UpdateAgent)")
public final class InstallFeatureHandler {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private IEclipseContext   context;
    @Inject
    private UpdateAgent       updateAgent;
    @Inject
    private ThreadSynchronize threadSync;
    private ProgressDialog    progressDialog;
    private AtomicBoolean     noFeatureInstallationError;

    @Execute
    public void execute() {
        noFeatureInstallationError = new AtomicBoolean();
        final InstallFeatureDialog dialog = new InstallFeatureDialog();

        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected install feature dialog to eclipse context");
        dialog.init();

        final Optional<SelectedFeaturesDTO> selectedFeatures = dialog.showAndWait();
        if (selectedFeatures.isPresent()) {
            final SelectedFeaturesDTO selected = selectedFeatures.get();
            udpateOrInstallFeatures(selected.features, selected.archiveURL);
        }
    }

    private void udpateOrInstallFeatures(final List<File> features, final String archiveURL) {
        final Task<Void> task = new Task<Void>() {
                                  @Override
                                  protected Void call() throws Exception {
                                      try {
                                          for (final File feature : features) {
                                              updateAgent.updateOrInstall(feature, archiveURL);
                                              logger.atInfo().log("Feature '%s' has been successfuly installed/updated", feature.getName());
                                          }
                                      } catch (final Exception e) {
                                          noFeatureInstallationError.set(true);
                                          logger.atError().withException(e).log("Cannot update or install feature");
                                          threadSync.asyncExec(() -> {
                                                                    progressDialog.close();
                                                                    final ExceptionDialog dialog = new ExceptionDialog(e);
                                                                    dialog.initStyle(StageStyle.UNDECORATED);
                                                                    dialog.getDialogPane().getStylesheets().add(
                                                                            getClass().getResource("/css/default.css").toExternalForm());
                                                                    dialog.show();
                                                                });
                                      }
                                      return null;
                                  }

                                  @Override
                                  protected void succeeded() {
                                      progressDialog.close();
                                      if (!noFeatureInstallationError.get()) {
                                          Fx.showSuccessNotification("Remote Feature Installation", "Successfully installed",
                                                  getClass().getClassLoader());
                                      }
                                  }
                              };
        final Thread     th   = new Thread(task);
        th.setDaemon(true);
        th.start();

        createProgressDialog(task);
    }

    private void createProgressDialog(final Task<?> task) {
        progressDialog = new ProgressDialog(task);
        progressDialog.setHeaderText("External Feature Installation");
        progressDialog.initStyle(StageStyle.UNDECORATED);
        progressDialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/default.css").toExternalForm());
        progressDialog.show();
    }

}
