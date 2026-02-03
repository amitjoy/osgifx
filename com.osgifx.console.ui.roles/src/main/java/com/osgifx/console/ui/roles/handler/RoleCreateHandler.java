/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.ui.roles.handler;

import static com.osgifx.console.event.topics.RoleActionEventTopics.ROLE_CREATED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
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
import com.osgifx.console.ui.roles.dialog.RoleCreateDialog;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class RoleCreateHandler {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private IEclipseContext   context;
    @Inject
    private Executor          executor;
    @Inject
    @Optional
    private Supervisor        supervisor;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    private IEventBroker      eventBroker;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;

    @Execute
    public void execute() {
        final var dialog = new RoleCreateDialog();
        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected role create dialog to eclipse context");
        dialog.init();

        final var role = dialog.showAndWait();
        if (role.isPresent()) {
            final Task<Void> createTask = new Task<>() {

                @Override
                protected Void call() throws Exception {
                    try {
                        final var dto      = role.get();
                        final var name     = dto.name();
                        final var roleType = dto.type();

                        final var agent = supervisor.getAgent();
                        if (agent == null) {
                            logger.atInfo().log("Agent not connected");
                            return null;
                        }

                        final var response = agent.createRole(name, roleType);

                        if (response.result == XResultDTO.SUCCESS) {
                            eventBroker.post(ROLE_CREATED_EVENT_TOPIC, name);
                            threadSync.asyncExec(() -> Fx.showSuccessNotification("New Role",
                                    "Role - '" + name + "' has been successfully created"));
                            logger.atInfo().log("Role - '%s' has been successfully created", name);
                        } else {
                            threadSync.asyncExec(() -> Fx.showErrorNotification("New Role", response.response));
                            logger.atError().log("Role - '%s' cannot be created", response.response);
                        }
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Role cannot be created");
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
