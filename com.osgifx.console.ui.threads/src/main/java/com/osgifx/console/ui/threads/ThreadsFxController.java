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
package com.osgifx.console.ui.threads;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.controlsfx.control.table.TableFilter;
import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;

import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.agent.spi.LargePayloadHandler;
import com.osgifx.console.agent.spi.PayloadMetadata;
import com.osgifx.console.agent.spi.PayloadType;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;
import com.osgifx.console.util.io.IO;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

public final class ThreadsFxController {

    @Log
    @Inject
    private FluentLogger                    logger;
    @FXML
    private TableView<XThreadDTO>           table;
    @FXML
    private TableColumn<XThreadDTO, String> nameColumn;
    @FXML
    private TableColumn<XThreadDTO, String> idColumn;
    @FXML
    private TableColumn<XThreadDTO, String> priorityColumn;
    @FXML
    private TableColumn<XThreadDTO, String> stateColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isInterruptedColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isAliveColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isDaemonColumn;
    @FXML
    private TableColumn<XThreadDTO, String> isDeadlockedColumn;
    @Inject
    @Named("is_connected")
    private boolean                         isConnected;
    @Inject
    private DataProvider                    dataProvider;
    @Inject
    private ThreadSynchronize               threadSync;
    @Inject
    private IEclipseContext                 eclipseContext;
    @Inject
    private Executor                        executor;
    @Inject
    @Optional
    private Supervisor                      supervisor;
    @FXML
    private Button                          threadDumpButton;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                         isSnapshotAgent;
    private boolean                         isInitialized;
    private ProgressDialog                  progressDialog;

    @FXML
    public void initialize() {
        initButtonIcons();
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            updateButtonStates();
            return;
        }
        try {
            if (!isInitialized) {
                initCells();
                Fx.addContextMenuToCopyContent(table);
                isInitialized = true;
            }
            updateButtonStates();
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    @FXML
    public void threadDump() {
        if (!isConnected) {
            return;
        }
        final var agent = supervisor.getAgent();
        if (agent == null) {
            return;
        }

        // Prepare thread dump statistics in background to avoid blocking UI
        final Task<ThreadDumpOptionsDialog.ThreadDumpStatistics> prepTask = new Task<>() {
            @Override
            protected ThreadDumpOptionsDialog.ThreadDumpStatistics call() throws Exception {
                updateMessage("Estimating thread dump size...");
                final long estimatedUncompressedSize = agent.estimateThreadDumpSize();
                // GZIP compresses plain text ~8-10x; use 10% as conservative compressed estimate
                final long    estimatedCompressedSize = estimatedUncompressedSize / 10;
                final long    rpcLimit                = 200_000_000L;
                final boolean rpcAvailable            = estimatedCompressedSize <= rpcLimit;
                final boolean isMqttTransport         = supervisor.getConnectionType().equals("MQTT");

                final var handlerTracker = supervisor.getLargePayloadHandlerTracker();
                final var handler        = handlerTracker != null ? handlerTracker.getService() : null;

                return new ThreadDumpOptionsDialog.ThreadDumpStatistics(estimatedUncompressedSize,
                                                                        estimatedCompressedSize, isMqttTransport,
                                                                        rpcAvailable, handler);
            }
        };

        prepTask.setOnSucceeded(_ -> {
            final var stats = prepTask.getValue();

            final var dialog = new ThreadDumpOptionsDialog();
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
                    threadDumpWithHandler(handler);
                    break;
                case USE_RPC:
                    threadDumpWithRpc();
                    break;
                case STORE_LOCALLY:
                    threadDumpLocally();
                    break;
            }
        });

        prepTask.setOnFailed(_ -> {
            final var ex = prepTask.getException();
            logger.atError().withException(ex).log("Failed to prepare thread dump statistics");
            threadSync.asyncExec(() -> FxDialog.showExceptionDialog(ex, getClass().getClassLoader()));
        });

        final var prepFuture = executor.runAsync(prepTask);
        FxDialog.showProgressDialog("Preparing Thread Dump", prepTask, getClass().getClassLoader(),
                () -> prepFuture.cancel(true));
    }

