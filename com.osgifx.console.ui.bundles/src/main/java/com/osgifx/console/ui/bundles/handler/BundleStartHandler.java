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

import static com.osgifx.console.event.topics.BundleActionEventTopics.BUNDLE_STARTED_EVENT_TOPIC;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;

public final class BundleStartHandler {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private IEventBroker      eventBroker;
    @Inject
    private Supervisor        supervisor;
    @Inject
    @Optional
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    private ThreadSynchronize threadSync;

    @Execute
    public void execute(@Named("id") final String id) {
        if (!isConnected) {
            logger.atWarning().log("Remote agent cannot be connected");
            return;
        }
        final Task<Void> startTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    final var agent = supervisor.getAgent();
                    final var error = agent.start(Long.parseLong(id));
                    if (error == null) {
                        logger.atInfo().log("Bundle with ID '%s' has been started", id);
                        eventBroker.post(BUNDLE_STARTED_EVENT_TOPIC, id);
                    } else {
                        logger.atError().log(error);
                        threadSync.asyncExec(() -> FxDialog.showErrorDialog("Bundle Start Error", error, getClass().getClassLoader()));
                    }
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Bundle with ID '%s' cannot be started", id);
                    threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                }
                return null;
            }
        };
        CompletableFuture.runAsync(startTask);
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected;
    }

}
