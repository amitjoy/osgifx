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

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XSnapshotDTO;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;

public final class SnapshotCaptureHandler {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    @Optional
    private Supervisor        supervisor;
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
        final Task<XSnapshotDTO> snapshotTask = new Task<>() {
            @Override
            protected XSnapshotDTO call() throws Exception {
                try {
                    updateMessage("Capturing snapshot of the remote runtime");
                    return supervisor.getAgent().snapshot();
                } catch (final InterruptedException e) {
                    logger.atInfo().log("Snapshot task interrupted");
                    threadSync.asyncExec(progressDialog::close);
                    throw e;
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot capture snapshot");
                    threadSync.asyncExec(() -> {
                        progressDialog.close();
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
            }
        };
        snapshotTask.valueProperty().addListener((ChangeListener<XSnapshotDTO>) (obs, oldValue, newValue) -> {
            if (newValue != null) {
                threadSync.asyncExec(() -> Fx.showSuccessNotification("Snapshot Successfully Captured",
                        "File: " + newValue.location));
            }
        });
        final CompletableFuture<?> taskFuture = CompletableFuture.runAsync(snapshotTask);
        progressDialog = FxDialog.showProgressDialog("Capture Snapshpt", snapshotTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected && !isSnapshotAgent;
    }

}
