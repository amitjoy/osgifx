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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

import com.google.gson.Gson;
import com.osgifx.console.agent.Agent;
import com.osgifx.console.dto.SnapshotDTO;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;
import com.osgifx.console.util.io.IO;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;

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
        final var directoryChooser = new DirectoryChooser();
        final var location         = directoryChooser.showDialog(null);
        if (location == null) {
            return;
        }
        final Task<String> snapshotTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    updateMessage("Capturing snapshot of the remote runtime");
                    return snapshot();
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
        snapshotTask.valueProperty().addListener((ChangeListener<String>) (obs, oldValue, newValue) -> {
            if (newValue != null) {
                threadSync.asyncExec(() -> {
                    try {
                        final var jsonSnapshot = new File(location, IO.prepareFilenameFor("json"));
                        Files.write(jsonSnapshot.toPath(), newValue.getBytes());
                        Fx.showSuccessNotification("Snapshot Successfully Captured", jsonSnapshot.getAbsolutePath());
                    } catch (final IOException e) {
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    }
                });
            }
        });
        final CompletableFuture<?> taskFuture = CompletableFuture.runAsync(snapshotTask);
        progressDialog = FxDialog.showProgressDialog("Capture Snapshpt", snapshotTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    private String snapshot() {
        Agent agent = null;
        if (supervisor == null || (agent = supervisor.getAgent()) == null) {
            return null;
        }
        final var dto = new SnapshotDTO();

        dto.bundles              = agent.getAllBundles();
        dto.components           = agent.getAllComponents();
        dto.configuations        = agent.getAllConfigurations();
        dto.properties           = agent.getAllProperties();
        dto.services             = agent.getAllServices();
        dto.threads              = agent.getAllThreads();
        dto.dmtNodes             = agent.readDmtNode(".");
        dto.memoryInfo           = agent.getMemoryInfo();
        dto.roles                = agent.getAllRoles();
        dto.healthChecks         = agent.getAllHealthChecks();
        dto.classloaderLeaks     = agent.getClassloaderLeaks();
        dto.httpComponents       = agent.getHttpComponents();
        dto.bundleLoggerContexts = agent.getBundleLoggerContexts();
        dto.heapUsage            = agent.getHeapUsage();
        dto.runtime              = agent.getRuntimeDTO();

        try {
            return new Gson().toJson(dto);
        } catch (final Exception e) {
            return null;
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected && !isSnapshotAgent;
    }

}
