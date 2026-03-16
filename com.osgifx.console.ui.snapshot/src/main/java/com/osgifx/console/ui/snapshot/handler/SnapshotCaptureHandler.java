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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
import com.osgifx.console.agent.dto.RuntimeDTO;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.agent.dto.XCdiContainerDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XJaxRsComponentDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XRuntimeCapabilityDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;
import com.osgifx.console.agent.spi.payload.LargePayloadHandler;
import com.osgifx.console.agent.spi.payload.PayloadMetadata;
import com.osgifx.console.agent.spi.payload.PayloadType;
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
                final var decoder        = new SnapshotDecoder(new BinaryCodec());

                updateMessage("Counting bundles and services...");
                final var bundleCount  = decoder.decodeList(agent.bundles(), XBundleDTO.class).size();
                final var serviceCount = decoder.decodeList(agent.services(), XServiceDTO.class).size();

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
                    final var dto     = new SnapshotDTO();
                    final var agent   = supervisor.getAgent();
                    final var decoder = new SnapshotDecoder(new BinaryCodec());

                    updateMessage("Capturing bundles (1/18)...");
                    dto.bundles = decoder.decodeList(agent.bundles(), XBundleDTO.class);
                    updateMessage("Capturing components (2/18)...");
                    dto.components = decoder.decodeList(agent.components(), XComponentDTO.class);
                    updateMessage("Capturing configurations (3/18)...");
                    dto.configurations = decoder.decodeList(agent.configurations(), XConfigurationDTO.class);
                    updateMessage("Capturing properties (4/18)...");
                    dto.properties = decoder.decodeList(agent.properties(), XPropertyDTO.class);
                    updateMessage("Capturing services (5/18)...");
                    dto.services = decoder.decodeList(agent.services(), XServiceDTO.class);
                    updateMessage("Capturing threads (6/18)...");
                    dto.threads = decoder.decodeList(agent.threads(), XThreadDTO.class);
                    updateMessage("Capturing DMT nodes (7/18)...");
                    dto.dmtNodes = agent.readDmtNode(".");
                    updateMessage("Capturing memory info (8/18)...");
                    dto.memoryInfo = agent.getMemoryInfo();
                    updateMessage("Capturing roles (9/18)...");
                    dto.roles = decoder.decodeList(agent.roles(), XRoleDTO.class);
                    updateMessage("Capturing health checks (10/18)...");
                    dto.healthChecks = decoder.decodeList(agent.healthChecks(), XHealthCheckDTO.class);
                    updateMessage("Capturing classloader leaks (11/18)...");
                    dto.classloaderLeaks = decoder.decodeSet(agent.leaks(), XBundleDTO.class);
                    updateMessage("Capturing HTTP components (12/18)...");
                    dto.httpComponents = decoder.decodeList(agent.httpComponents(), XHttpComponentDTO.class);
                    updateMessage("Capturing logger contexts (13/18)...");
                    dto.bundleLoggerContexts = decoder.decodeList(agent.bundleLoggerContexts(),
                            XBundleLoggerContextDTO.class);
                    updateMessage("Capturing JAX-RS components (14/18)...");
                    dto.jaxRsComponents = decoder.decodeList(agent.jaxRsComponents(), XJaxRsComponentDTO.class);
                    updateMessage("Capturing CDI containers (15/18)...");
                    dto.cdiContainers = decoder.decodeList(agent.cdiContainers(), XCdiContainerDTO.class);
                    updateMessage("Capturing runtime capabilities (16/18)...");
                    dto.runtimeCapabilities = decoder.decodeList(agent.runtimeCapabilities(),
                            XRuntimeCapabilityDTO.class);
                    updateMessage("Capturing heap usage (17/18)...");
                    dto.heapUsage = decoder.decode(agent.heapUsage(), XHeapUsageDTO.class);
                    updateMessage("Capturing runtime info (18/18)...");
                    dto.runtime = decoder.decode(agent.runtime(), RuntimeDTO.class);

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
        final var timestamp   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
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
