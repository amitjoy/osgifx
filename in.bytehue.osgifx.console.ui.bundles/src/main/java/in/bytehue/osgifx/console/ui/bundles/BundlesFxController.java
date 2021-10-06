package in.bytehue.osgifx.console.ui.bundles;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

public final class BundlesFxController implements Initializable {

    @Inject
    @LocalInstance
    private FXMLLoader loader;

    @FXML
    private TableView<XBundleDTO> table;

    @Inject
    private DataProvider dataProvider;

    @Inject
    @Named("in.bytehue.osgifx.console.ui.bundles")
    private BundleContext context;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        createControls();
    }

    private void createControls() {
        final GridPane                           expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final BundleDetailsFxController          controller     = loader.getController();
        final TableRowExpanderColumn<XBundleDTO> expanderColumn = new TableRowExpanderColumn<>(param -> {
                                                                    controller.setValue(param.getValue());
                                                                    return expandedNode;
                                                                });

        final TableColumn<XBundleDTO, String> symbolicNameColumn = new TableColumn<>("Symbolic Name");

        symbolicNameColumn.setPrefWidth(450);
        symbolicNameColumn.setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));

        final TableColumn<XBundleDTO, String> versionColumn = new TableColumn<>("Version");

        versionColumn.setPrefWidth(450);
        versionColumn.setCellValueFactory(new DTOCellValueFactory<>("version", String.class));

        final TableColumn<XBundleDTO, String> statusColumn = new TableColumn<>("State");

        statusColumn.setPrefWidth(200);
        statusColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(symbolicNameColumn);
        table.getColumns().add(versionColumn);
        table.getColumns().add(statusColumn);

        final ObservableList<XBundleDTO> bundles = dataProvider.bundles();
        table.setItems(bundles);

        TableFilter.forTableView(table).apply();
    }

}
