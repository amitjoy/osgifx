package in.bytehue.osgifx.console.ui.graph;

import static javafx.scene.control.SelectionMode.MULTIPLE;
import static org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import com.brunomnsilva.smartgraph.graphview.SmartPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartRandomPlacementStrategy;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=in.bytehue.osgifx.console.data.provider.DataProvider)")
public final class GraphFxController {

    @Log
    @Inject
    private FluentLogger              logger;
    @FXML
    private SegmentedButton           strategyButton;
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
    private RuntimeGraph              runtimeGraph;
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
        wiringSelection.getItems().addAll("find all bundles that are required by", "find all bundles that require");
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
        final ObservableList<XBundleDTO> bundles = dataProvider.bundles();
        runtimeGraph = new RuntimeGraph(bundles);
        bundlesList.setItems(bundles.sorted(Comparator.comparing(b -> b.symbolicName)));
    }

    @FXML
    private void deselectAllBundles(final ActionEvent event) {
        bundlesList.getCheckModel().clearChecks();
    }

    @FXML
    private void generateGraph(final ActionEvent event) {
        final ObservableList<XBundleDTO> selectedBundles = bundlesList.getCheckModel().getCheckedItems();
        if (selectedBundles.isEmpty()) {
            return;
        }
        final Task<?> task = new Task<Void>() {

            FxGraph fxGraph;

            @Override
            protected Void call() throws Exception {
                progressPane.setVisible(true);
                final int selection = wiringSelection.getSelectionModel().getSelectedIndex();

                final List<GraphPath<BundleVertex, DefaultEdge>> dependencies;
                if (selection == 0) {
                    dependencies = runtimeGraph.getAllBundlesThatAreRequiredBy(selectedBundles);
                } else {
                    dependencies = runtimeGraph.getAllBundlesThatRequire(selectedBundles);
                }
                fxGraph = new FxGraph(dependencies);
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