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
package com.osgifx.console.ui.overview;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;

import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.ConsoleStatusBar;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.colors.Bright;
import eu.hansolo.tilesfx.colors.Dark;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.supervisor.Supervisor)")
public final class OverviewFxUI {

	private static final double TILE_WIDTH  = 500;
	private static final double TILE_HEIGHT = 220;

	@Log
	@Inject
	private FluentLogger     logger;
	@Inject
	private ConsoleStatusBar statusBar;
	@Inject
	private Supervisor       supervisor;
	@Inject
	private DataProvider     dataProvider;
	@Inject
	@Named("is_connected")
	private boolean          isConnected;

	private volatile String frameworkBsn;
	private volatile String frameworkVersion;

	private volatile String osName;
	private volatile String osVersion;
	private volatile String osArchitecture;

	private volatile double noOfThreads;
	private volatile double noOfServices;
	private volatile double noOfComponents;
	private volatile double noOfInstalledBundles;

	private UptimeDTO      uptime;
	private XMemoryInfoDTO memoryInfo;

	@PostConstruct
	public void postConstruct(final BorderPane parent) {
		createControls(parent);
		logger.atDebug().log("Overview part has been initialized");
	}

	@Focus
	void focus(final BorderPane parent) {
		createControls(parent);
	}

	private void createControls(final BorderPane parent) {
		uptime = new UptimeDTO(0, 0, 0, 0);

		statusBar.addTo(parent);
		retrieveRuntimeInfo(parent);
		createWidgets(parent);
	}

	private void retrieveRuntimeInfo(final BorderPane parent) {
		final Task<Void> task = new Task<>() {

			@Override
			protected Void call() throws Exception {
				if (isConnected) {
					// wait a bit for the remote runtime information to be retrieved
					TimeUnit.SECONDS.sleep(3);
				}
				frameworkBsn         = isConnected ? dataProvider.bundles().get(0).symbolicName : "";
				frameworkVersion     = isConnected ? dataProvider.bundles().get(0).version : "";
				osName               = isConnected
				        ? dataProvider.properties().stream().filter(p -> "os.name".equals(p.name)).map(p -> p.value).map(Object::toString)
				                .findAny().orElse("")
				        : "";
				osVersion            = isConnected
				        ? dataProvider.properties().stream().filter(p -> "os.version".equals(p.name)).map(p -> p.value)
				                .map(Object::toString).findAny().orElse("")
				        : "";
				osArchitecture       = isConnected
				        ? dataProvider.properties().stream().filter(p -> "os.arch".equals(p.name)).map(p -> p.value).map(Object::toString)
				                .findAny().orElse("")
				        : "";
				noOfThreads          = isConnected ? dataProvider.threads().size() : 0d;
				noOfInstalledBundles = isConnected ? dataProvider.bundles().size() : 0d;
				noOfServices         = isConnected ? dataProvider.services().size() : 0d;
				noOfComponents       = isConnected ? dataProvider.components().size() : 0d;
				memoryInfo           = isConnected ? supervisor.getAgent().getMemoryInfo() : new XMemoryInfoDTO();
				uptime               = toUptimeEntry(memoryInfo.uptime);
				return null;
			}

			@Override
			protected void succeeded() {
				createWidgets(parent);
				updateProgress(0, 0);
			}
		};

		statusBar.progressProperty().bind(task.progressProperty());

		final var thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();
	}

