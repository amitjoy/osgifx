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
package com.osgifx.console.ui.batchinstall.handler;

import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.ui.batchinstall.dialog.ArtifactInstaller;
import com.osgifx.console.ui.batchinstall.dialog.BatchInstallDialog;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;

public final class BatchInstallHandler {

    private static final String HEADER = "Installing artifacts from directory";

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private IEclipseContext   context;
    @Inject
    private ArtifactInstaller installer;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;
    private ProgressDialog    progressDialog;

    @Execute
    public void execute() {
        final var directoryChooser = new DirectoryChooser();
        final var directory        = directoryChooser.showDialog(null);

        if (directory != null) {
            final var dialog = new BatchInstallDialog();

            ContextInjectionFactory.inject(dialog, context);
            logger.atDebug().log("Injected batch install dialog to eclipse context");
            dialog.init(directory);

            dialog.traverseDirectoryForFiles();
            final var selectedFeatures = dialog.showAndWait();
            if (selectedFeatures.isPresent()) {
                final Task<String> batchTask = new Task<>() {
                    @Override
                    protected String call() throws Exception {
                        return installer.installArtifacts(selectedFeatures.get());
                    }
                };
                batchTask.setOnSucceeded(t -> {
                    final var result = batchTask.getValue();
                    if (result != null) {
                        if (!result.isEmpty()) {
                            threadSync.asyncExec(() -> {
                                progressDialog.close();
                                FxDialog.showErrorDialog(HEADER, result, getClass().getClassLoader());
                            });
                        } else {
                            threadSync.asyncExec(progressDialog::close);
                        }
                    }
                });
                final CompletableFuture<?> taskFuture = CompletableFuture.runAsync(batchTask).exceptionally(e -> {
                    if (progressDialog != null) {
                        threadSync.asyncExec(() -> {
                            progressDialog.close();
                            FxDialog.showExceptionDialog(e, BatchInstallHandler.class.getClassLoader());
                        });
                    }
                    return null;
                });
                progressDialog = FxDialog.showProgressDialog(HEADER, batchTask, getClass().getClassLoader(),
                        () -> taskFuture.cancel(true));
            }
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected && !isSnapshotAgent;
    }

    @Inject
    @Optional
    private void updateOnAgentDisconnectedEvent(@UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data) {
        logger.atInfo().log("Agent disconnected event received");
        if (progressDialog != null) {
            progressDialog.close();
        }
    }

}
