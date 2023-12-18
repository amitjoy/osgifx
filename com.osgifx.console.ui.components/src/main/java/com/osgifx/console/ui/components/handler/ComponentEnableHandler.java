/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.ui.components.handler;

import static com.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_ENABLED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class ComponentEnableHandler {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private Executor          executor;
    @Inject
    private IEventBroker      eventBroker;
    @Inject
    @Optional
    private Supervisor        supervisor;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    @Optional
    @Named("is_connected")
    private boolean           isConnected;

    @Execute
    public void execute(@Named("name") final String name) {
        if (!isConnected) {
            logger.atWarning().log("Remote agent cannot be connected");
            return;
        }
        final Task<Void> enableTask = new Task<>() {

            @Override
            protected Void call() throws Exception {
                try {
                    final var agent  = supervisor.getAgent();
                    final var result = agent.enableComponentByName(name);
                    if (result.result == XResultDTO.SUCCESS) {
                        logger.atInfo().log(result.response);
                        eventBroker.post(COMPONENT_ENABLED_EVENT_TOPIC, name);
                    } else if (result.result == XResultDTO.SKIPPED) {
                        logger.atWarning().log(result.response);
                    } else {
                        logger.atError().log(result.response);
                        threadSync.asyncExec(() -> FxDialog.showErrorDialog("Component Enable Error", result.response,
                                getClass().getClassLoader()));
                    }
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Component with name '%s' cannot be enabled", name);
                    threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                }
                return null;
            }
        };
        executor.runAsync(enableTask);
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

}
