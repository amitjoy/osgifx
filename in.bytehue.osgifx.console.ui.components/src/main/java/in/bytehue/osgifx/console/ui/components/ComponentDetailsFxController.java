package in.bytehue.osgifx.console.ui.components;

import java.net.URL;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.dto.ReferenceDTO;

import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import in.bytehue.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

public final class ComponentDetailsFxController implements Initializable {

    @FXML
    private Label idLabel;

    @FXML
    private Label componentNameLabel;

    @FXML
    private Label stateLabel;

    @FXML
    private Label bundleLabel;

    @FXML
    private Label bundleIdLabel;

    @FXML
    private Label factoryLabel;

    @FXML
    private Label scopeLabel;

    @FXML
    private Label classLabel;

    @FXML
    private Label policyLabel;

    @FXML
    private Label failureLabel;

    @FXML
    private Label activateLabel;

    @FXML
    private Label deactivateLabel;

    @FXML
    private Label modifiedLabel;

    @FXML
    private ListView<String> pidsList;

    @FXML
    private ListView<String> interfacesList;

    @FXML
    private TableView<Entry<String, String>> propertiesTable;

    @FXML
    private TableColumn<Entry<String, String>, String> propertiesTableColumn1;

    @FXML
    private TableColumn<Entry<String, String>, String> propertiesTableColumn2;

    @FXML
    private TableView<ReferenceDTO> referencesTable;

    @FXML
    private TableView<XSatisfiedReferenceDTO> boundServicesTable;

    @FXML
    private TableColumn<XSatisfiedReferenceDTO, String> boundServicesNameColumn;

    @FXML
    private TableColumn<XSatisfiedReferenceDTO, String> boundServicesTargetColumn;

    @FXML
    private TableColumn<XSatisfiedReferenceDTO, String> boundServicesClassColumn;

    @FXML
    private TableView<XUnsatisfiedReferenceDTO> unboundServicesTable;

    @FXML
    private TableColumn<XUnsatisfiedReferenceDTO, String> unboundServicesNameColumn;

    @FXML
    private TableColumn<XUnsatisfiedReferenceDTO, String> unboundServicesTargetColumn;

    @FXML
    private TableColumn<XUnsatisfiedReferenceDTO, String> unboundServicesClassColumn;

    @Inject
    @LocalInstance
    private FXMLLoader loader;

    @Inject
    @Named("in.bytehue.osgifx.console.ui.components")
    private BundleContext context;

    private final AtomicBoolean areReferenceTableNodesLoader = new AtomicBoolean();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
    }

    void setValue(final XComponentDTO component) {
        idLabel.setText(String.valueOf(component.id));
        componentNameLabel.setText(component.name);
        stateLabel.setText(component.state);
        bundleLabel.setText(component.registeringBundle);
        bundleIdLabel.setText(String.valueOf(component.registeringBundleId));
        factoryLabel.setText(component.factory);
        scopeLabel.setText(component.scope);
        classLabel.setText(component.implementationClass);
        policyLabel.setText(component.configurationPolicy);
        failureLabel.setText(component.failure);
        activateLabel.setText(component.activate);
        deactivateLabel.setText(component.deactivate);
        modifiedLabel.setText(component.modified);

        pidsList.getItems().clear();
        pidsList.getItems().addAll(component.configurationPid);

        interfacesList.getItems().clear();
        interfacesList.getItems().addAll(component.serviceInterfaces);

        propertiesTableColumn1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));
        propertiesTableColumn2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue()));
        propertiesTable.setItems(FXCollections.observableArrayList(component.properties.entrySet()));

        boundServicesNameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        boundServicesTargetColumn.setCellValueFactory(new DTOCellValueFactory<>("target", String.class));
        boundServicesClassColumn.setCellValueFactory(new DTOCellValueFactory<>("objectClass", String.class));

        boundServicesTable.setItems(FXCollections.observableArrayList(component.satisfiedReferences));

        unboundServicesNameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        unboundServicesTargetColumn.setCellValueFactory(new DTOCellValueFactory<>("target", String.class));
        unboundServicesClassColumn.setCellValueFactory(new DTOCellValueFactory<>("objectClass", String.class));

        unboundServicesTable.setItems(FXCollections.observableArrayList(component.unsatisfiedReferences));

        createReferenceExpandedTable(component);
        applyTableFilters();
    }

    private void createReferenceExpandedTable(final XComponentDTO component) {
        final GridPane                             expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/sub-expander-column-content.fxml");
        final ReferenceDetailsFxController         controller     = loader.getController();
        final TableRowExpanderColumn<ReferenceDTO> expanderColumn = new TableRowExpanderColumn<>(param -> {
                                                                      controller.setValue(param.getValue());
                                                                      return expandedNode;
                                                                  });

        final TableColumn<ReferenceDTO, String> nameColumn = new TableColumn<>("Name");

        nameColumn.setPrefWidth(200);
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final TableColumn<ReferenceDTO, String> interfaceColumn = new TableColumn<>("Interface");

        interfaceColumn.setPrefWidth(550);
        interfaceColumn.setCellValueFactory(new DTOCellValueFactory<>("interfaceName", String.class));

        if (!areReferenceTableNodesLoader.get()) {
            referencesTable.getColumns().add(expanderColumn);
            referencesTable.getColumns().add(nameColumn);
            referencesTable.getColumns().add(interfaceColumn);
        }

        referencesTable.setItems(FXCollections.observableArrayList(component.references));

        TableFilter.forTableView(referencesTable).apply();
        areReferenceTableNodesLoader.set(true);
    }

    private void applyTableFilters() {
        TableFilter.forTableView(propertiesTable).apply();
        TableFilter.forTableView(referencesTable).apply();
        TableFilter.forTableView(boundServicesTable).apply();
        TableFilter.forTableView(unboundServicesTable).apply();
    }

}
