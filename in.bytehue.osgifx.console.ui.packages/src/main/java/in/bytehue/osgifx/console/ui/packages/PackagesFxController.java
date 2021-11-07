package in.bytehue.osgifx.console.ui.packages;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, attribute = "(objectClass=in.bytehue.osgifx.console.data.provider.DataProvider)")
public final class PackagesFxController {

    @Log
    @Inject
    private FluentLogger                     logger;
    @Inject
    @LocalInstance
    private FXMLLoader                       loader;
    @FXML
    private TableView<PackageDTO>            table;
    @Inject
    private DataProvider                     dataProvider;
    @Inject
    @Named("in.bytehue.osgifx.console.ui.packages")
    private BundleContext                    context;
    private TableRowDataFeatures<PackageDTO> selectedPackage;

    @FXML
    public void initialize() {
        createControls();
        Fx.disableSelectionModel(table);
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void createControls() {
        final GridPane                           expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final PackageDetailsFxController         controller     = loader.getController();
        final TableRowExpanderColumn<PackageDTO> expanderColumn = new TableRowExpanderColumn<>(expandedPackage -> {
                                                                    controller.initControls(expandedPackage.getValue());
                                                                    if (selectedPackage != null && selectedPackage.isExpanded()) {
                                                                        selectedPackage.toggleExpanded();
                                                                    }
                                                                    selectedPackage = expandedPackage;
                                                                    return expandedNode;
                                                                });

        final TableColumn<PackageDTO, String> nameColumn = new TableColumn<>("Name");

        nameColumn.setPrefWidth(450);
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final TableColumn<PackageDTO, String> versionColumn = new TableColumn<>("Version");

        versionColumn.setPrefWidth(450);
        versionColumn.setCellValueFactory(new DTOCellValueFactory<>("version", String.class));

        final TableColumn<PackageDTO, String> hasDuplicatesColumn = new TableColumn<>("Is Duplicate Export?");

        hasDuplicatesColumn.setPrefWidth(200);
        hasDuplicatesColumn.setCellValueFactory(new DTOCellValueFactory<>("isDuplicateExport", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(versionColumn);
        table.getColumns().add(hasDuplicatesColumn);

        final ObservableList<XBundleDTO> bundles = dataProvider.bundles();
        table.setItems(PackageHelper.prepareList(bundles, context));
        Fx.sortBy(table, nameColumn);

        TableFilter.forTableView(table).apply();
    }

}
