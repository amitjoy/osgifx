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
package com.osgifx.console.ui.bundles.handler;

import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_INSTALLED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.io.Files;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.bundles.dialog.BundleInstallDialog;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;

public final class BundleInstallHandler {

	@Log
	@Inject
	private FluentLogger      logger;
	@Inject
	private IEclipseContext   context;
	@Inject
	private IEventBroker      eventBroker;
	@Inject
	private Supervisor        supervisor;
	@Inject
	@Named("is_connected")
	private boolean           isConnected;
	@Inject
	private ThreadSynchronize threadSync;

	@Execute
	public void execute() {
		final var dialog = new BundleInstallDialog();

		ContextInjectionFactory.inject(dialog, context);
		logger.atInfo().log("Injected install bundle dialog to eclipse context");
		dialog.init();

		final var remoteInstall = dialog.showAndWait();
		if (remoteInstall.isPresent()) {
			final Task<Void> installTask = new Task<>() {
				@Override
				protected Void call() throws Exception {
					try {
						final var dto        = remoteInstall.get();
						final var file       = dto.file();
						final var startLevel = dto.startLevel();
						if (file == null) {
							return null;
						}
						logger.atInfo().log("Selected file to install or update as bundle: %s", file);

						final var agent = supervisor.getAgent();
						if (agent == null) {
							logger.atWarning().log("Remote agent cannot be connected");
							return null;
						}
						final var bundle = agent.installWithData(null, Files.toByteArray(file), startLevel);
						if (bundle == null) {
							logger.atError().log("Bundle cannot be installed or updated");
							return null;
						}
						logger.atInfo().log("Bundle has been installed or updated: %s", bundle);
						if (dto.startBundle()) {
							agent.start(bundle.id);
							logger.atInfo().log("Bundle has been started: %s", bundle);
						}
						eventBroker.post(BUNDLE_INSTALLED_EVENT_TOPIC, bundle.symbolicName);
						threadSync.asyncExec(() -> Fx.showSuccessNotification("Remote Bundle Install",
						        bundle.symbolicName + " successfully installed/updated"));
					} catch (final Exception e) {
						logger.atError().withException(e).log("Bundle cannot be installed or updated");
						threadSync.asyncExec(() -> Fx.showErrorNotification("Remote Bundle Install", "Bundle cannot be installed/updated"));
					}
					return null;
				}
			};

			final var thread = new Thread(installTask);
			thread.setDaemon(true);
			thread.start();
		}
	}

	@CanExecute
	public boolean canExecute() {
		return isConnected;
	}

}
