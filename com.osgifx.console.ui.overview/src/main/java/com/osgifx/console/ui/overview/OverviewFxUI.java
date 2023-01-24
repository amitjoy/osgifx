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
package com.osgifx.console.ui.overview;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_ALL_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static com.osgifx.console.ui.overview.OverviewFxUI.TimelineButtonType.PAUSE;
import static com.osgifx.console.ui.overview.OverviewFxUI.TimelineButtonType.PLAY;
import static java.util.Objects.requireNonNullElse;
import static javafx.animation.Animation.INDEFINITE;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.glyphfont.Glyph;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Maps;
import com.google.mu.util.stream.BiStream;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.ui.ConsoleStatusBar;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.colors.Bright;
import eu.hansolo.tilesfx.colors.Dark;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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
import javafx.util.Duration;

public final class OverviewFxUI {

    private static final double TILE_WIDTH  = 500;
    private static final double TILE_HEIGHT = 220;

    @Log
    @Inject
    private FluentLogger     logger;
    @Inject
    private ConsoleStatusBar statusBar;
    @Inject
    private DataProvider     dataProvider;
    @Inject
    @Named("is_connected")
    private boolean          isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean          isSnapshotAgent;

    private Tile noOfThreadsTile;
    private Tile runtimeInfoTile;
    private Tile noOfBundlesTile;
    private Tile noOfServicesTile;
    private Tile noOfComponentsTile;
    private Tile memoryConsumptionTile;
    private Tile availableMemoryTile;
    private Tile uptimeTile;

    private Button   timelineButton;
    private Timeline dataRetrieverTimeline;

    private final AtomicBoolean isRealtimeUpdateRunning = new AtomicBoolean(true);

    @PostConstruct
    public void postConstruct(final BorderPane parent) {
        createTimelineButton();
        retrieveRuntimeInfo();
        createTiles(parent);
        createPeriodicTaskToSetRuntimeInfo();

        dataRetrieverTimeline.play();
        logger.atDebug().log("Overview part has been initialized");
    }

    @Focus
    public void onFocus(final BorderPane parent) {
        if (isRealtimeUpdateRunning.get()) {
            // This is required as a workaround to ensure that after the tab gets focused,
            // the CSS overridden problem gets overridden once again with the TileFX
            // embedded CSS and the number tiles are shown properly
            createTiles(parent);
        }
    }

    @PreDestroy
    public void destroy() {
        dataRetrieverTimeline.stop();
    }

    enum TimelineButtonType {
        PLAY("PLAY", "Play Real-time Update"),
        PAUSE("PAUSE", "Pause Real-time Update");

        String glyph;
        String tooltip;

        TimelineButtonType(final String glyph, final String tooltip) {
            this.glyph   = glyph;
            this.tooltip = tooltip;
        }
    }

    private void createTimelineButton() {
        timelineButton = new Button("");
        updateTimelineButtonTo(PAUSE);
    }

    private void updateTimelineButtonTo(final TimelineButtonType buttonType) {
        final var glyph = new Glyph("FontAwesome", buttonType.glyph);
        glyph.useGradientEffect();
        glyph.useHoverEffect();

        timelineButton.setGraphic(glyph);
        timelineButton.setTooltip(new Tooltip(buttonType.tooltip));
        timelineButton.setOnMouseClicked(mouseEvent -> {
            if (buttonType == PLAY) {
                playTimelineAnimation();
            } else {
                pauseTimelineAnimation();
            }
        });
    }

    private void playTimelineAnimation() {
        dataRetrieverTimeline.play();
        updateTimelineButtonTo(PAUSE);
        isRealtimeUpdateRunning.set(true);
    }

    private void pauseTimelineAnimation() {
        dataRetrieverTimeline.pause();
        updateTimelineButtonTo(PLAY);
        isRealtimeUpdateRunning.set(false);
    }

    private void createPeriodicTaskToSetRuntimeInfo() {
        dataRetrieverTimeline = new Timeline(new KeyFrame(Duration.seconds(1), a -> {

            final var runtimeInfo = retrieveRuntimeInfo();

            noOfBundlesTile.setValue(runtimeInfo.noOfInstalledBundles());
            noOfServicesTile.setValue(runtimeInfo.noOfServices());
            noOfComponentsTile.setValue(runtimeInfo.noOfComponents());
            noOfThreadsTile.setValue(runtimeInfo.noOfThreads());

            final var memoryInfo = runtimeInfo.memoryInfo();

            final var freeMemoryInBytes  = memoryInfo.freeMemory;
            final var totalMemoryInBytes = memoryInfo.totalMemory;
            final var freeMemoryInMB     = toMB(freeMemoryInBytes);
            final var totalMemoryInMB    = toMB(totalMemoryInBytes);

            availableMemoryTile.setValue(freeMemoryInMB);
            availableMemoryTile.setMaxValue(totalMemoryInMB);
            availableMemoryTile.setThreshold(totalMemoryInMB * .8);

            var memoryConsumptionInfoInPercentage = 0D;
            if (totalMemoryInBytes != 0) {
                memoryConsumptionInfoInPercentage = (totalMemoryInBytes - freeMemoryInBytes) * 100D
                        / totalMemoryInBytes;
            }

            memoryConsumptionTile.setValue(memoryConsumptionInfoInPercentage);

            final var uptime = runtimeInfo.uptime();
            uptimeTile.setDuration(LocalTime.of(uptime.hours(), uptime.minutes(), uptime.seconds()));

            // @formatter:off
            runtimeInfoTile.setGraphic(
                    createRuntimeTable(
                            runtimeInfo.frameworkBsn(),
                            runtimeInfo.frameworkVersion(),
                            runtimeInfo.frameworkStartLevel(),
                            runtimeInfo.osName(),
                            runtimeInfo.osVersion(),
                            runtimeInfo.osArchitecture(),
                            runtimeInfo.javaVersion()));
            // @formatter:on
        }));
        dataRetrieverTimeline.setCycleCount(INDEFINITE);
    }

