package in.bytehue.osgifx.console.ui.bundles;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.command.CommandService;

import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XBundleInfoDTO;
import in.bytehue.osgifx.console.agent.dto.XPackageDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceInfoDTO;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class BundleDetailsFxController implements Initializable {

    private static final String BUNDLE_START_COMMAND_ID     = "in.bytehue.osgifx.console.application.command.bundle.start";
    private static final String BUNDLE_STOP_COMMAND_ID      = "in.bytehue.osgifx.console.application.command.bundle.stop";
    private static final String BUNDLE_UNINSTALL_COMMAND_ID = "in.bytehue.osgifx.console.application.command.bundle.uninstall";

    @Inject
    private CommandService commandService;

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
    private Button startBundleButton;

    @FXML
    private Button stopBundleButton;

    @FXML
    private Button uninstallBundleButton;

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

    void initControls(final XBundleDTO bundle) {
        idLabel.setText(String.valueOf(bundle.id));
        stateLabel.setText(bundle.state);
        bsnLabel.setText(bundle.symbolicName);
        versionLabel.setText(bundle.version);
        locationLabel.setText(bundle.location);
        categoryLabel.setText(bundle.category);
        initConditionalControls(bundle);
        registerButtonHandlers(bundle);
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

    private void initConditionalControls(final XBundleDTO bundle) {
        if (bundle.isFragment) {
            initControlsForFragements();
        } else {
            initControlsForBundles(bundle);
        }
    }

    private void initControlsForBundles(final XBundleDTO bundle) {
        if ("ACTIVE".equals(bundle.state)) {
            startBundleButton.setDisable(true);
        } else {
            startBundleButton.setDisable(false);
        }
        if ("RESOLVED".equals(bundle.state)) {
            stopBundleButton.setDisable(true);
        } else {
            stopBundleButton.setDisable(false);
        }
        fragmentLabel.setSelected(false);
    }

    private void initControlsForFragements() {
        startBundleButton.setDisable(true);
        stopBundleButton.setDisable(true);
        fragmentLabel.setSelected(true);
    }

    private void registerButtonHandlers(final XBundleDTO bundle) {
        startBundleButton.setOnAction(a -> {
            commandService.execute(BUNDLE_START_COMMAND_ID, createCommandMap(bundle.id));
        });
        stopBundleButton.setOnAction(a -> {
            commandService.execute(BUNDLE_STOP_COMMAND_ID, createCommandMap(bundle.id));
        });
        uninstallBundleButton.setOnAction(a -> {
            commandService.execute(BUNDLE_UNINSTALL_COMMAND_ID, createCommandMap(bundle.id));
        });
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

    private Map<String, Object> createCommandMap(final long value) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("id", value);
        return properties;
    }

}
