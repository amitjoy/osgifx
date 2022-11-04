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
package com.osgifx.console.ui.snapshot.handler;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.SNAPSHOT;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.SOCKET_RPC;

import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.supervisor.factory.SupervisorFactory;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

public final class SnapshotImportHandler {

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    @Optional
    private Supervisor                 supervisor;
    @Inject
    private IEventBroker               eventBroker;
    @Inject
    private ThreadSynchronize          threadSync;
    @Inject
    @Optional
    @ContextValue("is_connected")
    private ContextBoundValue<Boolean> isConnected;
    @Inject
    @Optional
    @ContextValue("connected.agent")
    private ContextBoundValue<String>  connectedAgent;
    @Inject
    private SupervisorFactory          supervisorFactory;
    private ProgressDialog             progressDialog;

    @Execute
    public void execute() {
        final var bundleChooser = new FileChooser();
        bundleChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Snapshots (.json)", "*.json"));
        final var snapshot = bundleChooser.showOpenDialog(null);

        if (snapshot != null) {
            final Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    try (var is = new FileInputStream(snapshot)) {
                        supervisorFactory.removeSupervisor(SOCKET_RPC);
                        supervisorFactory.createSupervisor(SNAPSHOT);
                        TimeUnit.SECONDS.sleep(4);
                        return null;
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Cannot import snapshot '%s'", snapshot.getName());
                        threadSync.asyncExec(() -> {
                            progressDialog.close();
                            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                        });
                        throw e;
                    }
                }

                @Override
                protected void succeeded() {
                    isConnected.publish(true);
                    connectedAgent.publish("Snapshot Agent");
                    eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC, "");
                    threadSync.asyncExec(progressDialog::close);
                    logger.atInfo().log("Snapshot has been successfully imported");
                }
            };

            final CompletableFuture<?> taskFuture = CompletableFuture.runAsync(task);
            progressDialog = FxDialog.showProgressDialog("Import Snapshot", task, getClass().getClassLoader(), () -> {
                taskFuture.cancel(true);
            });
        }
    }

}