	private void createWidgets(final BorderPane parent) {
		// @formatter:off
        final var clockTile = TileBuilder.create()
                                         .skinType(SkinType.CLOCK)
                                         .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                         .title("Today")
                                         .dateVisible(true)
                                         .locale(Locale.UK)
                                         .running(true)
                                         .styleClass("overview")
                                         .build();
        clockTile.setRoundedCorners(false);

        final var noOfThreadsTile = TileBuilder.create()
                                               .skinType(SkinType.NUMBER)
                                               .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                               .title("Threads")
                                               .text("Number of threads")
                                               .value(noOfThreads)
                                               .valueVisible(noOfThreads != 0.0d)
                                               .textVisible(true)
                                               .decimals(0)
                                               .build();
        noOfThreadsTile.setRoundedCorners(false);

        final var runtimeInfoTile = TileBuilder.create()
                                               .skinType(SkinType.CUSTOM)
                                               .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                               .title("Runtime Information")
                                               .graphic(createRuntimeTable())
                                               .valueVisible(memoryInfo != null)
                                               .text("")
                                               .build();
        runtimeInfoTile.setRoundedCorners(false);

        final var noOfBundlesTile = TileBuilder.create()
                                               .skinType(SkinType.NUMBER)
                                               .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                               .title("Bundles")
                                               .text("Number of installed bundles")
                                               .value(noOfInstalledBundles)
                                               .valueVisible(noOfInstalledBundles != 0.0d)
                                               .textVisible(true)
                                               .decimals(0)
                                               .build();
        noOfBundlesTile.setRoundedCorners(false);

        final var noOfServicesTile = TileBuilder.create()
                                                .skinType(SkinType.NUMBER)
                                                .numberFormat(new DecimalFormat("#"))
                                                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                .title("Services")
                                                .text("Number of registered services")
                                                .value(noOfServices)
                                                .valueVisible(noOfServices != 0.0d)
                                                .textVisible(true)
                                                .decimals(0)
                                                .build();
        noOfServicesTile.setRoundedCorners(false);

        final var noOfComponentsTile = TileBuilder.create()
                                                  .skinType(SkinType.NUMBER)
                                                  .numberFormat(new DecimalFormat("#"))
                                                  .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                  .title("Components")
                                                  .text("Number of registered components")
                                                  .value(noOfComponents)
                                                  .valueVisible(noOfComponents != 0.0d)
                                                  .textVisible(true)
                                                  .decimals(0)
                                                  .build();
        noOfComponentsTile.setRoundedCorners(false);

        final var freeMemoryInBytes = memoryInfo.freeMemory;
        final var totalMemoryInBytes = memoryInfo.totalMemory;

        final var freeMemoryInMB = toMB(freeMemoryInBytes);
        final var totalMemoryInMB = toMB(totalMemoryInBytes);

        final double usedMemory = totalMemoryInBytes - freeMemoryInBytes;
        final var memoryConsumptionInfo = totalMemoryInBytes == 0 ? 0D : usedMemory/totalMemoryInBytes;
        final var memoryConsumptionInfoInPercentage = memoryConsumptionInfo * 100;

        final var memoryConsumptionTile = TileBuilder.create()
                                                     .skinType(SkinType.PERCENTAGE)
                                                     .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                     .title("JVM Memory Consumption Percentage")
                                                     .textVisible(memoryConsumptionInfoInPercentage != 0D)
                                                     .roundedCorners(false)
                                                     .build();
        memoryConsumptionTile.setValue(memoryConsumptionInfoInPercentage);

        final var availableMemoryTile = TileBuilder.create()
                                                   .skinType(SkinType.BAR_GAUGE)
                                                   .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                   .minValue(0)
                                                   .maxValue(totalMemoryInMB)
                                                   .startFromZero(true)
                                                   .threshold(totalMemoryInMB * .8)
                                                   .thresholdVisible(true)
                                                   .title("JVM Allocated Memory")
                                                   .unit("MB")
                                                   .text("Allocated memory of the remote runtime")
                                                   .gradientStops(
                                                           new Stop(0, Bright.BLUE),
                                                           new Stop(0.1, Bright.BLUE_GREEN),
                                                           new Stop(0.2, Bright.GREEN),
                                                           new Stop(0.3, Bright.GREEN_YELLOW),
                                                           new Stop(0.4, Bright.YELLOW),
                                                           new Stop(0.5, Bright.YELLOW_ORANGE),
                                                           new Stop(0.6, Bright.ORANGE),
                                                           new Stop(0.7, Bright.ORANGE_RED),
                                                           new Stop(0.8, Bright.RED),
                                                           new Stop(1.0, Dark.RED))
                                                   .strokeWithGradient(true)
                                                   .animated(true)
                                                   .build();
        availableMemoryTile.setRoundedCorners(false);
        availableMemoryTile.setValue(totalMemoryInMB - freeMemoryInMB);

        final var uptimeTile = TileBuilder.create()
                                          .skinType(SkinType.TIME)
                                          .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                          .title("Uptime")
                                          .text("Uptime of the remote runtime")
                                          .duration(LocalTime.of(uptime.hours(), uptime.minutes()))
                                          .textVisible(true)
                                          .build();
        uptimeTile.setRoundedCorners(false);

        final var pane = new FlowGridPane(3, 3,
                                           clockTile,
                                           runtimeInfoTile,
                                           noOfThreadsTile,
                                           noOfBundlesTile,
                                           noOfServicesTile,
                                           noOfComponentsTile,
                                           memoryConsumptionTile,
                                           availableMemoryTile,
                                           uptimeTile);
        pane.setHgap(5);
        pane.setVgap(5);
        pane.setAlignment(Pos.CENTER);
        pane.setCenterShape(true);
        pane.setPadding(new Insets(5));
        pane.setBackground(new Background(new BackgroundFill(Color.web("#F1F1F1"), CornerRadii.EMPTY, Insets.EMPTY)));

        parent.setCenter(pane);
        // @formatter:on
	}

