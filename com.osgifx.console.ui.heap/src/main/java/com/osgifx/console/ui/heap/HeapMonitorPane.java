/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.ui.heap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Lists;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XGarbageCollectorMXBean;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XMemoryPoolMXBean;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XMemoryUsage;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;
import com.osgifx.console.util.io.IO;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

@Creatable
public final class HeapMonitorPane extends BorderPane {

    private static final double REFRESH_DELAY = 2;

    private final List<HeapMonitorChart> memoryUsageCharts = Lists.newArrayList();
    private final StringProperty         totalUsedHeap     = new SimpleStringProperty();
    private final StringProperty         gcCollectionCount = new SimpleStringProperty();
    private final StringProperty         gcCollectionTime  = new SimpleStringProperty();
    private final StringProperty         maxHeap           = new SimpleStringProperty();
    private final StringProperty         uptTime           = new SimpleStringProperty();

    private Timeline animation;

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private Executor          executor;
    @Inject
    @Optional
    private Supervisor        supervisor;
    @Inject
    private DataProvider      dataProvider;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;
    private ProgressDialog    progressDialog;

    @PostConstruct
    public void init() {
        setTop(createControlPanel());

        final var box        = createMainContent();
        final var scrollPane = new ScrollPane(box);

        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);

        final var frame = new KeyFrame(Duration.seconds(REFRESH_DELAY),
                                       (final var actionEvent) -> updateHeapInformation());

