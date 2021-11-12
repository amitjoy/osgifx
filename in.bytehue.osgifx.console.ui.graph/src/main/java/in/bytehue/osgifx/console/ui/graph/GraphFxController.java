package in.bytehue.osgifx.console.ui.graph;

import static javafx.scene.control.SelectionMode.MULTIPLE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.controlsfx.control.CheckListView;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;
import org.osgi.annotation.bundle.Requirement;

import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import com.brunomnsilva.smartgraph.graphview.SmartPlacementStrategy;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=in.bytehue.osgifx.console.data.provider.DataProvider)")
public final class GraphFxController {

    @Log
    @Inject
    private FluentLogger              logger;
    @FXML
    private Button                    applyButton;
    @FXML
    private CheckListView<XBundleDTO> bundlesList;
    @FXML
    private BorderPane                graphPane;
    @Inject
    private DataProvider              dataProvider;
    private RuntimeGraph              runtimeGraph;

    @FXML
    public void initialize() {
        initBundlesList();

        logger.atDebug().log("FXML controller has been initialized");
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
                    setText(formatId(bundle));
                }
            }

            private String formatId(final XBundleDTO bundle) {
                return bundle.symbolicName;
            }
        });
        final ObservableList<XBundleDTO> bundles = dataProvider.bundles();
        runtimeGraph = new RuntimeGraph(bundles);
        bundlesList.setItems(bundles.sorted(Comparator.comparing(b -> b.symbolicName)));
    }

    @FXML
    private void generateGraph(final ActionEvent event) {
        final ObservableList<XBundleDTO>                 selectedBundles = bundlesList.getCheckModel().getCheckedItems();
        final List<GraphPath<BundleVertex, DefaultEdge>> dependencies    = runtimeGraph.getTransitiveDependenciesOf(selectedBundles);
        final FxGraph                                    fxGraph         = new FxGraph(dependencies);

        final SmartPlacementStrategy                strategy  = new SmartCircularSortedPlacementStrategy();
        final SmartGraphPanel<BundleVertex, String> graphView = new SmartGraphPanel<>(fxGraph.graph, strategy);
        graphView.setAutomaticLayout(true);
        graphPane.setCenter(graphView);

        graphView.init();
    }

}