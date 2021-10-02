package in.bytehue.osgifx.console.ui.bundles;

import java.io.IOException;
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

import in.bytehue.osgifx.console.ui.dto.BundleFxDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public final class BundlesFxController implements Initializable {

    @FXML
    private TableView<BundleFxDTO> table;

    @Log
    @Inject
    private Logger logger;

    @Inject
    @LocalInstance
    private FXMLLoader loader;

    @Inject
    @Service
    private DataProvider dataProvider;

    @Inject
    @Named("in.bytehue.osgifx.console.ui.bundles")
    private BundleContext context;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        final URL fxml = context.getBundle().getResource("/fxml/expander-column-content.fxml");
        loader.setLocation(fxml);
        final Node n = loadFxml();

        final TableRowExpanderColumn<BundleFxDTO> expanderColumn     = new TableRowExpanderColumn<>(param -> n);
        final TableColumn<BundleFxDTO, String>    symbolicNameColumn = new TableColumn<>("Symbolic Name");
        symbolicNameColumn.setPrefWidth(450);
        symbolicNameColumn.setCellValueFactory(new PropertyValueFactory<>("symbolicName"));

        final TableColumn<BundleFxDTO, String> versionColumn = new TableColumn<>("Version");
        versionColumn.setPrefWidth(450);
        versionColumn.setCellValueFactory(new PropertyValueFactory<>("version"));

        final TableColumn<BundleFxDTO, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setPrefWidth(200);
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(symbolicNameColumn);
        table.getColumns().add(versionColumn);
        table.getColumns().add(statusColumn);

        table.setItems(dataProvider.bundles());

        TableFilter.forTableView(table).apply();
    }

    private Node loadFxml() {
        try {
            return loader.load();
        } catch (final IOException e) {
            logger.error("Failed to load fxml", e);
        }
        return null;
    }

}
