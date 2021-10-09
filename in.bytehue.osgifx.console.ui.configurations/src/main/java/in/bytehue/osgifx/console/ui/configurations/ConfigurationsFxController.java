package in.bytehue.osgifx.console.ui.configurations;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
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
import javafx.scene.layout.BorderPane;

public final class ConfigurationsFxController implements Initializable {

    @Inject
    @LocalInstance
    private FXMLLoader loader;

    @Inject
    private DataProvider dataProvider;

    @FXML
    private TableView<XConfigurationDTO> table;

    @Inject
    @Named("in.bytehue.osgifx.console.ui.configurations")
    private BundleContext context;

    private TableRowExpanderColumn.TableRowDataFeatures<XConfigurationDTO> selectedConfiguration;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        table.setSelectionModel(new NullTableViewSelectionModel<>(table));
        createControls();
    }

    private void createControls() {
        final BorderPane                                expandedNode   = (BorderPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final ConfigurationEditorFxController           controller     = loader.getController();
        final TableRowExpanderColumn<XConfigurationDTO> expanderColumn = new TableRowExpanderColumn<>(expandedConfig -> {
                                                                           controller.initControls(expandedConfig.getValue());
                                                                           if (selectedConfiguration != null
                                                                                   && selectedConfiguration.isExpanded()) {
                                                                               selectedConfiguration.toggleExpanded();
                                                                           }
                                                                           selectedConfiguration = expandedConfig;
                                                                           return expandedNode;
                                                                       });

        final TableColumn<XConfigurationDTO, String> pidColumn = new TableColumn<>("PID");
        pidColumn.setPrefWidth(550);
        pidColumn.setCellValueFactory(
                new DTOCellValueFactory<>("pid", String.class, s -> "Not created yet but property descriptor available"));

        final TableColumn<XConfigurationDTO, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setPrefWidth(400);
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class,
                s -> Optional.ofNullable(s.ocd).map(v -> v.name).orElse("No property descriptor available")));

        final TableColumn<XConfigurationDTO, String> locationColumn = new TableColumn<>("Location");
        locationColumn.setPrefWidth(150);
        locationColumn.setCellValueFactory(new DTOCellValueFactory<>("location", String.class, s -> "No PID associated"));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(pidColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(locationColumn);

        final ObservableList<XConfigurationDTO> configurations = dataProvider.configurations();
        table.setItems(configurations);

        TableFilter.forTableView(table).apply();
    }

}
