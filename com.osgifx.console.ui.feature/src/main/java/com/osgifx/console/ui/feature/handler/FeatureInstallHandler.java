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

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.preferences.Preference;
import org.eclipse.fx.core.preferences.Value;
import org.osgi.annotation.bundle.Requirement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.osgifx.console.feature.FeatureDTO;
import com.osgifx.console.feature.IdDTO;
import com.osgifx.console.ui.feature.dialog.FeatureInstallDialog;
import com.osgifx.console.update.FeatureAgent;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.update.FeatureAgent)")
public final class FeatureInstallHandler {

	@Log
	@Inject
	private FluentLogger    logger;
	@Inject
	private IEclipseContext context;
	@Inject
	private FeatureAgent    featureAgent;
	@Inject
	private IWorkbench      workbench;
	@Inject
	@Preference(nodePath = "osgi.fx.feature", key = "repos", defaultValue = "")
	private Value<String>   repos;
	private ProgressDialog  progressDialog;

	@Execute
	public void execute() {
		final var dialog = new FeatureInstallDialog();

		ContextInjectionFactory.inject(dialog, context);
		logger.atDebug().log("Injected install feature dialog to eclipse context");
		dialog.init();

		final var selectedFeatures = dialog.showAndWait();
		if (selectedFeatures.isPresent()) {
			final var selected = selectedFeatures.get();
			updateOrInstallFeatures(selected.features, selected.archiveURL);
		}
	}

	private void updateOrInstallFeatures(final List<? extends Entry<File, FeatureDTO>> features, final String archiveURL) {
		if (features.isEmpty()) {
			return;
		}
		final Task<Void> task = new Task<>() {
			final List<FeatureDTO> successfullyInstalledFeatures = Lists.newArrayList();
			final List<FeatureDTO> notInstalledFeatures          = Lists.newArrayList();

			@Override
			protected Void call() throws Exception {
				for (final Entry<File, FeatureDTO> feature : features) {
					final var f  = feature.getValue();
					final var id = featureIdAsString(f.id);
					try {
						featureAgent.updateOrInstall(feature.getKey(), archiveURL);
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
				final var header = "Feature Installation";

				final var builder = new StringBuilder();
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
				final var   gson = new Gson();
				Set<String> toBeStored;
				if (repos.getValue().isEmpty()) {
					toBeStored = Sets.newHashSet(archiveURL);
				} else {
					toBeStored = gson.fromJson(repos.getValue(), new TypeToken<HashSet<String>>() {
					}.getType());
					toBeStored.add(archiveURL);
				}
				final var json = gson.toJson(toBeStored);
				repos.publish(json);
			}
		};

		final var th = new Thread(task);
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
