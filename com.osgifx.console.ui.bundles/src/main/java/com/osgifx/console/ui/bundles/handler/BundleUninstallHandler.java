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
package com.osgifx.console.ui.bundles.handler;

import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_UNINSTALLED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class BundleUninstallHandler {

    @Log
    @Inject
    private FluentLogger                                                                     logger;
    @Inject
    private Executor                                                                         executor;
    @Inject
    private IEventBroker                                                                     eventBroker;
    @Inject
    @Optional
    private Supervisor                                                                       supervisor;
    @Inject
    private ThreadSynchronize                                                                threadSync;
    @Inject
    @Optional
    @Named("is_connected")
    private boolean                                                                          isConnected;
    @Inject
    private com.osgifx.console.data.provider.DataProvider                                    dataProvider;
    @Inject
    private javax.inject.Provider<com.osgifx.console.ui.bundles.dialog.ImpactAnalysisDialog> impactAnalysisDialogProvider;
    @Inject
    @org.eclipse.fx.core.di.ContextValue("shell")
    private javafx.stage.Window                                                              window;

    @Execute
    public void execute(@Named("id") final String id) {
        final var selection     = dataProvider.bundles().stream().filter(b -> String.valueOf(b.id).equals(id)).toList();
        final var allBundles    = dataProvider.bundles();
        final var allComponents = dataProvider.components();

        final var dialog = impactAnalysisDialogProvider.get();
        dialog.init("UNINSTALL", selection, allBundles, allComponents, window);

        final var result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData().isDefaultButton()) {
            final Task<Void> uninstallTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        final var agent = supervisor.getAgent();
                        final var error = agent.uninstall(Long.parseLong(id));
                        if (error == null) {
                            logger.atInfo().log("Bundle with ID '%s' has been uninstalled", id);
                            eventBroker.post(BUNDLE_UNINSTALLED_EVENT_TOPIC, id);
                        } else {
                            logger.atError().log(error);
                            threadSync.asyncExec(() -> FxDialog.showErrorDialog("Bundle Uninstall Error", error,
                                    getClass().getClassLoader()));
                        }
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Bundle with ID '%s' cannot be uninstalled", id);
                        threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                    }
                    return null;
                }
            };
            executor.runAsync(uninstallTask);
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

}
