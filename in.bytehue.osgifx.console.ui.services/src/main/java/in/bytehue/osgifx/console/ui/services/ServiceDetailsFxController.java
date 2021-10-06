package in.bytehue.osgifx.console.ui.services;

import java.net.URL;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.controlsfx.control.table.TableFilter;

import in.bytehue.osgifx.console.agent.dto.XServiceDTO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class ServiceDetailsFxController implements Initializable {

    @FXML
    private Label idLabel;

    @FXML
    private Label bundleBsnLabel;

    @FXML
    private Label bundleIdLabel;

    @FXML
    private TableView<Entry<String, String>> propertiesTable;

    @FXML
    private TableColumn<Entry<String, String>, String> propertiesTableColumn1;

    @FXML
    private TableColumn<Entry<String, String>, String> propertiesTableColumn2;

    @FXML
    private ListView<String> objectClassesList;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    void setValue(final XServiceDTO service) {
        idLabel.setText(String.valueOf(service.id));
        bundleBsnLabel.setText(service.registeringBundle);
        bundleIdLabel.setText(String.valueOf(service.bundleId));

        propertiesTableColumn1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));
        propertiesTableColumn2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue()));
        propertiesTable.setItems(FXCollections.observableArrayList(service.properties.entrySet()));

        objectClassesList.getItems().addAll(service.types);

        applyTableFilter();
    }

    private void applyTableFilter() {
        TableFilter.forTableView(propertiesTable).apply();
    }

}
