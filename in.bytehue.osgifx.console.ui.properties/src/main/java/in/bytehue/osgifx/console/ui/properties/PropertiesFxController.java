package in.bytehue.osgifx.console.ui.properties;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;

import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.NullTableViewSelectionModel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class PropertiesFxController implements Initializable {

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

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        propertyTable.setSelectionModel(new NullTableViewSelectionModel<>(propertyTable));

        propertyName.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        propertyValue.setCellValueFactory(new DTOCellValueFactory<>("value", String.class));
        propertyType.setCellValueFactory(new DTOCellValueFactory<>("type", String.class));

        propertyTable.setItems(dataProvider.properties());

        TableFilter.forTableView(propertyTable).apply();
    }

}
