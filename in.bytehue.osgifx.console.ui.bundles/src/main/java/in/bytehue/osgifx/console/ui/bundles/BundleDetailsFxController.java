package in.bytehue.osgifx.console.ui.bundles;

import java.net.URL;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.table.TableFilter;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;
import in.bytehue.osgifx.console.agent.dto.XPackageDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceInfoDTO;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class BundleDetailsFxController implements Initializable {

    @FXML
    private Label idLabel;

    @FXML
    private Label bsnLabel;

    @FXML
    private Label stateLabel;

    @FXML
    private Label versionLabel;

    @FXML
    private Label locationLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private ToggleSwitch fragmentLabel;

    @FXML
    private Label lasModifiedLabel;

    @FXML
    private Label docLabel;

    @FXML
    private Label vendorLabel;

    @FXML
    private Label descLabel;

    @FXML
    private Label startLevelLabel;

    @FXML
    private TableView<XPackageDTO> exportedPackagesNameTable;

    @FXML
    private TableColumn<XPackageDTO, String> exportedPackagesNameTableColumn;

    @FXML
    private TableColumn<XPackageDTO, String> exportedPackagesVersionTableColumn;

    @FXML
    private TableView<XServiceInfoDTO> registeredServicesTable;

    @FXML
    private TableColumn<XServiceInfoDTO, String> registeredServicesIdTableColumn;

    @FXML
    private TableColumn<XServiceInfoDTO, String> registeredServicesClassTableColumn;

    @FXML
    private TableView<Entry<String, String>> manifestHeadersTable;

    @FXML
    private TableColumn<Entry<String, String>, String> manifestHeadersTableColumn1;

    @FXML
    private TableColumn<Entry<String, String>, String> manifestHeadersTableColumn2;

    @FXML
    private TableView<XPackageDTO> importedPackagesTable;

    @FXML
    private TableColumn<XPackageDTO, String> importedPackagesNameTableColumn;

    @FXML
    private TableColumn<XPackageDTO, String> importedPackagesVersionTableColumn;

    @FXML
    private TableView<XBundleInfoDTO> wiredBundlesTable;

    @FXML
    private TableColumn<XBundleInfoDTO, String> wiredBundlesIdTableColumn;

    @FXML
    private TableColumn<XBundleInfoDTO, String> wiredBundlesBsnTableColumn;

    @FXML
    private TableView<XServiceInfoDTO> usedServicesTable;

    @FXML
    private TableColumn<XServiceInfoDTO, String> usedServicesIdTableColumn;

    @FXML
    private TableColumn<XServiceInfoDTO, String> usedServicesClassTableColumn;

    @FXML
    private TableView<XBundleInfoDTO> hostBundlesTable;

    @FXML
    private TableColumn<XBundleInfoDTO, String> hostBundlesIdTableColumn;

    @FXML
    private TableColumn<XBundleInfoDTO, String> hostBundlesBsnTableColumn;

    @FXML
    private TableView<XBundleInfoDTO> attachedFragmentsTable;

    @FXML
    private TableColumn<XBundleInfoDTO, String> attachedFragmentsIdTableColumn;

    @FXML
    private TableColumn<XBundleInfoDTO, String> attachedFragmentsBsnTableColumn;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    void setValue(final XBundleDTO bundle) {
        idLabel.setText(String.valueOf(bundle.id));
        stateLabel.setText(bundle.state);
        bsnLabel.setText(bundle.symbolicName);
        versionLabel.setText(bundle.version);
        locationLabel.setText(bundle.location);
        categoryLabel.setText(bundle.category);
        // reset before
        fragmentLabel.setSelected(false);
        if (bundle.isFragment) {
            fragmentLabel.setSelected(true);
        }
        lasModifiedLabel.setText(String.valueOf(bundle.lastModified));
        docLabel.setText(bundle.documentation);
        vendorLabel.setText(bundle.vendor);
        descLabel.setText(bundle.description);
        startLevelLabel.setText(String.valueOf(bundle.startLevel));

        exportedPackagesNameTableColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        exportedPackagesVersionTableColumn.setCellValueFactory(new DTOCellValueFactory<>("version", String.class));
        exportedPackagesNameTable.setItems(FXCollections.observableArrayList(bundle.exportedPackages));

        importedPackagesNameTableColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        importedPackagesVersionTableColumn.setCellValueFactory(new DTOCellValueFactory<>("version", String.class));
        importedPackagesTable.setItems(FXCollections.observableArrayList(bundle.importedPackages));

        wiredBundlesIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        wiredBundlesBsnTableColumn.setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        wiredBundlesTable.setItems(FXCollections.observableArrayList(bundle.wiredBundles));

        registeredServicesIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        registeredServicesClassTableColumn.setCellValueFactory(new DTOCellValueFactory<>("objectClass", String.class));
        registeredServicesTable.setItems(FXCollections.observableArrayList(bundle.registeredServices));

        usedServicesIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        usedServicesClassTableColumn.setCellValueFactory(new DTOCellValueFactory<>("objectClass", String.class));
        usedServicesTable.setItems(FXCollections.observableArrayList(bundle.usedServices));

        hostBundlesIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        hostBundlesBsnTableColumn.setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        hostBundlesTable.setItems(FXCollections.observableArrayList(bundle.hostBundles));

        attachedFragmentsIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        attachedFragmentsBsnTableColumn.setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        attachedFragmentsTable.setItems(FXCollections.observableArrayList(bundle.fragmentsAttached));

        manifestHeadersTableColumn1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));
        manifestHeadersTableColumn2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue()));
        manifestHeadersTable.setItems(FXCollections.observableArrayList(bundle.manifestHeaders.entrySet()));

        applyTableFilters();
    }

    private void applyTableFilters() {
        TableFilter.forTableView(exportedPackagesNameTable).apply();
        TableFilter.forTableView(registeredServicesTable).apply();
        TableFilter.forTableView(manifestHeadersTable).apply();
        TableFilter.forTableView(importedPackagesTable).apply();
        TableFilter.forTableView(wiredBundlesTable).apply();
        TableFilter.forTableView(usedServicesTable).apply();
        TableFilter.forTableView(hostBundlesTable).apply();
        TableFilter.forTableView(attachedFragmentsTable).apply();
    }

}
