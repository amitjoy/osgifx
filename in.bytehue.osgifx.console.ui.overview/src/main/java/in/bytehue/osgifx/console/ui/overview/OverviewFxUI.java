package in.bytehue.osgifx.console.ui.overview;

import static in.bytehue.osgifx.console.supervisor.ConsoleSupervisor.AGENT_CONNECTED_EVENT_TOPIC;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.colors.Bright;
import eu.hansolo.tilesfx.colors.Dark;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import in.bytehue.osgifx.console.supervisor.ConsoleSupervisor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

public final class OverviewFxUI {

    private static final double TILE_WIDTH  = 400;
    private static final double TILE_HEIGHT = 200;

    @Inject
    private ConsoleSupervisor supervisor;

    @PostConstruct
    public void postConstruct(final VBox parent) {
        createControls(parent);
    }

    @Focus
    void focus(final VBox parent) {
        createControls(parent);
    }

    private void createControls(final VBox parent) {
        // this is required as the CSS styling of tilesfx were getting overridden after switching tabs
        parent.getChildren().clear();
        createWidgets(parent);
    }

    private void createWidgets(final VBox parent) {
        // @formatter:off
        final Tile clockTile = TileBuilder.create()
                                          .skinType(SkinType.CLOCK)
                                          .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                          .title("Today")
                                          .text("Whatever text")
                                          .dateVisible(true)
                                          .locale(Locale.UK)
                                          .running(true)
                                          .build();
        clockTile.setRoundedCorners(false);

        final Tile agentInfoTile = TileBuilder.create()
                                               .skinType(SkinType.CUSTOM)
                                               .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                               .title("Agent")
                                               .text("Console agent information")
                                               .graphic(createAgentTable(supervisor.getHost(), supervisor.getPort(), supervisor.getTimeout()))
                                               .build();
        agentInfoTile.setRoundedCorners(false);

        final Map<String, String> runtimeInfo = java.util.Optional.ofNullable(supervisor.getAgent())
                                                                  .map(agent -> new HashMap<>(agent.runtimeInfo()))
                                                                  .orElse(new HashMap<>());
        final Tile runtimeInfoTile = TileBuilder.create()
                                                .skinType(SkinType.CUSTOM)
                                                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                .title("Runtime Information")
                                                .graphic(createRuntimeTable(runtimeInfo))
                                                .text("")
                                                .build();
        runtimeInfoTile.setRoundedCorners(false);

        final double noOfInstalledBundles = java.util.Optional.ofNullable(supervisor.getAgent())
                                                              .map(agent -> agent.getAllBundles().size())
                                                              .map(Double::valueOf)
                                                              .orElse(0.0d);
        final Tile noOfBundlesTile = TileBuilder.create()
                                                .skinType(SkinType.NUMBER)
                                                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                .title("Bundles")
                                                .text("Number of installed bundles")
                                                .value(noOfInstalledBundles)
                                                .valueVisible(noOfInstalledBundles != 0.0d)
                                                .textVisible(true)
                                                .build();
        noOfBundlesTile.setRoundedCorners(false);

        final double noOfServices = java.util.Optional.ofNullable(supervisor.getAgent())
                                                      .map(agent -> agent.getAllServices().size())
                                                      .map(Double::valueOf)
                                                      .orElse(0.0d);
        final Tile noOfServicesTile = TileBuilder.create()
                                                 .skinType(SkinType.NUMBER)
                                                 .numberFormat(new DecimalFormat("#"))
                                                 .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                 .title("Services")
                                                 .text("Number of registered services")
                                                 .value(noOfServices)
                                                 .valueVisible(noOfServices != 0.0d)
                                                 .textVisible(true)
                                                 .build();
        noOfServicesTile.setRoundedCorners(false);

        final double noOfComponents = java.util.Optional.ofNullable(supervisor.getAgent())
                                                        .map(agent -> agent.getAllComponents().size())
                                                        .map(Double::valueOf)
                                                        .orElse(0.0d);
        final Tile noOfComponentsTile = TileBuilder.create()
                                                   .skinType(SkinType.NUMBER)
                                                   .numberFormat(new DecimalFormat("#"))
                                                   .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                   .title("Components")
                                                   .text("Number of registered components")
                                                   .value(noOfComponents)
                                                   .valueVisible(noOfComponents != 0.0d)
                                                   .textVisible(true)
                                                   .build();
        noOfComponentsTile.setRoundedCorners(false);

        final Tile noOfErrorFrameworkEventsTile = TileBuilder.create()
                                                             .skinType(SkinType.NUMBER)
                                                             .numberFormat(new DecimalFormat("#"))
                                                             .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                             .title("Error Framework Events")
                                                             .text("Number of published error framework events")
                                                             .value(13)
                                                             .unit("mb")
                                                             .description("Test")
                                                             .textVisible(true)
                                                             .build();
        noOfErrorFrameworkEventsTile.setRoundedCorners(false);

        final int freeMemory = getMemory("Memory Free");
        final int totalMemory = getMemory("Memory Total");

        final Tile availableMemoryTile = TileBuilder.create()
                                                    .skinType(SkinType.BAR_GAUGE)
                                                    .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                    .minValue(0)
                                                    .maxValue(totalMemory)
                                                    .startFromZero(true)
                                                    .threshold(totalMemory * .8)
                                                    .thresholdVisible(true)
                                                    .title("Allocated Memory")
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
        availableMemoryTile.setValue(totalMemory - freeMemory);

        final UptimeDTO uptime = java.util.Optional.ofNullable(supervisor.getAgent())
                                                   .map(agent -> new HashMap<>(agent.runtimeInfo()))
                                                   .filter(info -> !info.isEmpty())
                                                   .map(info -> info.get("Uptime"))
                                                   .map(Long::valueOf)
                                                   .map(this::toUptimeEntry)
                                                   .orElse(new UptimeDTO(0, 0, 0, 0));
        final Tile uptimeTile = TileBuilder.create()
                                           .skinType(SkinType.TIME)
                                           .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                           .title("Uptime")
                                           .text("Uptime of the remote runtime")
                                           .duration(LocalTime.of(uptime.hours, uptime.minutes))
                                           .textVisible(true)
                                           .build();
        uptimeTile.setRoundedCorners(false);

        final FlowGridPane pane = new FlowGridPane(3, 3,
                                                   clockTile,
                                                   agentInfoTile,
                                                   runtimeInfoTile,
                                                   noOfBundlesTile,
                                                   noOfServicesTile,
                                                   noOfComponentsTile,
                                                   noOfErrorFrameworkEventsTile,
                                                   availableMemoryTile,
                                                   uptimeTile);
        pane.setHgap(5);
        pane.setVgap(5);
        pane.setAlignment(Pos.CENTER);
        pane.setCenterShape(true);
        pane.setPadding(new Insets(5));
        pane.setBackground(new Background(new BackgroundFill(Color.web("#F1F1F1"), CornerRadii.EMPTY, Insets.EMPTY)));

        parent.getChildren().add(pane);
        // @formatter:on
    }