    private OverviewInfo retrieveRuntimeInfo() {
        if (!isConnected) {
            return new OverviewInfo();
        }
        // @formatter:off
        final var frameworkBsn         = dataProvider.bundles().stream()
                                                               .findFirst()
                                                               .map(b -> b.symbolicName)
                                                               .orElse("");

        final var frameworkVersion     = dataProvider.bundles().stream()
                                                               .findFirst()
                                                               .map(b -> b.version)
                                                               .orElse("");

        final var frameworkStartLevel  = dataProvider.bundles().stream()
                                                               .findFirst()
                                                               .map(b -> b.frameworkStartLevel)
                                                               .map(Object::toString)
                                                               .orElse("");

        final var osName               = dataProvider.properties().stream()
                                                                  .filter(p -> "os.name".equals(p.name))
                                                                  .map(p -> p.value)
                                                                  .map(Object::toString)
                                                                  .findAny()
                                                                  .orElse("");

        final var osVersion            = dataProvider.properties().stream()
                                                                  .filter(p -> "os.version".equals(p.name))
                                                                  .map(p -> p.value)
                                                                  .map(Object::toString)
                                                                  .findAny()
                                                                  .orElse("");

        final var osArchitecture       = dataProvider.properties().stream()
                                                                  .filter(p -> "os.arch".equals(p.name))
                                                                  .map(p -> p.value)
                                                                  .map(Object::toString)
                                                                  .findAny()
                                                                  .orElse("");

        final var javaVersion          = dataProvider.properties().stream()
                                                                  .filter(p -> "java.version".equals(p.name))
                                                                  .map(p -> p.value)
                                                                  .map(Object::toString)
                                                                  .findAny()
                                                                  .orElse("");

        final var noOfThreads          = dataProvider.threads().size();
        final var noOfInstalledBundles = dataProvider.bundles().size();
        final var noOfServices         = dataProvider.services().size();
        final var noOfComponents       = dataProvider.components().size();
        final var memoryInfo           = requireNonNullElse(dataProvider.memory(), new XMemoryInfoDTO());
        final var uptime               = toUptimeEntry(memoryInfo.uptime);
        // @formatter:on

        return new OverviewInfo(frameworkBsn, frameworkVersion, frameworkStartLevel, osName, osVersion, osArchitecture,
                                javaVersion, noOfThreads, noOfInstalledBundles, noOfServices, noOfComponents,
                                memoryInfo, uptime);
    }

    private void createTiles(final BorderPane parent) {
        // @formatter:off
        final var clockTile = TileBuilder.create()
                                         .skinType(SkinType.CLOCK)
                                         .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                         .title("Today")
                                         .dateVisible(true)
                                         .locale(Locale.UK)
                                         .running(true)
                                         .styleClass("overview")
                                         .roundedCorners(false)
                                         .build();

        noOfThreadsTile = TileBuilder.create()
                                     .skinType(SkinType.NUMBER)
                                     .numberFormat(new DecimalFormat("#"))
                                     .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                     .title("Threads")
                                     .text("Number of threads")
                                     .textVisible(true)
                                     .decimals(0)
                                     .roundedCorners(false)
                                     .build();

        runtimeInfoTile = TileBuilder.create()
                                     .skinType(SkinType.CUSTOM)
                                     .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                     .title("Runtime Information")
                                     .text("")
                                     .roundedCorners(false)
                                     .build();

        noOfBundlesTile = TileBuilder.create()
                                     .skinType(SkinType.NUMBER)
                                     .numberFormat(new DecimalFormat("#"))
                                     .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                     .title("Bundles")
                                     .text("Number of installed bundles")
                                     .textVisible(true)
                                     .roundedCorners(false)
                                     .decimals(0)
                                     .build();

        noOfServicesTile = TileBuilder.create()
                                      .skinType(SkinType.NUMBER)
                                      .numberFormat(new DecimalFormat("#"))
                                      .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                      .title("Services")
                                      .text("Number of registered services")
                                      .textVisible(true)
                                      .roundedCorners(false)
                                      .decimals(0)
                                      .build();

        noOfComponentsTile = TileBuilder.create()
                                        .skinType(SkinType.NUMBER)
                                        .numberFormat(new DecimalFormat("#"))
                                        .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                        .title("Components")
                                        .text("Number of registered components")
                                        .textVisible(true)
                                        .roundedCorners(false)
                                        .decimals(0)
                                        .build();

        memoryConsumptionTile = TileBuilder.create()
                                           .skinType(SkinType.PERCENTAGE)
                                           .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                           .title("JVM Memory Consumption Percentage")
                                           .roundedCorners(false)
                                           .build();

        availableMemoryTile = TileBuilder.create()
                                         .skinType(SkinType.BAR_GAUGE)
                                         .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                         .minValue(0)
                                         .startFromZero(true)
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
                                         .roundedCorners(false)
                                         .build();

        uptimeTile = TileBuilder.create()
                                .skinType(SkinType.TIME)
                                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                .title("Uptime")
                                .text("Uptime of the remote runtime")
                                .textVisible(true)
                                .roundedCorners(false)
                                .duration(LocalTime.of(0, 0, 0))
                                .build();

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
        // @formatter:on
        pane.setHgap(5);
        pane.setVgap(5);
        pane.setAlignment(Pos.CENTER);
        pane.setCenterShape(true);
        pane.setPadding(new Insets(5));
        pane.setBackground(new Background(new BackgroundFill(Color.web("#F1F1F1"), CornerRadii.EMPTY, Insets.EMPTY)));

        parent.setCenter(pane);
        initStatusBar(parent);
    }

