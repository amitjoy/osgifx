package in.bytehue.osgifx.console.ui.components;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
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

public final class ComponentsFxController implements Initializable {

    @Inject
    @LocalInstance
    private FXMLLoader loader;

    @FXML
    private TableView<XComponentDTO> table;

    @Inject
    private DataProvider dataProvider;

    @Inject
    @Named("in.bytehue.osgifx.console.ui.components")
    private BundleContext context;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        createControls();
    }

    private void createControls() {
        final GridPane                              expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final ComponentDetailsFxController          controller     = loader.getController();
        final TableRowExpanderColumn<XComponentDTO> expanderColumn = new TableRowExpanderColumn<>(param -> {
                                                                       controller.setValue(param.getValue());
                                                                       return expandedNode;
                                                                   });

        final TableColumn<XComponentDTO, String> componentNameColumn = new TableColumn<>("Name");

        componentNameColumn.setPrefWidth(900);
        componentNameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final TableColumn<XComponentDTO, String> stateColumn = new TableColumn<>("State");

        stateColumn.setPrefWidth(200);
        stateColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(componentNameColumn);
        table.getColumns().add(stateColumn);

        final ObservableList<XComponentDTO> bundles = dataProvider.components();
        table.setItems(bundles);

        TableFilter.forTableView(table).apply();
    }

}
