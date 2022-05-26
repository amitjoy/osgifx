/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.ui.feature.handler;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import com.osgifx.console.feature.FeatureDTO;
import com.osgifx.console.feature.IdDTO;
import com.osgifx.console.ui.feature.dialog.CheckForFeatureUpdatesDialog;
import com.osgifx.console.update.FeatureAgent;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.update.FeatureAgent)")
public final class CheckForFeatureUpdatesHandler {

	@Log
	@Inject
	private FluentLogger      logger;
	@Inject
	private IEclipseContext   context;
	@Inject
	private FeatureAgent      featureAgent;
	@Inject
	private ThreadSynchronize threadSync;
	private ProgressDialog    updateProgressDialog;
	private ProgressDialog    updateCheckProgressDialog;

	@Execute
	public void execute() {
		logger.atInfo().log("Checking for updates");
		final Task<Collection<FeatureDTO>> updateCheckTask = new Task<>() {
			@Override
			protected Collection<FeatureDTO> call() throws Exception {
				try {
					final var tobeUpdatedFeatures = featureAgent.checkForFeatureUpdates();
					if (tobeUpdatedFeatures.isEmpty()) {
						threadSync.asyncExec(() -> {
							updateCheckProgressDialog.close();
							FxDialog.showInfoDialog("Check for Updates", "No update found", getClass().getClassLoader());
						});
					}
					return tobeUpdatedFeatures;
				} catch (final InterruptedException e) {
					logger.atInfo().log("Update check task interrupted");
					threadSync.asyncExec(updateCheckProgressDialog::close);
					throw e;
				} catch (final Exception e) {
					logger.atError().withException(e).log("Could not check for updates");
					threadSync.asyncExec(() -> {
						updateCheckProgressDialog.close();
						FxDialog.showExceptionDialog(e, getClass().getClassLoader());
					});
					throw e;
				}
			}

			@Override
			protected void succeeded() {
				threadSync.asyncExec(() -> updateCheckProgressDialog.close());
			}
		};

		final CompletableFuture<?> taskFuture = CompletableFuture.runAsync(updateCheckTask);
		updateCheckProgressDialog = FxDialog.showProgressDialog("Checking for updates", updateCheckTask, getClass().getClassLoader(),
		        () -> taskFuture.cancel(true));

		updateCheckTask.setOnSucceeded(t -> {
			final var dialog = new CheckForFeatureUpdatesDialog();
			ContextInjectionFactory.inject(dialog, context);
			dialog.init();
			logger.atDebug().log("Injected check for updates dialog to eclipse context");

			final var featuresToBeUpdated = updateCheckTask.getValue();
			if (featuresToBeUpdated.isEmpty()) {
				return;
			}
			dialog.initFeaturesToBeUpdated(featuresToBeUpdated);
			final var  selectedFeatures = dialog.showAndWait();
			Task<Void> updateTask       = null;
			if (selectedFeatures.isPresent()) {
				updateTask = new Task<>() {

					@Override
					protected Void call() throws Exception {
						final var              selected                    = selectedFeatures.get();
						final List<FeatureDTO> successfullyUpdatedFeatures = Lists.newArrayList();
						final List<FeatureDTO> notUpdatedFeatures          = Lists.newArrayList();
						for (final FeatureDTO entry : selected.features()) {
							final var repoURL = entry.archiveURL;
							try {
								final var feature = featureAgent.readFeature(new URL(repoURL), featureIdAsString(entry.id));
								logger.atInfo().log("Processing update: '%s'", feature.getValue());
								featureAgent.updateOrInstall(feature.getKey(), repoURL);
								successfullyUpdatedFeatures.add(entry);
							} catch (final InterruptedException e) {
								logger.atInfo().log("Update processing task interrupted");
								threadSync.asyncExec(updateProgressDialog::close);
								throw e;
							} catch (final Exception e) {
								logger.atError().withException(e).log("Cannot check for updates");
								notUpdatedFeatures.add(entry);
								throw e;
							}
						}
						threadSync.asyncExec(() -> {
							updateProgressDialog.close();
							final var header = "Feature Update";

							final var builder = new StringBuilder();
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
				final var future = CompletableFuture.runAsync(updateTask);
				updateProgressDialog = FxDialog.showProgressDialog("Updating Features", updateTask, getClass().getClassLoader(),
				        () -> future.cancel(true));
			}
		});
	}

	private String featureIdAsString(final IdDTO id) {
		return id.groupId + ":" + id.artifactId + ":" + id.version;
	}

}
