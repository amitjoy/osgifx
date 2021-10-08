package in.bytehue.osgifx.console.ui.properties;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;

import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.ui.service.DataProvider;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class PropertiesFxController implements Initializable {

    @Inject
    private DataProvider dataProvider;

    @FXML
    private TableView<XPropertyDTO> propertyTable;

    @FXML
    private TableColumn<XPropertyDTO, String> propertyName;

    @FXML
    private TableColumn<XPropertyDTO, String> propertyValue;

    @FXML
    private TableColumn<XPropertyDTO, String> propertyType;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        propertyTable.setSelectionModel(null);

        propertyName.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        propertyValue.setCellValueFactory(new DTOCellValueFactory<>("value", String.class));
        propertyType.setCellValueFactory(new DTOCellValueFactory<>("type", String.class));

        propertyTable.setItems(FXCollections.observableArrayList(dataProvider.properties()));

        TableFilter.forTableView(propertyTable).apply();
    }

}
