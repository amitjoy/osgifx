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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.gson.Gson;
import com.osgifx.console.agent.spi.LargePayloadHandler;
import com.osgifx.console.agent.spi.PayloadMetadata;
import com.osgifx.console.agent.spi.PayloadType;
import com.osgifx.console.dto.SnapshotDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.snapshot.SnapshotOptionsDialog;
import com.osgifx.console.ui.snapshot.SnapshotOptionsDialog.SnapshotStatistics;
import com.osgifx.console.ui.snapshot.SnapshotPathPromptDialog;
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
    private Executor          executor;
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
    @Inject
    private IEclipseContext   eclipseContext;
    private ProgressDialog    progressDialog;

    @Execute
    public void execute() {
        final var agent = supervisor.getAgent();
        if (agent == null) {
            return;
        }

        // Prepare snapshot statistics in background to avoid blocking UI
        final Task<SnapshotStatistics> prepTask = new Task<>() {
            @Override
            protected SnapshotStatistics call() throws Exception {
                updateMessage("Estimating snapshot size...");
                final long    estimatedSize       = agent.estimateSnapshotSize();
                final long    estimatedCompressed = estimatedSize / 5;
                final long    rpcLimit            = 200_000_000L;
                final boolean rpcAvailable        = estimatedCompressed <= rpcLimit;
                final boolean isMqttTransport     = supervisor.getConnectionType().equals("MQTT");

                final var handlerTracker = supervisor.getLargePayloadHandlerTracker();
                final var handler        = handlerTracker != null ? handlerTracker.getService() : null;

                updateMessage("Counting bundles and services...");
                final var bundleCount  = agent.getAllBundles().size();
                final var serviceCount = agent.getAllServices().size();

                return new SnapshotStatistics(bundleCount, serviceCount, estimatedSize, estimatedCompressed,
                                              isMqttTransport, rpcAvailable, handler);
            }
        };

        prepTask.setOnSucceeded(_ -> {
            final var stats = prepTask.getValue();

            final var dialog = new SnapshotOptionsDialog();
            ContextInjectionFactory.inject(dialog, eclipseContext);
            dialog.init(stats);

            final var optionResult = dialog.showAndWait();
            if (optionResult.isEmpty()) {
                return;
            }

            final var option  = optionResult.get();
            final var handler = stats.handler;
            switch (option) {
                case USE_HANDLER:
                    snapshotWithHandler(handler);
                    break;
                case USE_RPC:
                    snapshotWithRpc();
                    break;
                case STORE_LOCALLY:
                    snapshotLocally();
                    break;
            }
        });

        prepTask.setOnFailed(_ -> {
            final var ex = prepTask.getException();
            logger.atError().withException(ex).log("Failed to prepare snapshot statistics");
            threadSync.asyncExec(() -> FxDialog.showExceptionDialog(ex, getClass().getClassLoader()));
        });

        final var prepFuture = executor.runAsync(prepTask);
        FxDialog.showProgressDialog("Preparing Snapshot", prepTask, getClass().getClassLoader(),
                () -> prepFuture.cancel(true));
    }

    private void snapshotWithHandler(final LargePayloadHandler handler) {
        final var agent = supervisor.getAgent();

        final Task<String> snapshotTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    updateMessage("Stage 1/3: Generating snapshot file");
                    final var localPath = agent.createSnapshotLocally("/tmp/snapshots");

                    updateMessage("Stage 2/3: Uploading via handler");
                    final var metadata = new PayloadMetadata(new File(localPath).getName(),
                                                             new File(localPath).length(), "application/json",
                                                             PayloadType.SNAPSHOT, System.currentTimeMillis());
                    final var result   = handler.handle(localPath, metadata);

                    updateMessage("Stage 3/3: Finalizing");
                    if (!result.success) {
                        throw new Exception("Handler failed: " + result.errorMessage);
                    }
                    return result.location;
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot capture snapshot with handler");
                    threadSync.asyncExec(() -> {
                        progressDialog.close();
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
            }
        };

        snapshotTask.valueProperty().addListener((ChangeListener<String>) (_, _, location) -> {
            if (location != null) {
                threadSync.asyncExec(() -> Fx.showSuccessNotification("Snapshot Successfully Uploaded", location));
            }
        });

        final var taskFuture = executor.runAsync(snapshotTask);
        progressDialog = FxDialog.showProgressDialog("Capture Snapshot", snapshotTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    private void snapshotWithRpc() {
        final var directoryChooser = new DirectoryChooser();
        final var location         = directoryChooser.showDialog(null);
        if (location == null) {
            return;
        }

        final Task<String> snapshotTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    updateMessage("Capturing bundles (1/17)...");
                    final var dto   = new SnapshotDTO();
                    final var agent = supervisor.getAgent();

                    dto.bundles = agent.getAllBundles();
                    updateMessage("Capturing components (2/17)...");
                    dto.components = agent.getAllComponents();
                    updateMessage("Capturing configurations (3/17)...");
                    dto.configurations = agent.getAllConfigurations();
                    updateMessage("Capturing properties (4/17)...");
                    dto.properties = agent.getAllProperties();
                    updateMessage("Capturing services (5/17)...");
                    dto.services = agent.getAllServices();
                    updateMessage("Capturing threads (6/17)...");
                    dto.threads = agent.getAllThreads();
                    updateMessage("Capturing DMT nodes (7/17)...");
                    dto.dmtNodes = agent.readDmtNode(".");
                    updateMessage("Capturing memory info (8/17)...");
                    dto.memoryInfo = agent.getMemoryInfo();
                    updateMessage("Capturing roles (9/17)...");
                    dto.roles = agent.getAllRoles();
                    updateMessage("Capturing health checks (10/17)...");
                    dto.healthChecks = agent.getAllHealthChecks();
                    updateMessage("Capturing classloader leaks (11/17)...");
                    dto.classloaderLeaks = agent.getClassloaderLeaks();
                    updateMessage("Capturing HTTP components (12/17)...");
                    dto.httpComponents = agent.getHttpComponents();
                    updateMessage("Capturing logger contexts (13/17)...");
                    dto.bundleLoggerContexts = agent.getBundleLoggerContexts();
                    updateMessage("Capturing JAX-RS components (14/17)...");
                    dto.jaxRsComponents = agent.getJaxRsComponents();
                    updateMessage("Capturing CDI containers (15/17)...");
                    dto.cdiContainers = agent.getCdiContainers();
                    updateMessage("Capturing heap usage (16/17)...");
                    dto.heapUsage = agent.getHeapUsage();
                    updateMessage("Capturing runtime info (17/17)...");
                    dto.runtime = agent.getRuntimeDTO();

                    updateMessage("Serializing snapshot...");
                    return new Gson().toJson(dto);
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

        snapshotTask.valueProperty().addListener((ChangeListener<String>) (_, _, newValue) -> {
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

        final var taskFuture = executor.runAsync(snapshotTask);
        progressDialog = FxDialog.showProgressDialog("Capture Snapshot", snapshotTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    private void snapshotLocally() {
        final var pathDialog = new SnapshotPathPromptDialog();
        ContextInjectionFactory.inject(pathDialog, eclipseContext);
        final var timestamp   = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        final var defaultPath = "{java.io.tmpdir}/snapshot-" + timestamp + ".json";
        pathDialog.init(defaultPath, "Specify File Path",
                "Enter the full file path on the agent where the snapshot should be saved (including filename).\n"
                        + "System/Framework properties (e.g., {java.io.tmpdir}) and environment variables (e.g., {env:TEMP}) can be used:");

        final var pathResult = pathDialog.showAndWait();
        if (pathResult.isEmpty()) {
            return;
        }

        final var directoryPath = pathResult.get();
        final var agent         = supervisor.getAgent();

        final Task<String> snapshotTask = new Task<>() {

            @Override
            protected String call() throws Exception {
                try {
                    updateMessage("Stage 1/3: Capturing snapshot");
                    Thread.sleep(100);
                    updateMessage("Stage 2/3: Serializing snapshot");
                    Thread.sleep(100);
                    final var localPath = agent.createSnapshotLocally(directoryPath);
                    updateMessage("Stage 3/3: Finalizing");
                    return localPath;
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot create local snapshot");
                    threadSync.asyncExec(() -> {
                        progressDialog.close();
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
            }
        };

        snapshotTask.valueProperty().addListener((ChangeListener<String>) (_, _, localPath) -> {
            if (localPath != null) {
                threadSync.asyncExec(() -> Fx.showSuccessNotification("Snapshot Saved Locally",
                        "File saved at: " + localPath + "\nRetrieve via SFTP/SCP"));
            }
        });

        final var taskFuture = executor.runAsync(snapshotTask);
        progressDialog = FxDialog.showProgressDialog("Capture Snapshot", snapshotTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected && !isSnapshotAgent;
    }

}
