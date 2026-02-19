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
package com.osgifx.console.ui.overview;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_ANY_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static com.osgifx.console.ui.overview.OverviewFxUI.TimelineButtonType.PAUSE;
import static com.osgifx.console.ui.overview.OverviewFxUI.TimelineButtonType.PLAY;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javafx.geometry.Orientation.VERTICAL;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.glyphfont.Glyph;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.chart.ChartData;
import eu.hansolo.tilesfx.colors.Bright;
import eu.hansolo.tilesfx.colors.Dark;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public final class OverviewFxUI {


    private static final double REFRESH_DELAY = 5;
    private static final int    CYCLE_COUNT   = 5;
    private static final Color  MUTED_GREEN   = Color.web("#3EB16E");
    private static final Color  MUTED_RED     = Color.web("#CE4844");

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private BorderPane        parent;
    @Inject
    private ConsoleStatusBar  statusBar;
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
    @Inject
    private IEclipseContext   eclipseContext;

    private Tile runtimeInfoTile;
    private Tile noOfServicesTile;
    private Tile memoryConsumptionTile;
    private Tile uptimeTile;

    // New Tiles
    private Tile noOfConfigurationsTile;
    private Tile noOfLeaksTile;
    private Tile deadlockedThreadsTile;

    // New Tiles
    private Tile bundleStateTile;
    private Tile memoryHistoryTile;
    private Tile threadStateTile;
    private Tile logErrorTile;
    private Tile componentStateTile;

    // Chart Data Series
    private XYChart.Series<String, Number> memoryDataSeries;

    private Button   timelineButton;
    private Timeline dataRetrieverTimeline;

    private double              refreshDelayInSeconds   = REFRESH_DELAY;
    private final AtomicBoolean isRealtimeUpdateRunning = new AtomicBoolean(true);

    @PostConstruct
    public void postConstruct() {
        createTimelineButton();
        updateStaticOverviewInfo();
        retrieveRuntimeInfo();
        createUIComponents(parent);
        initTimeline();

        logger.atDebug().log("Overview part has been initialized");
    }

    @Focus
    public void onFocus(final BorderPane parent) {
        if (isRealtimeUpdateRunning.get()) {
            initTimeline();
            updateTimelineButtonTo(PAUSE);
        }
    }

    @PreDestroy
    public void destroy() {
        dataRetrieverTimeline.stop();
        cleanupTiles();
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

    private void initTimeline() {
        createPeriodicTaskToSetRuntimeInfo(REFRESH_DELAY);
        dataRetrieverTimeline.play();
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
        timelineButton.setOnMouseClicked(_ -> {
            if (buttonType == PLAY) {
                playTimelineAnimation();
            } else {
                pauseTimelineAnimation();
            }
        });
    }

    private void playTimelineAnimation() {
        isRealtimeUpdateRunning.set(true);
        updateTimelineButtonTo(PAUSE);
        dataRetrieverTimeline.play();
    }

    private void pauseTimelineAnimation() {
        isRealtimeUpdateRunning.set(false);
        updateTimelineButtonTo(PLAY);
        dataRetrieverTimeline.pause();
    }

    private void createPeriodicTaskToSetRuntimeInfo(final double refreshDelayInSeconds) {
        this.refreshDelayInSeconds = refreshDelayInSeconds;
        if (dataRetrieverTimeline != null) {
            dataRetrieverTimeline.stop();
        }
        dataRetrieverTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshDelayInSeconds),
                                                          _ -> retrieveRuntimeData()));
        dataRetrieverTimeline.setCycleCount(CYCLE_COUNT);
        dataRetrieverTimeline.setOnFinished(_ -> {
            updateTimelineButtonTo(PLAY);
            isRealtimeUpdateRunning.set(false);
        });
    }

    private void retrieveRuntimeData() {
        // @formatter:off
        final var runtimeInfo = retrieveRuntimeInfo();

        noOfServicesTile.setValue(runtimeInfo.noOfServices());
        noOfConfigurationsTile.setValue(runtimeInfo.noOfConfigurations());
        
        final var leaks = runtimeInfo.noOfLeaks();
        noOfLeaksTile.setValue(leaks);

        // Update Deadlocked Threads Tile
        final var deadlockedThreads = runtimeInfo.noOfDeadlockedThreads();
        deadlockedThreadsTile.setValue(deadlockedThreads);

        // Update Log Error Tile
        final var logErrors = runtimeInfo.noOfLogErrors();
        logErrorTile.setValue(logErrors);

        if (isConnected) {
            noOfLeaksTile.setBackgroundColor(leaks > 0 ? MUTED_RED : MUTED_GREEN);
            deadlockedThreadsTile.setBackgroundColor(deadlockedThreads > 0 ? MUTED_RED : MUTED_GREEN);
            logErrorTile.setBackgroundColor(logErrors > 0 ? MUTED_RED : MUTED_GREEN);
        }

        // Update Bundle State Tile
        final List<ChartData> bundleStateData = runtimeInfo.bundleStates().entrySet().stream()
                .map(entry -> new ChartData(entry.getKey(), entry.getValue(), getBundleStateColor(entry.getKey())))
                .toList();
        bundleStateTile.setChartData(bundleStateData);

        // Update Thread State Tile
        final List<ChartData> threadStateData = runtimeInfo.threadStates().entrySet().stream()
                .map(entry -> new ChartData(entry.getKey(), entry.getValue(), getThreadStateColor(entry.getKey())))
                .toList();
        threadStateTile.setChartData(threadStateData);

        // Update Component State Tile
        final List<ChartData> componentStateData = runtimeInfo.componentStates().entrySet().stream()
                .map(entry -> new ChartData(entry.getKey(), entry.getValue(), getComponentStateColor(entry.getKey())))
                .toList();
        componentStateTile.setChartData(componentStateData);

        final var memoryInfo = runtimeInfo.memoryInfo();

        memoryInfo.thenAccept(info -> {
            final var freeMemoryInBytes  = info.freeMemory;
            final var totalMemoryInBytes = info.totalMemory;
            final var usedMemoryInMB     = toMB(totalMemoryInBytes - freeMemoryInBytes);

            threadSync.asyncExec(() -> { 

                // Update Memory History Tile
                final var timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                memoryDataSeries.getData().add(new XYChart.Data<>(timestamp, usedMemoryInMB));
                
                // Keep only last 20 data points
                if (memoryDataSeries.getData().size() > 20) {
                    memoryDataSeries.getData().remove(0);
                }
                
                var memoryConsumptionInfoInPercentage = 0D;
                if (totalMemoryInBytes != 0) {
                    memoryConsumptionInfoInPercentage = (totalMemoryInBytes - freeMemoryInBytes) * 100D
                            / totalMemoryInBytes;
                }

                memoryConsumptionTile.setValue(memoryConsumptionInfoInPercentage);

                final var uptime = toUptimeEntry(info.uptime);
                uptimeTile.setDuration(LocalTime.of(uptime.hours, uptime.minutes(), uptime.seconds()));
            });
        });

        final var osDisplay = runtimeInfo.osName().isEmpty() ? ""
                : runtimeInfo.osName() + " " + runtimeInfo.osVersion();
        runtimeInfoTile.setGraphic(
                createRuntimeTable(
                        runtimeInfo.frameworkBsn(),
                        runtimeInfo.frameworkVersion(),
                        runtimeInfo.javaVersion(),
                        osDisplay,
                        runtimeInfo.osArchitecture()));
        // @formatter:on
    }

    private Color getBundleStateColor(String state) {
        return switch (state.toUpperCase()) {
            case "ACTIVE" -> Bright.GREEN;
            case "RESOLVED" -> Bright.ORANGE;
            case "INSTALLED" -> Bright.RED;
            case "STARTING" -> Bright.YELLOW;
            case "STOPPING" -> Color.MAGENTA;
            case "UNINSTALLED" -> Dark.RED;
            default -> Tile.GRAY;
        };
    }

    private Color getThreadStateColor(String state) {
        return switch (state.toUpperCase()) {
            case "RUNNABLE" -> Bright.GREEN;
            case "BLOCKED" -> Bright.RED;
            case "WAITING" -> Bright.ORANGE;
            case "TIMED_WAITING" -> Bright.YELLOW;
            case "TERMINATED" -> Tile.GRAY;
            case "NEW" -> Bright.BLUE;
            default -> Tile.GRAY;
        };
    }

    private Color getComponentStateColor(String state) {
        return switch (state.toUpperCase()) {
            case "ACTIVE" -> Bright.GREEN;
            case "UNSATISFIED_REFERENCE" -> Bright.RED;
            case "UNSATISFIED_CONFIGURATION" -> Bright.ORANGE;
            case "SATISFIED" -> Bright.GREEN;
            default -> Tile.GRAY;
        };
    }

    private void updateStaticOverviewInfo() {
        if (!isConnected) {
            cachedStaticOverviewInfo = new StaticOverviewInfo("", "", "", "", "", "", "", 0);
            return;
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

        final var noOfServices         = dataProvider.services().size();
        // @formatter:on

        cachedStaticOverviewInfo = new StaticOverviewInfo(frameworkBsn, frameworkVersion, frameworkStartLevel, osName,
                                                          osVersion, osArchitecture, javaVersion, noOfServices);
    }

    private OverviewInfo retrieveRuntimeInfo() {
        if (!isConnected) {
            return new OverviewInfo();
        }
        if (cachedStaticOverviewInfo == null) {
            updateStaticOverviewInfo();
        }
        final var memoryInfo = requireNonNullElse(dataProvider.memory(), completedFuture(new XMemoryInfoDTO()));

        // Calculate Bundle States
        final Map<String, Long> bundleStates = dataProvider.bundles().stream()
                .collect(Collectors.groupingBy(b -> b.state, Collectors.counting()));

        // Calculate Thread States
        final Map<String, Long> threadStates = dataProvider.threads().stream()
                .collect(Collectors.groupingBy(t -> t.state, Collectors.counting()));

        // Calculate Component States
        final Map<String, Long> componentStates = dataProvider.components().stream()
                .collect(Collectors.groupingBy(c -> c.state, Collectors.counting()));

        // Count Log Errors
        final long logErrors = dataProvider.logs().stream().filter(l -> "ERROR".equalsIgnoreCase(l.level)).count();

        // Count Leaks
        final var noOfLeaks = dataProvider.leaks().size();

        // Count Configurations
        final var noOfConfigurations = dataProvider.configurations().size();

        return new OverviewInfo(cachedStaticOverviewInfo.frameworkBsn, cachedStaticOverviewInfo.frameworkVersion,
                                cachedStaticOverviewInfo.frameworkStartLevel, cachedStaticOverviewInfo.osName,
                                cachedStaticOverviewInfo.osVersion, cachedStaticOverviewInfo.osArchitecture,
                                cachedStaticOverviewInfo.javaVersion, cachedStaticOverviewInfo.noOfServices, memoryInfo,
                                bundleStates, threadStates, componentStates, (int) logErrors, noOfConfigurations,
                                noOfLeaks, (int) dataProvider.threads().stream().filter(t -> t.isDeadlocked).count());
    }

    private void createUIComponents(final BorderPane parent) {
        createTiles(parent);
        initStatusBar(parent);
    }

    private void createTiles(final BorderPane parent) {
        // @formatter:off
        runtimeInfoTile = TileBuilder.create()
                                     .skinType(SkinType.CUSTOM)
                                     .title("Runtime Information")
                                     .text("")
                                     .roundedCorners(false)
                                     .build();

        noOfServicesTile = TileBuilder.create()
                                      .skinType(SkinType.NUMBER)
                                      .numberFormat(new DecimalFormat("#"))
                                      .title("Services")
                                      .text("Number of registered services")
                                      .textVisible(true)
                                      .roundedCorners(false)
                                      .decimals(0)
                                      .build();
                                      
        noOfConfigurationsTile = TileBuilder.create()
                                      .skinType(SkinType.NUMBER)
                                      .numberFormat(new DecimalFormat("#"))
                                      .title("Configurations")
                                      .text("Number of configurations")
                                      .textVisible(true)
                                      .roundedCorners(false)
                                      .decimals(0)
                                      .build();

         noOfLeaksTile = TileBuilder.create()
                                      .skinType(SkinType.NUMBER)
                                      .numberFormat(new DecimalFormat("#"))
                                      .title("Classloader Leaks")
                                      .text("Number of classloader leaks")
                                      .textVisible(true)
                                      .roundedCorners(false)
                                      .decimals(0)
                                      .build();

         deadlockedThreadsTile = TileBuilder.create()
                                      .skinType(SkinType.NUMBER)
                                      .numberFormat(new DecimalFormat("#"))
                                      .title("Deadlocked Threads")
                                      .text("Number of deadlocked threads")
                                      .textVisible(true)
                                      .roundedCorners(false)
                                      .decimals(0)
                                      .build();

        memoryConsumptionTile = TileBuilder.create()
                                           .skinType(SkinType.PERCENTAGE)
                                           .title("JVM Memory Consumption Percentage")
                                           .roundedCorners(false)
                                           .build();

        uptimeTile = TileBuilder.create()
                                .skinType(SkinType.TIME)
                                .title("Uptime")
                                .text("Uptime of the remote runtime")
                                .textVisible(true)
                                .roundedCorners(false)
                                .duration(LocalTime.of(0, 0, 0))
                                .build();
        
        // Bundle State Tile
        bundleStateTile = TileBuilder.create()
                                     .skinType(SkinType.DONUT_CHART)
                                     .title("Bundle States")
                                     .roundedCorners(false)
                                     .build();
        
        // Thread State Tile
        threadStateTile = TileBuilder.create()
                                     .skinType(SkinType.DONUT_CHART)
                                     .title("Thread States")
                                     .roundedCorners(false)
                                     .build();
        
        // Memory History Tile
        final var xAxis = new CategoryAxis();
        final var yAxis = new NumberAxis();
        yAxis.setLabel("Memory (MB)");
        
        final var areaChart = new AreaChart<>(xAxis, yAxis);
        areaChart.setLegendVisible(false);
        areaChart.setCreateSymbols(false);
        
        memoryDataSeries = new XYChart.Series<>();
        memoryDataSeries.setName("Heap Usage");
        areaChart.getData().add(memoryDataSeries);

        memoryHistoryTile = TileBuilder.create()
                                       .skinType(SkinType.CUSTOM)
                                       .title("Heap Memory Usage (MB)")
                                       .text("Tracked over time")
                                       .graphic(areaChart)
                                       .roundedCorners(false)
                                       .build();
        
        // Log Error Tile
        logErrorTile = TileBuilder.create()
                                  .skinType(SkinType.NUMBER)
                                  .numberFormat(new DecimalFormat("#"))
                                  .title("Error Logs")
                                  .text("Count of ERROR logs")
                                  .textVisible(true)
                                  .decimals(0)
                                  .decimals(0)
                                  .roundedCorners(false)
                                  .build();

        // Component State Tile
        componentStateTile = TileBuilder.create()
                                     .skinType(SkinType.DONUT_CHART)
                                     .title("Component States")
                                     .roundedCorners(false)
                                     .build();

        final var pane = new GridPane();
        pane.add(runtimeInfoTile, 0, 0);
        pane.add(uptimeTile, 1, 0);
        pane.add(memoryConsumptionTile, 2, 0);
        pane.add(memoryHistoryTile, 3, 0);

        pane.add(bundleStateTile, 0, 1);
        pane.add(componentStateTile, 1, 1);
        pane.add(threadStateTile, 2, 1);
        pane.add(noOfServicesTile, 3, 1);

        pane.add(deadlockedThreadsTile, 0, 2);
        pane.add(noOfLeaksTile, 1, 2);
        pane.add(logErrorTile, 2, 2);
        pane.add(noOfConfigurationsTile, 3, 2);

        final var columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(25);

        final var rowConstraints = new RowConstraints();
        rowConstraints.setPercentHeight(33);

        pane.getColumnConstraints().addAll(columnConstraints, columnConstraints, columnConstraints, columnConstraints);
        pane.getRowConstraints().addAll(rowConstraints, rowConstraints, rowConstraints);

        pane.setHgap(5);
        pane.setVgap(5);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(5));
        pane.setBackground(new Background(new BackgroundFill(Color.web("#F1F1F1"), CornerRadii.EMPTY, Insets.EMPTY)));

        parent.setCenter(pane);
    }

    private Node createRuntimeTable(final String frameworkBsn,
                                    final String frameworkVersion,
                                    final String javaVersion,
                                    final String os,
                                    final String osArchitecture) {
        final var container = new VBox();
        container.setSpacing(12);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(10, 15, 10, 15));

        final String[][] entries = {
                { "CUBES",     "Framework",      frameworkBsn },
                { "TAG",       "Version",        frameworkVersion },
                { "COFFEE",    "Java",           javaVersion },
                { "DESKTOP",   "OS",             os },
                { "SERVER",    "Architecture",   osArchitecture }
        };

        for (final var entry : entries) {
            container.getChildren().add(createInfoRow(entry[0], entry[1], entry[2]));
        }

        return container;
    }

    private Node createInfoRow(final String iconName, final String label, final String value) {
        final var icon = new Glyph("FontAwesome", iconName);
        icon.setTextFill(Color.web("#81D4FA"));
        icon.setFontSize(14);
        icon.setPrefWidth(20);
        icon.setAlignment(Pos.CENTER);

        final var keyLabel = new Label(label);
        keyLabel.setTextFill(Tile.FOREGROUND);
        keyLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        final var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        final var valLabel = new Label(value);
        valLabel.setTextFill(Color.web("#E0F7FA"));
        valLabel.setFont(Font.font("Monospace", 13));
        valLabel.setPadding(new Insets(2, 6, 2, 6));
        valLabel.setBackground(new Background(new BackgroundFill(Color.web("#263238"), new CornerRadii(4), Insets.EMPTY)));

        final var row = new HBox(10, icon, keyLabel, spacer, valLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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

    private volatile StaticOverviewInfo cachedStaticOverviewInfo;

    private record StaticOverviewInfo(String frameworkBsn,
                                      String frameworkVersion,
                                      String frameworkStartLevel,
                                      String osName,
                                      String osVersion,
                                      String osArchitecture,
                                      String javaVersion,
                                      int noOfServices) {
    }

    private record OverviewInfo(String frameworkBsn,
                                String frameworkVersion,
                                String frameworkStartLevel,
                                String osName,
                                String osVersion,
                                String osArchitecture,
                                String javaVersion,
                                int noOfServices,
                                CompletableFuture<XMemoryInfoDTO> memoryInfo,
                                Map<String, Long> bundleStates,
                                Map<String, Long> threadStates,
                                Map<String, Long> componentStates,
                                int noOfLogErrors,
                                int noOfConfigurations,
                                int noOfLeaks,
                                int noOfDeadlockedThreads) {
        public OverviewInfo() {
            this("", "", "", "", "", "", "", 0, completedFuture(new XMemoryInfoDTO()), Map.of(), Map.of(), Map.of(), 0,
                 0, 0, 0);
        }
    }

    @Inject
    @Optional
    private void updateOnAgentConnectedEvent(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data,
                                             final BorderPane parent) {
        logger.atInfo().log("Agent connected event received");
        playTimelineAnimation();
        parent.setBottom(null);
        statusBar.addTo(parent);
    }

    @Inject
    @Optional
    private void updateOnAgentDisconnectedEvent(@UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data,
                                                final BorderPane parent) {
        logger.atInfo().log("Agent disconnected event received");
        dataRetrieverTimeline.stop();

        updateStaticOverviewInfo();
        retrieveRuntimeInfo();
        createUIComponents(parent);
        createPeriodicTaskToSetRuntimeInfo(REFRESH_DELAY);
    }

    @Inject
    @Optional
    private void updateOnDataRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_ANY_TOPIC) final String data,
                                            final BorderPane parent) {
        logger.atDebug().log("Data retrieved event received");
        updateStaticOverviewInfo();
        initStatusBar(parent);
        retrieveRuntimeData();
    }

    private void initStatusBar(final BorderPane parent) {
        statusBar.clearAllInRight();
        statusBar.addTo(parent);
        if (isConnected && !isSnapshotAgent) {
            final var refreshDelayDialog = Fx.initStatusBarButton(this::showViewRefreshDelayDialog,
                    "View Refresh Delay", "GEAR");
            statusBar.addToRight(timelineButton);
            statusBar.addToRight(new Separator(VERTICAL));
            statusBar.addToRight(refreshDelayDialog);
            statusBar.addToRight(new Separator(VERTICAL));
            final var refreshButton = Fx.initStatusBarButton(this::refreshData, "Refresh", "REFRESH");
            statusBar.addToRight(refreshButton);
        }
    }

    private void refreshData() {
        dataProvider.retrieveInfo("bundles", true);
        dataProvider.retrieveInfo("services", true);
        dataProvider.retrieveInfo("components", true);
        dataProvider.retrieveInfo("threads", true);
        dataProvider.retrieveInfo("properties", true);
        dataProvider.retrieveInfo("logs", true);
        dataProvider.retrieveInfo("memory", true);
        dataProvider.retrieveInfo("configurations", true);
        dataProvider.retrieveInfo("packages", true);
        dataProvider.retrieveInfo("leaks", true);
    }

    /**
     * Stops internal timer/animation threads of existing tiles before they are
     * replaced by new instances. This prevents thread leaks from TilesFX tiles
     * that spawn background threads for clock updates and gauge animations.
     */
    private void cleanupTiles() {
        if (memoryHistoryTile != null) {
            memoryHistoryTile.setAnimated(false);
        }
    }

    private void showViewRefreshDelayDialog() {
        final var dialog = new ViewRefreshDelayDialog();
        ContextInjectionFactory.inject(dialog, eclipseContext);
        dialog.init(Math.round(refreshDelayInSeconds));
        final var refreshDelayInput = dialog.showAndWait();
        if (refreshDelayInput.isPresent()) {
            dataRetrieverTimeline.stop();
            createPeriodicTaskToSetRuntimeInfo(refreshDelayInput.get());
            if (isRealtimeUpdateRunning.get()) {
                dataRetrieverTimeline.play();
            }
            Fx.showSuccessNotification("Refresh Delay", "View refresh delay has been updated successfully");
        }
    }

}
