package in.bytehue.osgifx.console.application.handler;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import in.bytehue.osgifx.console.application.dialog.FeatureInstallDialog;
import in.bytehue.osgifx.console.application.dialog.FeatureInstallDialog.SelectedFeaturesDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;
import in.bytehue.osgifx.console.util.fx.Fx;
import in.bytehue.osgifx.console.util.fx.FxDialog;
import javafx.concurrent.Task;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=in.bytehue.osgifx.console.update.UpdateAgent)")
public final class FeatureInstallHandler {

    @Log
    @Inject
    private FluentLogger        logger;
    @Inject
    private IEclipseContext     context;
    @Inject
    private UpdateAgent         updateAgent;
    @Inject
    private ThreadSynchronize   threadSync;
    @Inject
    @Preference(nodePath = "osgi.fx.feature")
    private IEclipsePreferences preferences;
    private ProgressDialog      progressDialog;
    private AtomicBoolean       hasFeatureInstallationError;

    @Execute
    public void execute() {
        hasFeatureInstallationError = new AtomicBoolean();
        final FeatureInstallDialog dialog = new FeatureInstallDialog();

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
        if (features.isEmpty()) {
            return;
        }
        final Task<Void> task = new Task<Void>() {
                                  @Override
                                  protected Void call() throws Exception {
                                      try {
                                          for (final File feature : features) {
                                              updateAgent.updateOrInstall(feature, archiveURL);
                                              logger.atInfo().log("Feature '%s' has been successfuly installed/updated", feature.getName());
                                          }
                                      } catch (final Exception e) {
                                          hasFeatureInstallationError.set(true);
                                          logger.atError().withException(e).log("Cannot update or install feature");
                                          threadSync.asyncExec(() -> {
                                                                    progressDialog.close();
                                                                    FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                                                                });
                                      }
                                      return null;
                                  }

                                  @Override
                                  protected void succeeded() {
                                      progressDialog.close();
                                      if (!hasFeatureInstallationError.get()) {
                                          final String repo = preferences.get("repos", "");
                                          if (repo.isEmpty()) {
                                              storeURL(archiveURL);
                                          } else {

                                          }
                                          Fx.showSuccessNotification("External Feature Installation", "Successfully installed",
                                                  getClass().getClassLoader());
                                      }
                                  }

                                  private void storeURL(final String archiveURL) {
                                      try {
                                          final String repo = preferences.get("repos", "");
                                          final Gson   gson = new Gson();
                                          Set<String>  toBeStored;
                                          if (repo.isEmpty()) {
                                              toBeStored = Sets.newHashSet(archiveURL);
                                          } else {
                                              toBeStored = gson.fromJson(repo, new TypeToken<HashSet<String>>() {
                                                                    }.getType());
                                              toBeStored.add(archiveURL);
                                          }
                                          final String json = gson.toJson(toBeStored);
                                          preferences.put("repos", json);
                                          preferences.flush();
                                      } catch (final BackingStoreException e) {
                                          logger.atError().withException(e).log("Repo URL cannot be stored");
                                      }
                                  }
                              };
        final Thread     th   = new Thread(task);
        th.setDaemon(true);
        th.start();

        progressDialog = FxDialog.showProgressDialog("External Feature Installation", task, getClass().getClassLoader());
    }

}
