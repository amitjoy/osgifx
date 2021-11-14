package in.bytehue.osgifx.console.smartgraph.containers;

import in.bytehue.osgifx.console.smartgraph.graphview.SmartGraphPanel;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class SmartGraphDemoContainer extends BorderPane {

    public SmartGraphDemoContainer(@SuppressWarnings("rawtypes") final SmartGraphPanel graphView) {

        setCenter(new ContentZoomPane(graphView));

        // create bottom pane with controls
        final HBox bottom = new HBox(10);

        final CheckBox automatic = new CheckBox("Automatic layout");
        automatic.selectedProperty().bindBidirectional(graphView.automaticLayoutProperty());

        bottom.getChildren().add(automatic);

        setBottom(bottom);
    }

}
