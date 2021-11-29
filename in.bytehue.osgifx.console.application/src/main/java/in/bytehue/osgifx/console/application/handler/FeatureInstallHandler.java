package in.bytehue.osgifx.console.application.handler;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.service.prefs.BackingStoreException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import in.bytehue.osgifx.console.application.dialog.FeatureInstallDialog;
import in.bytehue.osgifx.console.application.dialog.FeatureInstallDialog.SelectedFeaturesDTO;
import in.bytehue.osgifx.console.feature.FeatureDTO;
import in.bytehue.osgifx.console.feature.IdDTO;
import in.bytehue.osgifx.console.update.UpdateAgent;
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
    private IWorkbench          workbench;
    @Inject
    @Preference(nodePath = "osgi.fx.feature")
    private IEclipsePreferences preferences;
    private ProgressDialog      progressDialog;

    @Execute
    public void execute() {
        final FeatureInstallDialog dialog = new FeatureInstallDialog();

        ContextInjectionFactory.inject(dialog, context);
        logger.atDebug().log("Injected install feature dialog to eclipse context");
        dialog.init();

        final Optional<SelectedFeaturesDTO> selectedFeatures = dialog.showAndWait();
        if (selectedFeatures.isPresent()) {
            final SelectedFeaturesDTO selected = selectedFeatures.get();
            udpateOrInstallFeatures(selected.features, selected.archiveURL);
        }
    }

    private void udpateOrInstallFeatures(final List<Entry<File, FeatureDTO>> features, final String archiveURL) {
        if (features.isEmpty()) {
            return;
        }
        final Task<Void> task = new Task<Void>() {
            final List<FeatureDTO> successfullyInstalledFeatures = Lists.newArrayList();
            final List<FeatureDTO> notInstalledFeatures          = Lists.newArrayList();

            @Override
            protected Void call() throws Exception {
                for (final Entry<File, FeatureDTO> feature : features) {
                    final FeatureDTO f  = feature.getValue();
                    final String     id = featureIdAsString(f.id);
                    try {
                        updateAgent.updateOrInstall(feature.getKey(), archiveURL);
                        successfullyInstalledFeatures.add(f);
                        logger.atInfo().log("Feature '%s' has been successfuly installed/updated", id);
                    } catch (final Exception e) {
                        notInstalledFeatures.add(f);
                        logger.atError().withException(e).log("Cannot update or install feature '%s'", id);
                    }
                }
                return null;
            }

            @Override
            protected void succeeded() {
                progressDialog.close();
                final String header = "Feature Installation";

                final StringBuilder builder = new StringBuilder();
                if (!successfullyInstalledFeatures.isEmpty()) {
                    builder.append("Successfully installed features: ");
                    builder.append(System.lineSeparator());
                    for (final FeatureDTO feature : successfullyInstalledFeatures) {
                        builder.append(featureIdAsString(feature.id));
                        builder.append(System.lineSeparator());
                    }
                } else {
                    builder.append("No features updated");
                    builder.append(System.lineSeparator());
                }
                if (!notInstalledFeatures.isEmpty()) {
                    builder.append("Features not updated: ");
                    builder.append(System.lineSeparator());
                    for (final FeatureDTO feature : notInstalledFeatures) {
                        builder.append(featureIdAsString(feature.id));
                        builder.append(System.lineSeparator());
                    }
                }
                if (!notInstalledFeatures.isEmpty()) {
                    logger.atWarning().log("Some of the features successfully installed and some not");
                    FxDialog.showWarningDialog(header, builder.toString(), getClass().getClassLoader());
                } else {
                    logger.atInfo().log("All features successfully installed");
                    storeURL(archiveURL);
                    FxDialog.showInfoDialog(header, builder.toString(), getClass().getClassLoader());
                    // show information about the restart of the application
                    FxDialog.showInfoDialog(header, "The application must be restarted, therefore, will be shut down right away",
                            getClass().getClassLoader(), btn -> workbench.restart());
                }
            }

            private void storeURL(final String archiveURL) {
                if (!isWebURL(archiveURL)) {
                    return;
                }
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
                    threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                }
            }
        };

        final Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();

        progressDialog = FxDialog.showProgressDialog("External Feature Installation", task, getClass().getClassLoader());
    }

    private static boolean isWebURL(final String url) {
        try {
            new URL(url);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    private String featureIdAsString(final IdDTO id) {
        return id.groupId + ":" + id.artifactId + ":" + id.version;
    }

}