    private void threadDumpWithHandler(final LargePayloadHandler handler) {
        final var agent = supervisor.getAgent();

        final Task<String> threadDumpTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                try {
                    updateMessage("Stage 1/3: Generating thread dump file");
                    final var localPath = agent.createThreadDumpLocally("/tmp/threaddumps");

                    updateMessage("Stage 2/3: Uploading via handler");
                    final var metadata = new PayloadMetadata(new File(localPath).getName(),
                                                             new File(localPath).length(), "application/gzip",
                                                             PayloadType.THREADDUMP, System.currentTimeMillis());
                    final var result   = handler.handle(localPath, metadata);

                    updateMessage("Stage 3/3: Finalizing");
                    if (!result.success) {
                        throw new Exception("Handler failed: " + result.errorMessage);
                    }
                    return result.location;
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot capture thread dump with handler");
                    threadSync.asyncExec(() -> {
                        if (progressDialog != null) {
                            progressDialog.close();
                        }
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
            }
        };

        threadDumpTask.valueProperty().addListener((ChangeListener<String>) (_, _, location) -> {
            if (location != null) {
                threadSync.asyncExec(() -> Fx.showSuccessNotification("Thread Dump Successfully Uploaded", location));
            }
        });

        final var taskFuture = executor.runAsync(threadDumpTask);
        progressDialog = FxDialog.showProgressDialog("Capture Thread Dump", threadDumpTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    private void threadDumpWithRpc() {
        final var directoryChooser = new DirectoryChooser();
        final var location         = directoryChooser.showDialog(null);
        if (location == null) {
            return;
        }
        final var agent = supervisor.getAgent();

        final Task<byte[]> threadDumpTask = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                try {
                    updateMessage("Stage 1/3: Generating thread dump");
                    Thread.sleep(100);
                    updateMessage("Stage 2/3: Compressing thread dump");
                    Thread.sleep(100);
                    final var data = agent.threadDump();
                    updateMessage("Stage 3/3: Transferring thread dump");
                    return data;
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot capture thread dump");
                    threadSync.asyncExec(() -> {
                        if (progressDialog != null) {
                            progressDialog.close();
                        }
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
            }
        };

        threadDumpTask.valueProperty().addListener((ChangeListener<byte[]>) (_, _, newValue) -> {
            if (newValue != null) {
                threadSync.asyncExec(() -> {
                    try {
                        final var threadDumpFile = new File(location, IO.prepareFilenameFor("tdump.gz"));
                        FileUtils.writeByteArrayToFile(threadDumpFile, newValue);
                        Fx.showSuccessNotification("Thread Dump Successfully Captured",
                                threadDumpFile.getAbsolutePath());
                    } catch (final IOException e) {
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    }
                });
            }
        });

        final var taskFuture = executor.runAsync(threadDumpTask);
        progressDialog = FxDialog.showProgressDialog("Capture Thread Dump", threadDumpTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    private void threadDumpLocally() {
        final var pathDialog = new ThreadDumpPathPromptDialog();
        ContextInjectionFactory.inject(pathDialog, eclipseContext);
        final var timestamp   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        final var defaultPath = "{java.io.tmpdir}/threaddump-" + timestamp + ".tdump.gz";
        pathDialog.init(defaultPath, "Specify File Path",
                "Enter the full file path on the agent where the thread dump should be saved (including filename).\n"
                        + "System/Framework properties (e.g., {java.io.tmpdir}) and environment variables (e.g., {env:TEMP}) can be used:");

        final var pathResult = pathDialog.showAndWait();
        if (pathResult.isEmpty()) {
            return;
        }

        final var directoryPath = pathResult.get();
        final var agent         = supervisor.getAgent();

        final Task<String> threadDumpTask = new Task<>() {

            @Override
            protected String call() throws Exception {
                try {
                    updateMessage("Stage 1/3: Generating thread dump file");
                    Thread.sleep(100);
                    updateMessage("Stage 2/3: Compressing thread dump");
                    Thread.sleep(100);
                    final var localPath = agent.createThreadDumpLocally(directoryPath);
                    updateMessage("Stage 3/3: Finalizing");
                    return localPath;
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot create local thread dump");
                    threadSync.asyncExec(() -> {
                        if (progressDialog != null) {
                            progressDialog.close();
                        }
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
            }
        };

        threadDumpTask.valueProperty().addListener((ChangeListener<String>) (_, _, localPath) -> {
            if (localPath != null) {
                threadSync.asyncExec(() -> Fx.showSuccessNotification("Thread Dump Saved Locally",
                        "File saved at: " + localPath + "\nRetrieve via SFTP/SCP"));
            }
        });

        final var taskFuture = executor.runAsync(threadDumpTask);
        progressDialog = FxDialog.showProgressDialog("Capture Thread Dump", threadDumpTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    private void initButtonIcons() {
        final var image     = new Image(getClass().getResourceAsStream("/graphic/icons/threaddump.png"));
        final var imageView = new ImageView(image);
        imageView.setFitHeight(16.0);
        imageView.setFitWidth(16.0);
        imageView.setPreserveRatio(true);
        threadDumpButton.setGraphic(imageView);
    }

    private void updateButtonStates() {
        final var disableActions = !isConnected || isSnapshotAgent;
        threadDumpButton.setDisable(disableActions);
    }

    private void initCells() {
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        Fx.addCellFactory(nameColumn, b -> b.isDeadlocked, Color.RED, Color.BLACK);

        idColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        priorityColumn.setCellValueFactory(new DTOCellValueFactory<>("priority", String.class));
        stateColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));
        isInterruptedColumn.setCellValueFactory(new DTOCellValueFactory<>("isInterrupted", String.class));
        isAliveColumn.setCellValueFactory(new DTOCellValueFactory<>("isAlive", String.class));
        isDaemonColumn.setCellValueFactory(new DTOCellValueFactory<>("isDaemon", String.class));

        isDeadlockedColumn.setCellValueFactory(new DTOCellValueFactory<>("isDeadlocked", String.class));
        Fx.addCellFactory(isDeadlockedColumn, b -> b.isDeadlocked, Color.RED, Color.BLACK);

        threadSync.asyncExec(() -> {
            table.setItems(dataProvider.threads());
            TableFilter.forTableView(table).lazy(true).apply();
            threadSync.asyncExec(() -> {
                table.getSortOrder().add(nameColumn);
                table.sort();
            });
        });
    }

}
