package in.bytehue.osgifx.console.ui.properties;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.log.Log;
import org.eclipse.fx.core.log.Logger;

import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import in.bytehue.osgifx.console.util.fx.NullTableViewSelectionModel;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class PropertiesFxController {

    @Log
    @Inject
    private Logger                            logger;
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
        propertyTable.setSelectionModel(new NullTableViewSelectionModel<>(propertyTable));

        propertyName.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        propertyValue.setCellValueFactory(new DTOCellValueFactory<>("value", String.class));
        propertyType.setCellValueFactory(new DTOCellValueFactory<>("type", String.class));

        propertyTable.setItems(dataProvider.properties());
        Fx.sortBy(propertyTable, propertyName);

        TableFilter.forTableView(propertyTable).apply();

        logger.debug("FXML controller (" + getClass() + ") has been initialized");
    }

}
