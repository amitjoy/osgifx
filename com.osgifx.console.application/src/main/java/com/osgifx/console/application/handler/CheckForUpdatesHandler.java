/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.application.handler;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;

import com.google.common.collect.Lists;
import com.osgifx.console.application.dialog.CheckForUpdatesDialog;
import com.osgifx.console.application.dialog.CheckForUpdatesDialog.SelectedFeaturesForUpdateDTO;
import com.osgifx.console.feature.FeatureDTO;
import com.osgifx.console.feature.IdDTO;
import com.osgifx.console.update.UpdateAgent;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.update.UpdateAgent)")
public final class CheckForUpdatesHandler {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private IEclipseContext   context;
    @Inject
    private UpdateAgent       updateAgent;
    @Inject
    private ThreadSynchronize threadSync;
    private ProgressDialog    updateProgressDialog;
    private ProgressDialog    updateCheckProgressDialog;

    @Execute
    public void execute() {
        logger.atInfo().log("Checking for updates");
        final Task<Collection<FeatureDTO>> updateCheckTask = new Task<Collection<FeatureDTO>>() {
            @Override
            protected Collection<FeatureDTO> call() throws Exception {
                final Collection<FeatureDTO> tobeUpdatedFeatures = updateAgent.checkForUpdates();
                if (tobeUpdatedFeatures.isEmpty()) {
                    threadSync.asyncExec(() -> {
                        updateCheckProgressDialog.close();
                        FxDialog.showInfoDialog("Check for Updates", "No update found", getClass().getClassLoader());
                    });
                }
                return tobeUpdatedFeatures;
            }

            @Override
            protected void succeeded() {
                threadSync.asyncExec(() -> updateCheckProgressDialog.close());
            }
        };
        updateCheckProgressDialog = FxDialog.showProgressDialog("Checking for updates", updateCheckTask, getClass().getClassLoader());
        final Thread thread = new Thread(updateCheckTask);
        thread.setDaemon(true);
        thread.start();

        updateCheckTask.setOnSucceeded(t -> {
            final CheckForUpdatesDialog dialog = new CheckForUpdatesDialog();
            ContextInjectionFactory.inject(dialog, context);
            dialog.init();
            logger.atDebug().log("Injected check for updates dialog to eclipse context");

            final Collection<FeatureDTO> featuresToBeUpdated = updateCheckTask.getValue();
            if (featuresToBeUpdated.isEmpty()) {
                return;
            }
            dialog.initFeaturesToBeUpdated(featuresToBeUpdated);
            final Optional<SelectedFeaturesForUpdateDTO> selectedFeatures = dialog.showAndWait();
            Task<Void>                                   updateTask       = null;
            if (selectedFeatures.isPresent()) {
                updateTask = new Task<Void>() {

                    @Override
                    protected Void call() throws Exception {
                        final SelectedFeaturesForUpdateDTO selected                    = selectedFeatures.get();
                        final List<FeatureDTO>             successfullyUpdatedFeatures = Lists.newArrayList();
                        final List<FeatureDTO>             notUpdatedFeatures          = Lists.newArrayList();
                        for (final FeatureDTO entry : selected.features) {
                            final String repoURL = entry.archiveURL;
                            try {
                                final Entry<File, FeatureDTO> feature = updateAgent.readFeature(new URL(repoURL),
                                        featureIdAsString(entry.id));
                                logger.atInfo().log("Processing update: '%s'", feature.getValue());
                                updateAgent.updateOrInstall(feature.getKey(), repoURL);
                                successfullyUpdatedFeatures.add(entry);
                            } catch (final Exception e) {
                                logger.atError().withException(e).log("Cannot check for updates");
                                notUpdatedFeatures.add(entry);
                            }
                        }
                        threadSync.asyncExec(() -> {
                            updateProgressDialog.close();
                            final String header = "Feature Update";

                            final StringBuilder builder = new StringBuilder();
                            if (!successfullyUpdatedFeatures.isEmpty()) {
                                builder.append("Successfully updated features: ");
                                builder.append(System.lineSeparator());
                                for (final FeatureDTO feature : successfullyUpdatedFeatures) {
                                    builder.append(featureIdAsString(feature.id));
                                    builder.append(System.lineSeparator());
                                }
                            } else {
                                builder.append("No features updated");
                                builder.append(System.lineSeparator());
                            }
                            if (!notUpdatedFeatures.isEmpty()) {
                                builder.append("Features not updated: ");
                                builder.append(System.lineSeparator());
                                for (final FeatureDTO feature : notUpdatedFeatures) {
                                    builder.append(featureIdAsString(feature.id));
                                    builder.append(System.lineSeparator());
                                }
                            }
                            if (!notUpdatedFeatures.isEmpty()) {
                                FxDialog.showWarningDialog(header, builder.toString(), getClass().getClassLoader());
                            } else {
                                FxDialog.showInfoDialog(header, builder.toString(), getClass().getClassLoader());
                            }
                        });
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        threadSync.asyncExec(() -> updateProgressDialog.close());
                    }
                };
            }
            if (updateTask != null) {
                updateProgressDialog = FxDialog.showProgressDialog("Updating Features", updateTask, getClass().getClassLoader());
                final Thread th = new Thread(updateTask);
                th.setDaemon(true);
                th.start();
            }
        });
    }

    private String featureIdAsString(final IdDTO id) {
        return id.groupId + ":" + id.artifactId + ":" + id.version;
    }

}
