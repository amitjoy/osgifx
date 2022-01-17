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

import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_INSTALLED_EVENT_TOPIC;

import java.io.File;
import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.dto.BundleDTO;

import com.google.common.io.Files;
import com.osgifx.console.agent.Agent;
import com.osgifx.console.application.dialog.BundleInstallDTO;
import com.osgifx.console.application.dialog.BundleInstallDialog;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

public final class BundleInstallHandler {

    @Log
    @Inject
    private FluentLogger    logger;
    @Inject
    private IEclipseContext context;
    @Inject
    private IEventBroker    eventBroker;
    @Inject
    private Supervisor      supervisor;

    @Execute
    public void execute() {
        final BundleInstallDialog dialog = new BundleInstallDialog();

        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected install bundle dialog to eclipse context");
        dialog.init();

        final Optional<BundleInstallDTO> remoteInstall = dialog.showAndWait();
        if (remoteInstall.isPresent()) {
            try {
                final BundleInstallDTO dto        = remoteInstall.get();
                final File             file       = dto.file;
                final int              startLevel = dto.startLevel;
                if (file == null) {
                    return;
                }
                logger.atInfo().log("Selected file to install or update as bundle: %s", file);

                final Agent agent = supervisor.getAgent();
                if (agent == null) {
                    logger.atWarning().log("Remote agent cannot be connected");
                    return;
                }
                final BundleDTO bundle = agent.installWithData(null, Files.toByteArray(file), startLevel);
                if (bundle == null) {
                    logger.atError().log("Bundle cannot be installed or updated");
                    return;
                }
                logger.atInfo().log("Bundle has been installed or updated: %s", bundle);
                if (dto.startBundle) {
                    agent.start(bundle.id);
                    logger.atInfo().log("Bundle has been started: %s", bundle);
                }
                eventBroker.send(BUNDLE_INSTALLED_EVENT_TOPIC, bundle.symbolicName);
                Fx.showSuccessNotification("Remote Bundle Install", bundle.symbolicName + " successfully installed/updated");
            } catch (final Exception e) {
                logger.atError().withException(e).log("Bundle cannot be installed or updated");
                Fx.showErrorNotification("Remote Bundle Install", "Bundle cannot be installed/updated");
            }
        }
    }

}
