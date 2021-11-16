package in.bytehue.osgifx.console.ui.graph;

import static javafx.scene.control.SelectionMode.MULTIPLE;
import static org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Collection;
import java.util.Comparator;
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
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;
import org.osgi.annotation.bundle.Requirement;

import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import in.bytehue.osgifx.console.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import in.bytehue.osgifx.console.smartgraph.graphview.SmartGraphPanel;
import in.bytehue.osgifx.console.smartgraph.graphview.SmartPlacementStrategy;
import in.bytehue.osgifx.console.smartgraph.graphview.SmartRandomPlacementStrategy;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=in.bytehue.osgifx.console.data.provider.DataProvider)")
public final class GraphFxComponentController {

    @Log
    @Inject
    private FluentLogger                 logger;
    @FXML
    private SegmentedButton              strategyButton;
    @FXML
    private ToggleButton                 circularStrategyButton;
    @FXML
    private ToggleButton                 randomStrategyButton;
    @FXML
    private CheckListView<XComponentDTO> componentsList;
    @FXML
    private ChoiceBox<String>            wiringSelection;
    @FXML
    private BorderPane                   graphPane;
    @Inject
    private DataProvider                 dataProvider;
    private RuntimeComponentGraph        runtimeGraph;
    private MaskerPane                   progressPane;
    private ExecutorService              executor;
    private Future<?>                    graphGenFuture;

    @FXML
    public void initialize() {
        initComponentsList();
        executor     = Executors.newSingleThreadExecutor(r -> new Thread(r, "graph-gen"));
        progressPane = new MaskerPane();
        logger.atDebug().log("FXML controller has been initialized");
        strategyButton.getStyleClass().add(STYLE_CLASS_DARK);
        wiringSelection.getItems().addAll("find all components that are required by", "find all SCR cycles");
        wiringSelection.getSelectionModel().select(0);
        wiringSelection.getSelectionModel().selectedIndexProperty()
                .addListener((ChangeListener<Number>) (observable, oldValue, newValue) -> {
                    componentsList.getCheckModel().clearChecks();
                    componentsList.setDisable(newValue.intValue() == 1);
                });
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    private void initComponentsList() {
        componentsList.getSelectionModel().setSelectionMode(MULTIPLE);
        componentsList.setCellFactory(param -> new CheckBoxListCell<XComponentDTO>(componentsList::getItemBooleanProperty) {
            @Override
            public void updateItem(final XComponentDTO component, final boolean empty) {
                super.updateItem(component, empty);
                if (empty || component == null) {
                    setText(null);
                } else {
                    setText(component.name);
                }
            }
        });
        final ObservableList<XComponentDTO> components = dataProvider.components();
        runtimeGraph = new RuntimeComponentGraph(components);
        componentsList.setItems(components.sorted(Comparator.comparing(b -> b.name)));
    }

    @FXML
    private void generateGraph(final ActionEvent event) {
        final int                           selection       = wiringSelection.getSelectionModel().getSelectedIndex();
        final ObservableList<XComponentDTO> selectedBundles = componentsList.getCheckModel().getCheckedItems();
        if (selectedBundles.isEmpty() && selection == 0) {
            return;
        }
        final Task<?> task = new Task<Void>() {

            FxComponentGraph fxGraph;

            @Override
            protected Void call() throws Exception {
                progressPane.setVisible(true);

                if (selection == 0) {
                    final Collection<GraphPath<ComponentVertex, DefaultEdge>> dependencies = runtimeGraph
                            .getAllServiceComponentsThatAreRequiredBy(selectedBundles);
                    fxGraph = new FxComponentGraph(dependencies);
                } else {
                    final Graph<ComponentVertex, DefaultEdge> graph = runtimeGraph.getAllCycles();
                    fxGraph = new FxComponentGraph(graph);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                final SmartGraphPanel<ComponentVertex, String> graphView = new SmartGraphPanel<>(fxGraph.graph, getStrategy());
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
        componentsList.getCheckModel().clearChecks();
    }

    private SmartPlacementStrategy getStrategy() {
        if (randomStrategyButton.isSelected()) {
            return new SmartRandomPlacementStrategy();
        }
        return new SmartCircularSortedPlacementStrategy();
    }

}