/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
package com.osgifx.console.ui.configurations.handler;

import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_UPDATED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.configurations.converter.ConfigurationManager;
import com.osgifx.console.ui.configurations.dialog.ConfigurationCreateDialog;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class ConfigurationCreateHandler {

    @Log
    @Inject
    private FluentLogger         logger;
    @Inject
    private IEclipseContext      context;
    @Inject
    private Executor             executor;
    @Inject
    private ThreadSynchronize    threadSync;
    @Inject
    private IEventBroker         eventBroker;
    @Inject
    @Named("is_connected")
    private boolean              isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean              isSnapshotAgent;
    @Inject
    private ConfigurationManager configManager;

    @Execute
    public void execute() {
        final var dialog = new ConfigurationCreateDialog();
        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected configuration create dialog to eclipse context");
        dialog.init();

        final var configuration = dialog.showAndWait();
        if (configuration.isPresent()) {
            final Task<Void> createTask = new Task<>() {

                @Override
                protected Void call() throws Exception {
                    try {
                        final var dto        = configuration.get();
                        final var pid        = dto.pid();
                        final var factoryPid = dto.factoryPid();
                        final var properties = dto.properties();

                        if (StringUtils.isBlank(pid) && StringUtils.isBlank(factoryPid) || properties == null) {
                            return null;
                        }

                        final boolean result;
                        final String  effectivePID;

                        if (StringUtils.isNotBlank(pid)) {
                            effectivePID = pid.strip();
                            result       = configManager.createOrUpdateConfiguration(effectivePID, properties);
                        } else if (StringUtils.isNotBlank(factoryPid)) {
                            effectivePID = factoryPid.strip();
                            result       = configManager.createFactoryConfiguration(effectivePID, properties);
                        } else {
                            return null;
                        }

                        if (result) {
                            eventBroker.post(CONFIGURATION_UPDATED_EVENT_TOPIC, effectivePID);
                            threadSync.asyncExec(() -> Fx.showSuccessNotification("New Configuration",
                                    "Configuration - '" + effectivePID + "' has been successfully created"));
                            logger.atInfo().log("Configuration - '%s' has been successfully created", effectivePID);
                        } else {
                            threadSync.asyncExec(() -> Fx.showErrorNotification("New Configuration",
                                    "Configuration - '" + effectivePID + "' could not be created"));
                            logger.atWarning().log("Configuration - '%s' could not be created", effectivePID);
                        }
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Configuration could not be created");
                        threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                    }
                    return null;
                }
            };
            executor.runAsync(createTask);
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected && !isSnapshotAgent;
    }

}
