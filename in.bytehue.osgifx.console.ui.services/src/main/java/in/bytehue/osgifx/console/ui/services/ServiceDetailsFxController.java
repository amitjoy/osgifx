package in.bytehue.osgifx.console.ui.services;

import java.util.Map.Entry;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import in.bytehue.osgifx.console.agent.dto.XServiceDTO;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class ServiceDetailsFxController {

    @Log
    @Inject
    private FluentLogger                               logger;
    @FXML
    private Label                                      idLabel;
    @FXML
    private Label                                      bundleBsnLabel;
    @FXML
    private Label                                      bundleIdLabel;
    @FXML
    private TableView<Entry<String, String>>           propertiesTable;
    @FXML
    private TableColumn<Entry<String, String>, String> propertiesTableColumn1;
    @FXML
    private TableColumn<Entry<String, String>, String> propertiesTableColumn2;
    @FXML
    private ListView<String>                           objectClassesList;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    void initControls(final XServiceDTO service) {
        idLabel.setText(String.valueOf(service.id));
        bundleBsnLabel.setText(service.registeringBundle);
        bundleIdLabel.setText(String.valueOf(service.bundleId));

        propertiesTableColumn1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));
        propertiesTableColumn2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue()));
        propertiesTable.setItems(FXCollections.observableArrayList(service.properties.entrySet()));

        objectClassesList.getItems().clear();
        objectClassesList.getItems().addAll(service.types);

        applyTableFilter();

        Fx.disableSelectionModel(propertiesTable);
    }

    private void applyTableFilter() {
        TableFilter.forTableView(propertiesTable).apply();
    }

}
