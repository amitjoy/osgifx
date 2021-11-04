package in.bytehue.osgifx.console.ui.properties;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;

import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.data.provider.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, attribute = "(objectClass=in.bytehue.osgifx.console.data.provider.DataProvider)")
public final class PropertiesFxController {

    @Log
    @Inject
    private FluentLogger                      logger;
    @FXML
    private TableView<XPropertyDTO>           propertyTable;
    @FXML
    private TableColumn<XPropertyDTO, String> propertyName;
    @FXML
    private TableColumn<XPropertyDTO, String> propertyValue;
    @FXML
    private TableColumn<XPropertyDTO, String> propertyType;
    @Inject
    private DataProvider                      dataProvider;

    @FXML
    public void initialize() {
        propertyName.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        propertyValue.setCellValueFactory(new DTOCellValueFactory<>("value", String.class));
        propertyType.setCellValueFactory(new DTOCellValueFactory<>("type", String.class));

        propertyTable.setItems(dataProvider.properties());
        Fx.sortBy(propertyTable, propertyName);

        TableFilter.forTableView(propertyTable).apply();
        Fx.addContextMenuToCopyContent(propertyTable);

        logger.atDebug().log("FXML controller has been initialized");
    }

}
