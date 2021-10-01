package in.bytehue.osgifx.console.ui.overview;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.fx.core.di.LocalInstance;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.colors.Bright;
import eu.hansolo.tilesfx.colors.Dark;
import eu.hansolo.tilesfx.skins.LeaderBoardItem;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

public final class OverviewFxUI {

    private static final double TILE_WIDTH  = 400;
    private static final double TILE_HEIGHT = 200;

    @PostConstruct
    public void postConstruct(final VBox parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent);
    }

    private List<LeaderBoardItem> getRuntimeInfo() {
        return Collections.emptyList();
    }

    private List<LeaderBoardItem> getAgentInfo() {
        return Collections.emptyList();
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

    @SuppressWarnings("unchecked")
    private void createWidgets(final VBox parent) {
        // @formatter:off
        final Tile clockTile = TileBuilder.create()
                                          .skinType(SkinType.CLOCK)
                                          .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                          .title("Today")
                                          .text("Whatever text")
                                          .dateVisible(true)
                                          .locale(Locale.US)
                                          .running(true)
                                          .build();
        clockTile.setRoundedCorners(false);

        final Tile agentInfoTile = TileBuilder.create()
                                                .skinType(SkinType.LEADER_BOARD)
                                                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                .title("Agent")
                                                .text("Console agent information")
                                                .leaderBoardItems(getAgentInfo())
                                                .build();
        agentInfoTile.setRoundedCorners(false);

        final Tile runtimeInfoTile = TileBuilder.create()
                                                  .skinType(SkinType.LEADER_BOARD)
                                                  .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                  .title("Runtime")
                                                  .text("Runtime OSGi framework information")
                                                  .leaderBoardItems(getRuntimeInfo())
                                                  .build();
        runtimeInfoTile.setRoundedCorners(false);

        final Tile noOfBundlesTile = TileBuilder.create()
                                                .skinType(SkinType.NUMBER)
                                                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                .title("Bundles")
                                                .text("Number of installed bundles")
                                                .value(13)
                                                .unit("mb")
                                                .description("Test")
                                                .textVisible(true)
                                                .build();
        noOfBundlesTile.setRoundedCorners(false);

        final Tile noOfServicesTile = TileBuilder.create()
                                                 .skinType(SkinType.NUMBER)
                                                 .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                 .title("Services")
                                                 .text("Number of registered services")
                                                 .value(13)
                                                 .unit("mb")
                                                 .description("Test")
                                                 .textVisible(true)
                                                 .build();
        noOfServicesTile.setRoundedCorners(false);

        final Tile noOfComponentsTile = TileBuilder.create()
                                                   .skinType(SkinType.NUMBER)
                                                   .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                   .title("Components")
                                                   .text("Number of registered components")
                                                   .value(13)
                                                   .unit("mb")
                                                   .description("Test")
                                                   .textVisible(true)
                                                   .build();
        noOfComponentsTile.setRoundedCorners(false);

        final Tile noOfErrorFrameworkEventsTile = TileBuilder.create()
                                                             .skinType(SkinType.NUMBER)
                                                             .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                             .title("Error Framework Events")
                                                             .text("Number of published error framework events")
                                                             .value(13)
                                                             .unit("mb")
                                                             .description("Test")
                                                             .textVisible(true)
                                                             .build();
        noOfErrorFrameworkEventsTile.setRoundedCorners(false);

        final Tile availableMemoryTile = TileBuilder.create()
                                                    .skinType(SkinType.BAR_GAUGE)
                                                    .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                                    .minValue(0)
                                                    .maxValue(100)
                                                    .startFromZero(true)
                                                    .threshold(80)
                                                    .thresholdVisible(true)
                                                    .title("Available Memory")
                                                    .unit("MM")
                                                    .text("Available memory of the remote runtime")
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

        final Tile uptimeTile = TileBuilder.create()
                                           .skinType(SkinType.TIME)
                                           .prefSize(TILE_WIDTH, TILE_HEIGHT)
                                           .title("Uptime")
                                           .text("Uptime of the remote runtime")
                                           .duration(LocalTime.of(1, 22))
                                           .description("Average reply time")
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

}