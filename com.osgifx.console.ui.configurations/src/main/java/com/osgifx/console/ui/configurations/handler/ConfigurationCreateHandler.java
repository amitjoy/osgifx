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
package com.osgifx.console.ui.configurations.handler;

import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_UPDATED_EVENT_TOPIC;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.base.Strings;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.ui.configurations.converter.ConfigurationManager;
import com.osgifx.console.ui.configurations.dialog.ConfigurationCreateDialog;
import com.osgifx.console.ui.configurations.dialog.ConfigurationDTO;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

public final class ConfigurationCreateHandler {

    @Log
    @Inject
    private FluentLogger         logger;
    @Inject
    private IEclipseContext      context;
    @Inject
    private IEventBroker         eventBroker;
    @Inject
    @Named("is_connected")
    private boolean              isConnected;
    @Inject
    private ConfigurationManager configManager;

    @Execute
    public void execute() {
        final ConfigurationCreateDialog dialog = new ConfigurationCreateDialog();
        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected configuration create dialog to eclipse context");
        dialog.init();

        final Optional<ConfigurationDTO> configuration = dialog.showAndWait();
        if (configuration.isPresent()) {
            try {
                final ConfigurationDTO  dto        = configuration.get();
                final String            pid        = dto.pid;
                final String            factoryPid = dto.factoryPid;
                final List<ConfigValue> properties = dto.properties;

                if (Strings.isNullOrEmpty(pid) && Strings.isNullOrEmpty(factoryPid) || properties == null) {
                    return;
                }

                boolean result;
                String  effectivePID;
                if (!Strings.isNullOrEmpty(pid)) {
                    result       = configManager.createOrUpdateConfiguration(pid, properties);
                    effectivePID = pid;
                } else if (!Strings.isNullOrEmpty(factoryPid)) {
                    result       = configManager.createFactoryConfiguration(factoryPid, properties);
                    effectivePID = factoryPid;
                } else {
                    return;
                }
                if (result) {
                    eventBroker.post(CONFIGURATION_UPDATED_EVENT_TOPIC, dto.pid);
                    Fx.showSuccessNotification("New Configuration", "Configuration - '" + effectivePID + "' has been successfully created");
                    logger.atInfo().log("Configuration - '%s' has been successfully created", effectivePID);
                } else {
                    Fx.showErrorNotification("New Configuration", "Configuration - '" + effectivePID + "' cannot be created");
                }
            } catch (final Exception e) {
                logger.atError().withException(e).log("Configuration cannot be created");
                FxDialog.showExceptionDialog(e, getClass().getClassLoader());
            }
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

}
