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

import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.stage.FileChooser;

public final class SnapshotImportHandler {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private IWorkbench        workbench;
    @Inject
    private ThreadSynchronize threadSync;
    private ProgressDialog    progressDialog;

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
                        logger.atInfo().log("Extension '%s' has been successfuly installed/updated");
                        return null;
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Cannot install extension '%s'", snapshot.getName());
                        threadSync.asyncExec(() -> {
                            progressDialog.close();
                            FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                        });
                        throw e;
                    }
                }

                @Override
                protected void succeeded() {
                    threadSync.asyncExec(progressDialog::close);
                    FxDialog.showInfoDialog("Extension Installation",
                            "The application must be restarted, therefore, will be shut down right away",
                            getClass().getClassLoader(), btn -> workbench.restart());
                }
            };

            final CompletableFuture<?> taskFuture = CompletableFuture.runAsync(task);
            progressDialog = FxDialog.showProgressDialog("Extension Installation", task, getClass().getClassLoader(),
                    () -> {
                        taskFuture.cancel(true);
                    });
        }
    }

}
