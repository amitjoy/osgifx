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
package com.osgifx.console.ui.heap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.fx.core.ThreadSynchronize;

import com.osgifx.console.agent.dto.XHeapUsageDTO.XGarbageCollectorMXBean;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XMemoryPoolMXBean;
import com.osgifx.console.agent.dto.XHeapUsageDTO.XMemoryUsage;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
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
import javafx.util.Duration;

public final class HeapMonitorPane extends BorderPane {

	private final List<HeapMonitorChart> memoryUsageCharts = new ArrayList<>();
	private final StringProperty         totalUsedHeap     = new SimpleStringProperty();
	private final StringProperty         gcCollectionCount = new SimpleStringProperty();
	private final StringProperty         gcCollectionTime  = new SimpleStringProperty();
	private final StringProperty         maxHeap           = new SimpleStringProperty();
	private final StringProperty         uptTime           = new SimpleStringProperty();

	private final Timeline          animation;
	private final Supervisor        supervisor;
	private final ThreadSynchronize threadSync;

	public HeapMonitorPane(final Supervisor supervisor, final ThreadSynchronize threadSync) {
		this.supervisor = supervisor;
		this.threadSync = threadSync;
		setTop(createControlPanel());
		final var box        = createMainContent();
		final var scrollPane = new ScrollPane(box);
		scrollPane.setFitToWidth(true);
		setCenter(scrollPane);
		final var frame = new KeyFrame(Duration.millis(1_000), (final var actionEvent) -> updateHeapInformation());
		animation = new Timeline();
		animation.getKeyFrames().add(frame);
		animation.setCycleCount(Animation.INDEFINITE);
	}

	private Pane createMainContent() {
		final var box          = new VBox();
		final var vBoxChildren = box.getChildren();
		final var now          = System.currentTimeMillis();

		final Supplier<XMemoryUsage> supplier = () -> {
			final var agent = supervisor.getAgent();
			if (agent == null || agent.getHeapUsage() == null) {
				return new XMemoryUsage();
			}
			return agent.getHeapUsage().memoryUsage;
		};

		final var memoryUsageChartGlobal = new HeapMonitorChart("Heap", supplier, now);
		addToList(memoryUsageChartGlobal, vBoxChildren);
		final var separator = new Separator();
		separator.setPrefHeight(2);
		vBoxChildren.add(separator);
		final var agent = supervisor.getAgent();
		if (agent != null && agent.getHeapUsage() != null) {
			for (final XMemoryPoolMXBean mpBean : agent.getHeapUsage().memoryPoolBeans) {
				if ("HEAP".equals(mpBean.type)) {
					final var memoryUsageChart = new HeapMonitorChart(mpBean.name, getMemoryUsagedByMemoryPoolBean(mpBean), now);
					addToList(memoryUsageChart, vBoxChildren);
				}
			}
		}
		return box;
	}

	private void addToList(final HeapMonitorChart memoryUsageChart, final ObservableList<Node> vBoxChildren) {
		memoryUsageChart.setPrefHeight(250);
		memoryUsageCharts.add(memoryUsageChart);
		vBoxChildren.add(memoryUsageChart);
		VBox.setVgrow(memoryUsageChart, Priority.ALWAYS);
	}

	private void updateHeapInformation() {
		final var agent = supervisor.getAgent();
		if (agent == null || agent.getHeapUsage() == null) {
			return;
		}
		final var used = agent.getHeapUsage().memoryUsage.used;
		totalUsedHeap.setValue(formatByteSize(used));

		final var max = agent.getHeapUsage().memoryUsage.max;
		maxHeap.setValue(formatByteSize(max));

		for (final HeapMonitorChart memoryUsageChart : memoryUsageCharts) {
			memoryUsageChart.update();
		}
		updateGCStats();
	}

	private void updateGCStats() {
		final var agent = supervisor.getAgent();
		if (agent == null || agent.getHeapUsage() == null) {
			return;
		}
		final var mbeanUptime     = agent.getHeapUsage().uptime;
		final var formattedUptime = formatTimeDifference(mbeanUptime);
		uptTime.setValue(formattedUptime);

		var                garbageCollectionTime = 0L;
		final List<String> gcCollections         = new ArrayList<>();
		for (final XGarbageCollectorMXBean gc : agent.getHeapUsage().gcBeans) {
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
		final var border = new Border(
		        new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT, new Insets(5)));
		borderPane.setBorder(border);
		return borderPane;
	}

	private Pane createRightPane() {
		final var vBox = new VBox();
		vBox.setSpacing(5);
		final var children = vBox.getChildren();

		final var garbageCollect = new Button("Garbage Collect");
		garbageCollect.setMaxWidth(Double.MAX_VALUE);
		garbageCollect.setOnAction(e -> performGC());
		children.add(garbageCollect);

		final var heapDump = new Button("Heap Dump");
		heapDump.setMaxWidth(Double.MAX_VALUE);
		heapDump.setOnAction(e -> heapDump());
		children.add(heapDump);

		return vBox;
	}

	private void performGC() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			return;
		}
		agent.gc();
	}

	private void heapDump() {
		final var agent = supervisor.getAgent();
		if (agent == null) {
			return;
		}
		try {
			final var heapdump = agent.heapdump();
			if (heapdump == null) {
				return;
			}
			threadSync.asyncExec(() -> Fx.showSuccessNotification("Heapdump Successfully Created",
			        "File: " + heapdump.location + ", size: " + formatByteSize(heapdump.size)));
		} catch (final Exception e) {
			threadSync.asyncExec(() -> {
				final var message = Optional.ofNullable(e.getMessage()).orElse("Heapdump cannot be created due to runtime errors");
				Fx.showErrorNotification("Heapdump Processing Error", message);
			});
		}
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

	public Supplier<XMemoryUsage> getMemoryUsagedByMemoryPoolBean(final XMemoryPoolMXBean bean) {
		return () -> {
			final var agent = supervisor.getAgent();
			if (agent == null || agent.getHeapUsage() == null) {
				return new XMemoryUsage();
			}
			final var heapUsage = agent.getHeapUsage();
			for (final XMemoryPoolMXBean mpbean : heapUsage.memoryPoolBeans) {
				if (bean.name.equals(mpbean.name)) {
					return mpbean.memoryUsage;
				}
			}
			return null;
		};
	}

}
