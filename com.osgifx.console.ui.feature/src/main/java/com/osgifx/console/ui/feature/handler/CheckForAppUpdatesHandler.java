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

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.controlsfx.dialog.CommandLinksDialog;
import org.controlsfx.dialog.CommandLinksDialog.CommandLinksButtonType;
import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import com.osgifx.console.update.FeatureAgent;
import com.osgifx.console.util.fx.FxDialog;

import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.stage.StageStyle;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.update.FeatureAgent)")
public final class CheckForAppUpdatesHandler {

	private static final String DOWNLOAD_URL = "https://osgifx.com";
	@Log
	@Inject
	private FluentLogger        logger;
	@Inject
	private ThreadSynchronize   threadSync;
	@Inject
	private FeatureAgent        featureAgent;
	@Inject
	private HostServices        hostServices;
	@Inject
	@OSGiBundle
	private BundleContext       bundleContext;
	private ProgressDialog      updateCheckProgressDialog;

	@Execute
	public void execute() {
		logger.atInfo().log("Checking for application updates");
		final Task<String> updateCheckTask = new Task<>() {
			@Override
			protected String call() throws Exception {
				try {
					final var latestAppVersion = featureAgent.checkForAppUpdates();
					if (latestAppVersion.isEmpty() || updatesNotAvailable(latestAppVersion.get())) {
						logger.atInfo().log("No application updates available");
						threadSync.asyncExec(() -> {
							updateCheckProgressDialog.close();
							FxDialog.showInfoDialog("Check for Updates", "No update found", getClass().getClassLoader());
						});
						return null;
					}
					return latestAppVersion.get();
				} catch (final InterruptedException e) {
					logger.atInfo().log("Check for application updates task interrupted");
					threadSync.asyncExec(updateCheckProgressDialog::close);
					throw e;
				} catch (final Exception e) {
					logger.atError().withException(e).log("Cannot check for application updates");
					threadSync.asyncExec(() -> {
						updateCheckProgressDialog.close();
						FxDialog.showExceptionDialog(e, getClass().getClassLoader());
					});
				}
				return null;
			}

			private boolean updatesNotAvailable(final String latest) {
				final var currentVersion = bundleContext.getBundle().getVersion();
				final var latestVersion  = new Version(latest);

				logger.atDebug().log("Current Version: %s | Latest Version: %s", currentVersion, latestVersion);

				return latestVersion.compareTo(currentVersion) <= 0;
			}

			@Override
			protected void succeeded() {
				threadSync.asyncExec(updateCheckProgressDialog::close);
			}
		};

		final CompletableFuture<?> taskFuture = CompletableFuture.runAsync(updateCheckTask);
		updateCheckProgressDialog = FxDialog.showProgressDialog("Checking for Updates", updateCheckTask, getClass().getClassLoader(),
		        () -> taskFuture.cancel(true));
		updateCheckTask.setOnSucceeded(t -> openDialog(updateCheckTask.getValue()));
	}

	private void openDialog(final String latestVersion) {
		if (latestVersion == null) {
			return;
		}
		final List<CommandLinksButtonType> links = new ArrayList<>();

		final var downloadNowBtn  = new CommandLinksButtonType("Download Now", "Open download page", true);
		final var downloadSkipBtn = new CommandLinksButtonType("Skip Download", "Skip updating the application", false);

		links.add(downloadNowBtn);
		links.add(downloadSkipBtn);

		final var dialog = new CommandLinksDialog(links);

		dialog.initStyle(StageStyle.UNDECORATED);
		dialog.getDialogPane().getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
		dialog.getDialogPane().setHeaderText("Updates are available");
		dialog.getDialogPane().setContentText("Latest version is " + latestVersion);

		final var buttonType = dialog.showAndWait();

		if (buttonType.isPresent()) {
			final var result = buttonType.get();
			if (result == downloadNowBtn.getButtonType()) {
				hostServices.showDocument(DOWNLOAD_URL);
				logger.atInfo().log("Download URL opened");
			} else if (result == downloadSkipBtn.getButtonType()) {
				logger.atInfo().log("Download skipped");
			}
		}
	}

}
