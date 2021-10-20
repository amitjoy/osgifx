package in.bytehue.osgifx.console.ui.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XServiceDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

public final class ServicesFxController {

    @Log
    @Inject
    private FluentLogger           logger;
    @Inject
    @LocalInstance
    private FXMLLoader             loader;
    @Inject
    private DataProvider           dataProvider;
    @FXML
    private TableView<XServiceDTO> table;
    @Inject
    @Named("in.bytehue.osgifx.console.ui.services")
    private BundleContext          context;

    private TableRowExpanderColumn.TableRowDataFeatures<XServiceDTO> selectedService;

    @FXML
    public void initialize() {
        createControls();
        Fx.disableSelectionModel(table);
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void createControls() {
        final GridPane                            expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final ServiceDetailsFxController          controller     = loader.getController();
        final TableRowExpanderColumn<XServiceDTO> expanderColumn = new TableRowExpanderColumn<>(expandedService -> {
                                                                     controller.initControls(expandedService.getValue());
                                                                     if (selectedService != null && selectedService.isExpanded()) {
                                                                         selectedService.toggleExpanded();
                                                                     }
                                                                     selectedService = expandedService;
                                                                     return expandedNode;
                                                                 });

        final TableColumn<XServiceDTO, String> serviceIdColumn = new TableColumn<>("Service ID");

        serviceIdColumn.setPrefWidth(100);
        serviceIdColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));

        final TableColumn<XServiceDTO, String> objectClassColumn = new TableColumn<>("Object Class");

        objectClassColumn.setPrefWidth(600);
        objectClassColumn.setCellValueFactory(new DTOCellValueFactory<>("types", String.class));

        final TableColumn<XServiceDTO, String> registeringBundleColumn = new TableColumn<>("Registering Bundle");

        registeringBundleColumn.setPrefWidth(400);
        registeringBundleColumn.setCellValueFactory(new DTOCellValueFactory<>("registeringBundle", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(serviceIdColumn);
        table.getColumns().add(objectClassColumn);
        table.getColumns().add(registeringBundleColumn);

        final ObservableList<XServiceDTO> services = dataProvider.services();
        table.setItems(services);
        Fx.sortBy(table, objectClassColumn);

        TableFilter.forTableView(table).apply();
    }

}
