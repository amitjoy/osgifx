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
package com.osgifx.console.ui.snapshot.handler;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.SNAPSHOT;
import static com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType.REMOTE_RPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;

import javax.inject.Inject;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;

import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.supervisor.factory.SupervisorFactory;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

public final class SnapshotImportHandler {

    private static final String PID = "com.osgifx.console.snapshot";

    @Log
    @Inject
    private FluentLogger               logger;
    @Inject
    private Executor                   executor;
    @Inject
    @Optional
    private Supervisor                 supervisor;
    @Inject
    private IEventBroker               eventBroker;
    @Inject
    private ThreadSynchronize          threadSync;
    @Inject
    private ConfigurationAdmin         configAdmin;
    @Inject
    @Optional
    @ContextValue("is_connected")
    private ContextBoundValue<Boolean> isConnected;
    @Inject
    @Optional
    @ContextValue("connected.agent")
    private ContextBoundValue<String>  connectedAgent;
    @Inject
    @Optional
    @ContextValue("is_snapshot_agent")
    private ContextBoundValue<Boolean> isSnapshotAgent;
    @Inject
    private SupervisorFactory          supervisorFactory;
    private ProgressDialog             progressDialog;

    @Execute
    public void execute() {
        final var bundleChooser = new FileChooser();
        bundleChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Snapshots (.json)", "*.json"));
        final var snapshot = bundleChooser.showOpenDialog(null);

        if (snapshot != null) {
            final Task<File> task = new Task<>() {

                @Override
                protected File call() throws Exception {
                    try (var is = new FileInputStream(snapshot)) {
                        updateSnapshotLocation(snapshot.getAbsolutePath());
                        supervisorFactory.removeSupervisor(REMOTE_RPC);
                        supervisorFactory.createSupervisor(SNAPSHOT);
                        return snapshot;
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Cannot import snapshot '%s'", snapshot.getName());
                        threadSync.asyncExec(() -> {
                            progressDialog.close();
                            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                        });
                        throw e;
                    }
                }
            };

            task.setOnSucceeded(t -> {
                isConnected.publish(true);
                isSnapshotAgent.publish(true);
                connectedAgent.publish("Snapshot Agent: " + task.getValue().getName());
                eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC, "");
                threadSync.asyncExec(progressDialog::close);
                logger.atInfo().log("Snapshot has been successfully imported");
            });

            final var taskFuture = executor.runAsync(task);
            progressDialog = FxDialog.showProgressDialog("Import Snapshot", task, getClass().getClassLoader(), () -> {
                taskFuture.cancel(true);
            });
        }
    }

    @CanExecute
    public boolean canExecute() {
        return !isConnected.getValue();
    }

    private void updateSnapshotLocation(final String snapshot) {
        try {
            final var                        configuration = configAdmin.getConfiguration(PID, "?");
            final Dictionary<String, String> properties    = FrameworkUtil.asDictionary(Map.of("location", snapshot));
            configuration.updateIfDifferent(properties);
        } catch (final IOException e) {
            logger.atError().withException(e).log("Cannot rretrieve configuration '%s'", PID);
        }

    }

}