	private synchronized Node createRuntimeTable() {
		final var name = new Label("");
		name.setTextFill(Tile.FOREGROUND);
		name.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(name, Priority.NEVER);

		final var spacer = new Region();
		spacer.setPrefSize(5, 5);
		HBox.setHgrow(spacer, Priority.ALWAYS);

		final var views = new Label("");
		views.setTextFill(Tile.FOREGROUND);
		views.setAlignment(Pos.CENTER_RIGHT);
		HBox.setHgrow(views, Priority.NEVER);

		final var header = new HBox(5, name, spacer, views);
		header.setAlignment(Pos.CENTER_LEFT);
		header.setFillHeight(true);

		final var dataTable = new VBox(0, header);
		dataTable.setFillWidth(true);

		final Map<String, String> runtimeInfo = Map.of("Framework", frameworkBsn, "Framework Version", frameworkVersion, "Memory Total",
		        String.valueOf(memoryInfo.totalMemory), "Memory Free", String.valueOf(memoryInfo.freeMemory), "OS Name", osName,
		        "OS Version", osVersion, "OS Architecture", osArchitecture);

		final Map<String, String> filteredMap = Maps.newTreeMap();
		filteredMap.putAll(runtimeInfo);

		for (final Entry<String, String> entry : filteredMap.entrySet()) {
			final var key   = entry.getKey();
			final var value = entry.getValue();
			final var node  = getTileTableInfo(key, value);
			dataTable.getChildren().add(node);
		}
		return dataTable;
	}

	private HBox getTileTableInfo(final String property, final String value) {
		final var propertyLabel = new Label(property);
		propertyLabel.setTextFill(Tile.FOREGROUND);
		propertyLabel.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(propertyLabel, Priority.NEVER);

		final var spacer = new Region();
		spacer.setPrefSize(5, 5);
		HBox.setHgrow(spacer, Priority.ALWAYS);

		final var valueLabel = new Label(value);
		valueLabel.setTextFill(Tile.FOREGROUND);
		valueLabel.setAlignment(Pos.CENTER_RIGHT);
		HBox.setHgrow(valueLabel, Priority.NEVER);

		final var hBox = new HBox(5, propertyLabel, spacer, valueLabel);
		hBox.setAlignment(Pos.CENTER_LEFT);
		hBox.setFillHeight(true);

		return hBox;
	}

	private int toMB(final long sizeInBytes) {
		return (int) (sizeInBytes / 1024 / 1024);
	}

	private UptimeDTO toUptimeEntry(final long uptime) {
		final var days    = (int) TimeUnit.MILLISECONDS.toDays(uptime);
		final var hours   = (int) TimeUnit.MILLISECONDS.toHours(uptime) - days * 24;
		final var minutes = (int) (TimeUnit.MILLISECONDS.toMinutes(uptime) - TimeUnit.MILLISECONDS.toHours(uptime) * 60);
		final var seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(uptime) - TimeUnit.MILLISECONDS.toMinutes(uptime) * 60);

		return new UptimeDTO(days, hours, minutes, seconds);
	}

	private record UptimeDTO(int days, int hours, int minutes, int seconds) {
	}

	@Inject
	@Optional
	private void updateOnAgentConnectedEvent( //
	        @UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data, //
	        final BorderPane parent) {
		logger.atInfo().log("Agent connected event received");
		createControls(parent);
	}

	@Inject
	@Optional
	private void updateOnAgentDisconnectedEvent(//
	        @UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data, //
	        final BorderPane parent) {
		logger.atInfo().log("Agent disconnected event received");
		createControls(parent);
	}

}
