package in.bytehue.osgifx.console.ui.bundles;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.di.Service;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class BundlesFxController implements Initializable {

    @Log
    @Inject
    private Logger logger;

    @Inject
    @LocalInstance
    private FXMLLoader loader;

    @FXML
    private TableView<XBundleDTO> table;

    @Inject
    @Service
    private DataProvider dataProvider;

    @Inject
    @Named("in.bytehue.osgifx.console.ui.bundles")
    private BundleContext context;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        final Node expandedNode = Fx.loadFXML(loader, context, "/fxml/expander-column-content.fxml");

        final TableRowExpanderColumn<XBundleDTO> expanderColumn     = new TableRowExpanderColumn<>(param -> expandedNode);
        final TableColumn<XBundleDTO, String>    symbolicNameColumn = new TableColumn<>("Symbolic Name");
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

        table.setItems(dataProvider.bundles());

        TableFilter.forTableView(table).apply();
    }

}