    private Node createRuntimeTable(final String frameworkBsn,
                                    final String frameworkVersion,
                                    final String frameworkStartLevel,
                                    final String osName,
                                    final String osVersion,
                                    final String osArchitecture,
                                    final String javaVersion) {
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

        // @formatter:off
        final Map<String, String> runtimeInfo =
                Map.of(
                        "Framework", frameworkBsn,
                        "Framework Version", frameworkVersion,
                        "Framework Start Level", frameworkStartLevel,
                        "Java Version", javaVersion,
                        "OS Name", osName,
                        "OS Version", osVersion,
                        "OS Architecture", osArchitecture);
        // @formatter:on

        final Map<String, String> filteredMap = Maps.newTreeMap();
        filteredMap.putAll(runtimeInfo);

        BiStream.from(filteredMap).mapToObj(this::getTileTableInfo).forEach(n -> dataTable.getChildren().add(n));
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

    private long toMB(final long sizeInBytes) {
        return sizeInBytes / 1024 / 1024;
    }

    private UptimeDTO toUptimeEntry(final long uptime) {
        final var days    = (int) TimeUnit.MILLISECONDS.toDays(uptime);
        final var hours   = (int) TimeUnit.MILLISECONDS.toHours(uptime) - days * 24;
        final var minutes = (int) (TimeUnit.MILLISECONDS.toMinutes(uptime)
                - TimeUnit.MILLISECONDS.toHours(uptime) * 60);
        final var seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(uptime)
                - TimeUnit.MILLISECONDS.toMinutes(uptime) * 60);

        return new UptimeDTO(days, hours, minutes, seconds);
    }

    private record UptimeDTO(int days, int hours, int minutes, int seconds) {
    }

    private record OverviewInfo(String frameworkBsn,
                                String frameworkVersion,
                                String frameworkStartLevel,
                                String osName,
                                String osVersion,
                                String osArchitecture,
                                String javaVersion,
                                int noOfThreads,
                                int noOfInstalledBundles,
                                int noOfServices,
                                int noOfComponents,
                                XMemoryInfoDTO memoryInfo,
                                UptimeDTO uptime) {
        public OverviewInfo() {
            this("", "", "", "", "", "", "", 0, 0, 0, 0, new XMemoryInfoDTO(), new UptimeDTO(0, 0, 0, 0));
        }
    }

    @Inject
    @Optional
    private void updateOnAgentConnectedEvent(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data,
                                             final BorderPane parent) {
        logger.atInfo().log("Agent connected event received");
        dataRetrieverTimeline.play();
        parent.setBottom(null);
        statusBar.addTo(parent);
    }

    @Inject
    @Optional
    private void updateOnAgentDisconnectedEvent(@UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data,
                                                final BorderPane parent) {
        logger.atInfo().log("Agent disconnected event received");
        dataRetrieverTimeline.stop();

        retrieveRuntimeInfo();
        createTiles(parent);
        createPeriodicTaskToSetRuntimeInfo();
        dataRetrieverTimeline.play();
    }

    /*
     * This is only required as a workaround to ensure that after the progress
     * dialog is closed, the CSS overridden problem gets overridden once again with
     * the TileFX embedded CSS and the number tiles are shown properly
     */
    @Inject
    @Optional
    private void updateOnDataRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_ALL_TOPIC) final String data,
                                            final BorderPane parent) {
        logger.atInfo().log("All data retrieved event received");
        createTiles(parent);
    }

    private void initStatusBar(final BorderPane parent) {
        if (isConnected) {
            statusBar.clearAllInRight();
            if (!isSnapshotAgent) {
                statusBar.addToRight(timelineButton);
            }
        } else {
            statusBar.clearAllInRight();
        }
        statusBar.addTo(parent);
    }

}
