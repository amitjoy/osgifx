package in.bytehue.osgifx.console.ui.graph;

import static javafx.scene.control.SelectionMode.MULTIPLE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Comparator;

import javax.inject.Inject;

import org.controlsfx.control.CheckListView;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;

import com.brunomnsilva.smartgraph.graph.Graph;
import com.brunomnsilva.smartgraph.graph.GraphEdgeList;
import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import com.brunomnsilva.smartgraph.graphview.SmartPlacementStrategy;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, attribute = "(objectClass=in.bytehue.osgifx.console.data.provider.DataProvider)")
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

    @FXML
    public void initialize() {
        initBundlesList();
        createControls();

        final SmartPlacementStrategy          strategy  = new SmartCircularSortedPlacementStrategy();
        final SmartGraphPanel<String, String> graphView = new SmartGraphPanel<>(createDummyGraph(), strategy);
        graphView.setAutomaticLayout(true);
        graphPane.setCenter(graphView);

        graphView.init();

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
        bundlesList.setItems(dataProvider.bundles().sorted(Comparator.comparing(b -> b.symbolicName)));
    }

    private void createControls() {
        // TODO Auto-generated method stub
    }

    private Graph<String, String> createDummyGraph() {
        final Graph<String, String> g = new GraphEdgeList<>();

        g.insertVertex("A");
        g.insertVertex("B");
        g.insertVertex("C");
        g.insertVertex("D");
        g.insertVertex("E");
        g.insertVertex("F");
        g.insertVertex("G");

        g.insertEdge("A", "B", "1");
        g.insertEdge("A", "C", "2");
        g.insertEdge("A", "D", "3");
        g.insertEdge("A", "E", "4");
        g.insertEdge("A", "F", "5");
        g.insertEdge("A", "G", "6");

        g.insertVertex("H0");
        g.insertVertex("I");
        g.insertVertex("J");
        g.insertVertex("K");
        g.insertVertex("L");
        g.insertVertex("M");
        g.insertVertex("N");

        g.insertEdge("H0", "I", "7");
        g.insertEdge("H0", "J", "8");
        g.insertEdge("H0", "K", "9");
        g.insertEdge("H0", "L", "10");
        g.insertEdge("H0", "M", "11");
        g.insertEdge("H0", "N", "12");

        g.insertEdge("A", "H0", "0");
        return g;
    }
}