    @Inject
    @Optional
    public void updateView(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data, final VBox parent) {
        createControls(parent);
    }

    private Node createRuntimeTable(final Map<String, String> info) {
        final Label name = new Label("");
        name.setTextFill(Tile.FOREGROUND);
        name.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(name, Priority.NEVER);

        final Region spacer = new Region();
        spacer.setPrefSize(5, 5);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        final Label views = new Label("");
        views.setTextFill(Tile.FOREGROUND);
        views.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(views, Priority.NEVER);

        final HBox header = new HBox(5, name, spacer, views);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setFillHeight(true);

        final VBox dataTable = new VBox(0, header);
        dataTable.setFillWidth(true);

        final Map<String, String> sorted = new TreeMap<>(info);
        for (final Entry<String, String> entry : sorted.entrySet()) {
            final String key   = entry.getKey();
            final String value = entry.getValue();

            final HBox node = getTileTableInfo(key, value);
            dataTable.getChildren().add(node);
        }
        return dataTable;
    }

    private Node createAgentTable(final String host, final int port, final int timeout) {
        final Label name = new Label("");
        name.setTextFill(Tile.FOREGROUND);
        name.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(name, Priority.NEVER);

        final Region spacer = new Region();
        spacer.setPrefSize(5, 5);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        final Label views = new Label("");
        views.setTextFill(Tile.FOREGROUND);
        views.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(views, Priority.NEVER);

        final HBox header = new HBox(5, name, spacer, views);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setFillHeight(true);

        final VBox dataTable = new VBox(0, header);
        dataTable.setFillWidth(true);

        final HBox hostNode    = getTileTableInfo("Host", host);
        final HBox portNode    = getTileTableInfo("Port", String.valueOf(port));
        final HBox timeoutNode = getTileTableInfo("Timeout", String.valueOf(timeout));

        dataTable.getChildren().addAll(hostNode, portNode, timeoutNode);
        return dataTable;
    }

    private HBox getTileTableInfo(final String property, final String value) {
        final Label propertyLabel = new Label(property);
        propertyLabel.setTextFill(Tile.FOREGROUND);
        propertyLabel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(propertyLabel, Priority.NEVER);

        final Region spacer = new Region();
        spacer.setPrefSize(5, 5);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        final Label valueLabel = new Label(value);
        valueLabel.setTextFill(Tile.FOREGROUND);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(valueLabel, Priority.NEVER);

        final HBox hBox = new HBox(5, propertyLabel, spacer, valueLabel);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setFillHeight(true);

        return hBox;
    }

    private int toMB(final long sizeInBytes) {
        return (int) (sizeInBytes / 1024 / 1024);
    }

    private UptimeDTO toUptimeEntry(final long uptime) {
        final int days    = (int) TimeUnit.MILLISECONDS.toDays(uptime);
        final int hours   = (int) TimeUnit.MILLISECONDS.toHours(uptime) - days * 24;
        final int minutes = (int) (TimeUnit.MILLISECONDS.toMinutes(uptime) - TimeUnit.MILLISECONDS.toHours(uptime) * 60);
        final int seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(uptime) - TimeUnit.MILLISECONDS.toMinutes(uptime) * 60);

        return new UptimeDTO(days, hours, minutes, seconds);
    }

    @SuppressWarnings("unused")
    private static class UptimeDTO {
        int days;
        int hours;
        int minutes;
        int seconds;

        public UptimeDTO(final int days, final int hours, final int minutes, final int seconds) {
            this.days    = days;
            this.hours   = hours;
            this.minutes = minutes;
            this.seconds = seconds;
        }
    }

    private int getMemory(final String key) {
        // @formatter:off
        return java.util.Optional.ofNullable(supervisor.getAgent())
                                 .map(agent -> new HashMap<>(agent.runtimeInfo()))
                                 .filter(info -> !info.isEmpty())
                                 .map(info -> info.get(key))
                                 .map(Long::valueOf)
                                 .map(this::toMB)
                                 .orElse(0);
        // @formatter:on
    }

}