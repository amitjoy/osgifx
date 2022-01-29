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

import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_UNINSTALLED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.Agent;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.FxDialog;

public final class BundleUninstallHandler {

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private IEventBroker eventBroker;
    @Inject
    private Supervisor   supervisor;

    @Execute
    public void execute(@Named("id") final String id) {
        final Agent agent = supervisor.getAgent();
        if (supervisor.getAgent() == null) {
            logger.atWarning().log("Remote agent cannot be connected");
            return;
        }
        try {
            final String error = agent.uninstall(Long.parseLong(id));
            if (error == null) {
                logger.atInfo().log("Bundle with ID '%s' has been uninstalled", id);
                eventBroker.post(BUNDLE_UNINSTALLED_EVENT_TOPIC, id);
            } else {
                logger.atError().log(error);
                FxDialog.showErrorDialog("Bundle Uninstall Error", error, getClass().getClassLoader());
            }
        } catch (final Exception e) {
            logger.atError().withException(e).log("Bundle with ID '%s' cannot be uninstalled", e);
            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
        }
    }

}
