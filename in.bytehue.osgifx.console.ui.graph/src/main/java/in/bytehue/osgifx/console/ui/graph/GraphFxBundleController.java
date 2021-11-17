package in.bytehue.osgifx.console.ui.graph;

import static javafx.scene.control.SelectionMode.MULTIPLE;
import static org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.controlsfx.control.CheckListView;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.SegmentedButton;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;
import org.osgi.annotation.bundle.Requirement;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import in.bytehue.osgifx.console.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import in.bytehue.osgifx.console.smartgraph.graphview.SmartGraphPanel;
import in.bytehue.osgifx.console.smartgraph.graphview.SmartPlacementStrategy;
import in.bytehue.osgifx.console.smartgraph.graphview.SmartRandomPlacementStrategy;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=in.bytehue.osgifx.console.data.provider.DataProvider)")
public final class GraphFxBundleController {

    @Log
    @Inject
    private FluentLogger              logger;
    @FXML
    private SegmentedButton           strategyButton;
    @FXML
    private TextField                 searchText;
    @FXML
    private ToggleButton              circularStrategyButton;
    @FXML
    private ToggleButton              randomStrategyButton;
    @FXML
    private CheckListView<XBundleDTO> bundlesList;
    @FXML
    private ChoiceBox<String>         wiringSelection;
    @FXML
    private BorderPane                graphPane;
    @Inject
    private DataProvider              dataProvider;
    private RuntimeBundleGraph        runtimeGraph;
    private MaskerPane                progressPane;
    private ExecutorService           executor;
    private Future<?>                 graphGenFuture;

    @FXML
    public void initialize() {
        initBundlesList();
        executor     = Executors.newSingleThreadExecutor(r -> new Thread(r, "graph-gen"));
        progressPane = new MaskerPane();
        logger.atDebug().log("FXML controller has been initialized");
        strategyButton.getStyleClass().add(STYLE_CLASS_DARK);
        wiringSelection.getItems().addAll("Find all bundles that are required by", "Find all bundles that require");
        wiringSelection.getSelectionModel().select(0);
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    private void initBundlesList() {
        bundlesList.getSelectionModel().setSelectionMode(MULTIPLE);
        bundlesList.setCellFactory(param -> new CheckBoxListCell<XBundleDTO>(bundlesList::getItemBooleanProperty) {
            @Override
            public void updateItem(final XBundleDTO bundle, final boolean empty) {
                super.updateItem(bundle, empty);
                if (empty || bundle == null) {
                    setText(null);
                } else {
                    setText(bundle.symbolicName);
                }
            }
        });
        final ObservableList<XBundleDTO> bundles             = dataProvider.bundles();
        final FilteredList<XBundleDTO>   filteredBundlesList = initSearchFilter(bundles);
        runtimeGraph = new RuntimeBundleGraph(bundles);
        bundlesList.setItems(filteredBundlesList.sorted(Comparator.comparing(b -> b.symbolicName)));
        logger.atInfo().log("Bundles list has been initialized");
    }

    private FilteredList<XBundleDTO> initSearchFilter(final ObservableList<XBundleDTO> bundles) {
        final FilteredList<XBundleDTO> filteredBundlesList = new FilteredList<>(bundles, s -> true);
        searchText.textProperty().addListener(obs -> {
            final String filter = searchText.getText();
            if (filter == null || filter.length() == 0) {
                filteredBundlesList.setPredicate(s -> true);
            } else {
                filteredBundlesList.setPredicate(s -> Stream.of(filter.split("\\|")).anyMatch(s.symbolicName::contains));
            }
        });
        return filteredBundlesList;
    }

    @FXML
    private void generateGraph(final ActionEvent event) {
        logger.atInfo().log("Generating graph for bundles");
        final ObservableList<XBundleDTO> selectedBundles = bundlesList.getCheckModel().getCheckedItems();
        if (selectedBundles.isEmpty()) {
            logger.atInfo().log("No bundles has been selected. Skipped graph generation.");
            return;
        }
        final Task<?> task = new Task<Void>() {

            FxBundleGraph fxGraph;

            @Override
            protected Void call() throws Exception {
                progressPane.setVisible(true);
                final int selection = wiringSelection.getSelectionModel().getSelectedIndex();

                final Collection<GraphPath<BundleVertex, DefaultEdge>> dependencies;
                if (selection == 0) {
                    logger.atInfo().log("Generating all graph paths for bundles that are required by '%s'", selectedBundles);
                    dependencies = runtimeGraph.getAllBundlesThatAreRequiredBy(selectedBundles);
                } else {
                    logger.atInfo().log("Generating all graph paths for bundles that require '%s'", selectedBundles);
                    dependencies = runtimeGraph.getAllBundlesThatRequire(selectedBundles);
                }
                fxGraph = new FxBundleGraph(dependencies);
                return null;
            }

            @Override
            protected void succeeded() {
                final SmartGraphPanel<BundleVertex, String> graphView = new SmartGraphPanel<>(fxGraph.graph, getStrategy());
                graphView.setPrefSize(800, 520);
                progressPane.setVisible(false);
                graphPane.setCenter(graphView);
                graphView.init();
            }
        };
        graphPane.setCenter(progressPane);
        if (graphGenFuture != null) {
            graphGenFuture.cancel(true);
        }
        graphGenFuture = executor.submit(task);
    }

    @FXML
    private void deselectAll(final ActionEvent event) {
        bundlesList.getCheckModel().clearChecks();
    }

    private SmartPlacementStrategy getStrategy() {
        if (randomStrategyButton.isSelected()) {
            return new SmartRandomPlacementStrategy();
        }
        return new SmartCircularSortedPlacementStrategy();
    }

}