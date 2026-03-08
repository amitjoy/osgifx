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
package com.osgifx.console.ui.components.handler;

import static com.osgifx.console.event.topics.ComponentActionEventTopics.COMPONENT_DISABLED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.components.dialog.ImpactAnalysisDialog;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.stage.Window;

@Creatable
public final class ComponentDisableHandler {

    @Log
    @Inject
    private FluentLogger                   logger;
    @Inject
    private Executor                       executor;
    @Inject
    private IEventBroker                   eventBroker;
    @Inject
    @Optional
    private Supervisor                     supervisor;
    @Inject
    private ThreadSynchronize              threadSync;
    @Inject
    @Optional
    @Named("is_connected")
    private boolean                        isConnected;
    @Inject
    private DataProvider                   dataProvider;
    @Inject
    private Provider<ImpactAnalysisDialog> impactAnalysisDialogProvider;
    @Inject
    @ContextValue("shell")
    private Window                         window;

    @Execute
    public void execute(@Named("id") final String id) {
        final var selection     = dataProvider.components().stream().filter(c -> String.valueOf(c.id).equals(id))
                .toList();
        final var allComponents = dataProvider.components();
        final var allBundles    = dataProvider.bundles();

        final var dialog = impactAnalysisDialogProvider.get();
        dialog.init("DISABLE", selection, allComponents, allBundles, window);

        final var result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData().isDefaultButton()) {
            final Task<Void> disableTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        final var agent  = supervisor.getAgent();
                        final var result = agent.disableComponentById(Long.parseLong(id));
                        if (result.result == XResultDTO.SUCCESS) {
                            logger.atInfo().log(result.response);
                            eventBroker.post(COMPONENT_DISABLED_EVENT_TOPIC, id);
                        } else if (result.result == XResultDTO.SKIPPED) {
                            logger.atWarning().log(result.response);
                        } else {
                            logger.atError().log(result.response);
                            threadSync.asyncExec(() -> FxDialog.showErrorDialog("Component Disable Error",
                                    result.response, getClass().getClassLoader()));
                        }
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Service component with ID '%s' cannot be disabled", id);
                        threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                    }
                    return null;
                }
            };
            executor.runAsync(disableTask);
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

}
