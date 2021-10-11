package in.bytehue.osgifx.console.ui.events;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import in.bytehue.osgifx.console.util.fx.NullTableViewSelectionModel;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

public final class EventsFxController implements Initializable {

    @Inject
    @LocalInstance
    private FXMLLoader loader;

    @FXML
    private TableView<XEventDTO> table;

    @Inject
    private DataProvider dataProvider;

    @Inject
    @Named("in.bytehue.osgifx.console.ui.events")
    private BundleContext context;

    private TableRowExpanderColumn.TableRowDataFeatures<XEventDTO> selectedEvent;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        table.setSelectionModel(new NullTableViewSelectionModel<>(table));
        createControls();
    }

    private void createControls() {
        final GridPane                          expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final EventDetailsFxController          controller     = loader.getController();
        final TableRowExpanderColumn<XEventDTO> expanderColumn = new TableRowExpanderColumn<>(expandedEvent -> {
                                                                   controller.initControls(expandedEvent.getValue());
                                                                   if (selectedEvent != null && selectedEvent.isExpanded()) {
                                                                       selectedEvent.toggleExpanded();
                                                                   }
                                                                   selectedEvent = expandedEvent;
                                                                   return expandedNode;
                                                               });

        final TableColumn<XEventDTO, Date> receivedAtColumn = new TableColumn<>("Received");

        receivedAtColumn.setPrefWidth(290);
        receivedAtColumn.setCellValueFactory(new DTOCellValueFactory<>("received", Date.class));

        final TableColumn<XEventDTO, String> topicColumn = new TableColumn<>("Topic");

        topicColumn.setPrefWidth(650);
        topicColumn.setCellValueFactory(new DTOCellValueFactory<>("topic", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(receivedAtColumn);
        table.getColumns().add(topicColumn);

        final ObservableList<XEventDTO> bundles = dataProvider.events();
        table.setItems(bundles);

        TableFilter.forTableView(table).apply();
    }

}
