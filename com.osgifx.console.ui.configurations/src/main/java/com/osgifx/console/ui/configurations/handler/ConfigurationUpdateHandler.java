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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.osgifx.console.agent.Agent;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.FxDialog;

public final class ConfigurationUpdateHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private IEventBroker eventBroker;
    @Inject
    private Supervisor   supervisor;

    @Execute
    public void execute(@Named("pid") final String pid, @Named("properties") final String properties) {
        final Agent agent = supervisor.getAgent();
        if (supervisor.getAgent() == null) {
            logger.atWarning().log("Remote agent cannot be connected");
            return;
        }
        try {
            final Map<String, Object> props  = new Gson().fromJson(properties, new TypeToken<Map<String, Object>>() {
                                             }.getType());
            final XResultDTO          result = agent.createOrUpdateConfiguration(pid, props);
            if (result.result == XResultDTO.SUCCESS) {
                logger.atInfo().log(result.response);
                eventBroker.send(CONFIGURATION_UPDATED_EVENT_TOPIC, pid);
            } else if (result.result == XResultDTO.SKIPPED) {
                logger.atWarning().log(result.response);
            } else {
                logger.atError().log(result.response);
                FxDialog.showErrorDialog("Configuration Update Error", result.response, getClass().getClassLoader());
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Configuration with PID '%s' cannot be updated", pid);
            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
        }
    }

}