        animation = new Timeline();
        animation.getKeyFrames().add(frame);
        animation.setCycleCount(Animation.INDEFINITE);
    }

    private Pane createMainContent() {
        final var box          = new VBox();
        final var vBoxChildren = box.getChildren();
        final var now          = System.currentTimeMillis();

        final Supplier<CompletableFuture<XMemoryUsage>> supplier = () -> {
            if (supervisor == null || supervisor.getAgent() == null) {
                return CompletableFuture.completedFuture(new XMemoryUsage());
            }
            return dataProvider.heapUsage().thenApply(u -> u.memoryUsage);
        };

        final var memoryUsageChartGlobal = new HeapMonitorChart("Heap", supplier, now);
        addToList(memoryUsageChartGlobal, vBoxChildren);

        final var separator = new Separator();
        separator.setPrefHeight(2);
        vBoxChildren.add(separator);

        if (supervisor == null || supervisor.getAgent() == null) {
            return box;
        }
        final var promise = dataProvider.heapUsage();
        promise.thenAccept(usage -> {
            if (usage == null) {
                return;
            }
            threadSync.asyncExec(() -> {
                for (final XMemoryPoolMXBean mpBean : usage.memoryPoolBeans) {
                    if ("HEAP".equals(mpBean.type)) {
                        final var memoryUsageChart = new HeapMonitorChart(mpBean.name,
                                                                          getMemoryUsageByMemoryPoolBean(mpBean), now);
                        addToList(memoryUsageChart, vBoxChildren);
                    }
                }
            });
        });
        return box;
    }

    private void addToList(final HeapMonitorChart memoryUsageChart, final ObservableList<Node> vBoxChildren) {
        memoryUsageChart.setPrefHeight(250);
        memoryUsageCharts.add(memoryUsageChart);
        vBoxChildren.add(memoryUsageChart);
        VBox.setVgrow(memoryUsageChart, Priority.ALWAYS);
    }

    private void updateHeapInformation() {
        dataProvider.heapUsage().thenAccept(usage -> {
            final var used = usage.memoryUsage.used;
            final var max  = usage.memoryUsage.max;

            threadSync.asyncExec(() -> {
                totalUsedHeap.setValue(formatByteSize(used));
                maxHeap.setValue(formatByteSize(max));
                memoryUsageCharts.forEach(HeapMonitorChart::update);

                updateGCStats(usage);
            });
        });
    }

    private void updateGCStats(final XHeapUsageDTO usage) {
        if (!isConnected) {
            return;
        }
        final var mbeanUptime     = usage.uptime;
        final var formattedUptime = formatTimeDifference(mbeanUptime);
        uptTime.setValue(formattedUptime);

        var                garbageCollectionTime = 0L;
        final List<String> gcCollections         = Lists.newArrayList();
        for (final XGarbageCollectorMXBean gc : usage.gcBeans) {
            gcCollections.add(gc.name + "=" + gc.collectionCount);
            garbageCollectionTime += gc.collectionTime;
        }
        final var formattedGCTime = formatTimeDifference(garbageCollectionTime);
        gcCollectionCount.setValue(String.join(", ", gcCollections));
        gcCollectionTime.setValue(formattedGCTime);

    }

    private static String formatByteSize(final long bytes) {
        final var unit = 1000;
        if (bytes < unit) {
            return bytes + " B";
        }
        final var exp = (int) (Math.log(bytes) / Math.log(unit));
        final var pre = Character.toString("kMGTPE".charAt(exp - 1));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private String formatTimeDifference(long durationInMilli) {
        final var secondsInMilli = 1_000L;
        final var minutesInMilli = secondsInMilli * 60;
        final var hoursInMilli   = minutesInMilli * 60;
        final var daysInMilli    = hoursInMilli * 24;

        final var days = durationInMilli / daysInMilli;
        durationInMilli = durationInMilli % daysInMilli;

        final var hours = durationInMilli / hoursInMilli;
        durationInMilli = durationInMilli % hoursInMilli;

        final var minutes = durationInMilli / minutesInMilli;
        durationInMilli = durationInMilli % minutesInMilli;

        final var seconds = durationInMilli / secondsInMilli;

        return String.format("%S day, %02d hr, %02d min, %02d sec", days, hours, minutes, seconds);

    }

    private Pane createControlPanel() {
        final var borderPane = new BorderPane();

        borderPane.setLeft(createLeftPane());
        borderPane.setRight(createRightPane());
        borderPane.setPadding(new Insets(5, 5, 5, 5));

        final var border = new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                                                       BorderWidths.DEFAULT, new Insets(5)));
        borderPane.setBorder(border);
        return borderPane;
    }

    private Pane createRightPane() {
        final var vBox = new VBox();
        vBox.setSpacing(5);
        final var children = vBox.getChildren();

        final var garbageCollectBtn = new Button("Garbage Collect");
        garbageCollectBtn.setDisable(true);
        garbageCollectBtn.setMaxWidth(Double.MAX_VALUE);
        garbageCollectBtn.setOnAction(e -> performGC());

        final var heapDumpBtn = new Button("Heap Dump");
        heapDumpBtn.setDisable(!isConnected || isSnapshotAgent);
        heapDumpBtn.setMaxWidth(Double.MAX_VALUE);
        heapDumpBtn.setOnAction(e -> heapDump());

        final var startStopBtn = new Button("Start");
        startStopBtn.setDisable(!isConnected || isSnapshotAgent);
        startStopBtn.setMaxWidth(Double.MAX_VALUE);
        startStopBtn.setOnAction(e -> {
            switch (animation.getStatus()) {
                case RUNNING:
                    animation.pause();
                    garbageCollectBtn.setDisable(true);
                    startStopBtn.setText("Start");
                    break;
                case PAUSED, STOPPED:
                default:
                    animation.play();
                    garbageCollectBtn.setDisable(false);
                    startStopBtn.setText("Stop");
                    break;
            }
        });

        children.add(startStopBtn);
        children.add(garbageCollectBtn);
        children.add(heapDumpBtn);

        return vBox;
    }

    private void performGC() {
        final var agent = supervisor.getAgent();
        if (agent == null) {
            return;
        }
        executor.runAsync(agent::gc);
    }

    private void heapDump() {
        if (!isConnected) {
            return;
        }
        final var directoryChooser = new DirectoryChooser();
        final var location         = directoryChooser.showDialog(null);
        if (location == null) {
            return;
        }
        final var agent = supervisor.getAgent();

        final Task<byte[]> heapdumpTask = new Task<>() {

            @Override
            protected byte[] call() throws Exception {
                try {
                    updateMessage("Capturing heapdump");
                    return agent.heapdump();
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot capture heapdump");
                    threadSync.asyncExec(() -> {
                        progressDialog.close();
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    });
                    throw e;
                }
            }
        };
        heapdumpTask.valueProperty().addListener((ChangeListener<byte[]>) (obs, oldValue, newValue) -> {
            if (newValue != null) {
                threadSync.asyncExec(() -> {
                    try {
                        final var heapdumpFile = new File(location, IO.prepareFilenameFor("hprof"));
                        FileUtils.writeByteArrayToFile(heapdumpFile, newValue);
                        threadSync.asyncExec(() -> Fx.showSuccessNotification("Heapdump Successfully Captured",
                                heapdumpFile.getAbsolutePath()));
                    } catch (final IOException e) {
                        FxDialog.showExceptionDialog(e, getClass().getClassLoader());
                    }
                });
            }
        });
        final var taskFuture = executor.runAsync(heapdumpTask);
        progressDialog = FxDialog.showProgressDialog("Capture Snapshpt", heapdumpTask, getClass().getClassLoader(),
                () -> taskFuture.cancel(true));
    }

    private Pane createLeftPane() {
        final var gridPane = new GridPane();

        gridPane.add(createLabel("Used Heap : "), 0, 0);
        final var usedHeadLabel = createRightSideLabel(totalUsedHeap);
        gridPane.add(usedHeadLabel, 1, 0);

        gridPane.add(createLabel("Max Heap : "), 0, 1);
        final var maxHeapLabel = createRightSideLabel(maxHeap);
        gridPane.add(maxHeapLabel, 1, 1);

        gridPane.add(createLabel("Up Time : "), 0, 2);
        final var uptimeLabel = createRightSideLabel(uptTime);
        gridPane.add(uptimeLabel, 1, 2);

        gridPane.add(createLabel("GC Count : "), 0, 3);
        final var gcCountLabel = createRightSideLabel(gcCollectionCount);
        gridPane.add(gcCountLabel, 1, 3);

        gridPane.add(createLabel("Total GC Time : "), 0, 4);
        final var getTimeLabel = createRightSideLabel(gcCollectionTime);
        gridPane.add(getTimeLabel, 1, 4);
        return gridPane;
    }

    private Label createRightSideLabel(final StringProperty totalUsedHeap) {
        final var label = new Label();
        label.textProperty().bind(totalUsedHeap);

        GridPane.setHalignment(label, HPos.LEFT);

        label.setTextAlignment(TextAlignment.LEFT);
        label.setPrefWidth(800);

        return label;
    }

    private Label createLabel(final String text) {
        final var label = new Label(text);
        GridPane.setHalignment(label, HPos.RIGHT);
        label.setTextAlignment(TextAlignment.RIGHT);
        return label;
    }

    public void startUpdates() {
        animation.play();
    }

    public void stopUpdates() {
        animation.pause();
    }

    public Supplier<CompletableFuture<XMemoryUsage>> getMemoryUsageByMemoryPoolBean(final XMemoryPoolMXBean bean) {
        return () -> {
            if (!isConnected) {
                return CompletableFuture.completedFuture(new XMemoryUsage());
            }
            final var promise = dataProvider.heapUsage();
            return promise.thenApply(usage -> Stream.of(usage.memoryPoolBeans).filter(m -> bean.name.equals(m.name))
                    .map(m -> m.memoryUsage).findAny().orElse(null));
        };
    }

}
