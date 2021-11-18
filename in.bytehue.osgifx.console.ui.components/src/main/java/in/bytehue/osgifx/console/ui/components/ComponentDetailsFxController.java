package in.bytehue.osgifx.console.ui.components;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.fx.core.command.CommandService;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XReferenceDTO;
import in.bytehue.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import in.bytehue.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import in.bytehue.osgifx.console.util.fx.Fx;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

public final class ComponentDetailsFxController {

    private static final String COMPONENT_ENABLE_COMMAND_ID  = "in.bytehue.osgifx.console.application.command.component.enable";
    private static final String COMPONENT_DISABLE_COMMAND_ID = "in.bytehue.osgifx.console.application.command.component.disable";

    @FXML
    private Label                                         idLabel;
    @FXML
    private Label                                         componentNameLabel;
    @FXML
    private Label                                         stateLabel;
    @FXML
    private Label                                         bundleLabel;
    @FXML
    private Label                                         bundleIdLabel;
    @FXML
    private Label                                         factoryLabel;
    @FXML
    private Label                                         scopeLabel;
    @FXML
    private Label                                         classLabel;
    @FXML
    private Label                                         policyLabel;
    @FXML
    private Label                                         failureLabel;
    @FXML
    private Label                                         activateLabel;
    @FXML
    private Label                                         deactivateLabel;
    @FXML
    private Label                                         modifiedLabel;
    @FXML
    private Button                                        enableComponentButton;
    @FXML
    private Button                                        disableComponentButton;
    @FXML
    private ListView<String>                              pidsList;
    @FXML
    private ListView<String>                              interfacesList;
    @FXML
    private TableView<Entry<String, String>>              propertiesTable;
    @FXML
    private TableColumn<Entry<String, String>, String>    propertiesTableColumn1;
    @FXML
    private TableColumn<Entry<String, String>, String>    propertiesTableColumn2;
    @FXML
    private TableView<XReferenceDTO>                      referencesTable;
    @FXML
    private TableView<XSatisfiedReferenceDTO>             boundServicesTable;
    @FXML
    private TableColumn<XSatisfiedReferenceDTO, String>   boundServicesNameColumn;
    @FXML
    private TableColumn<XSatisfiedReferenceDTO, String>   boundServicesTargetColumn;
    @FXML
    private TableColumn<XSatisfiedReferenceDTO, String>   boundServicesClassColumn;
    @FXML
    private TableView<XUnsatisfiedReferenceDTO>           unboundServicesTable;
    @FXML
    private TableColumn<XUnsatisfiedReferenceDTO, String> unboundServicesNameColumn;
    @FXML
    private TableColumn<XUnsatisfiedReferenceDTO, String> unboundServicesTargetColumn;
    @FXML
    private TableColumn<XUnsatisfiedReferenceDTO, String> unboundServicesClassColumn;
    @Log
    @Inject
    private FluentLogger                                  logger;
    @Inject
    @LocalInstance
    private FXMLLoader                                    loader;
    @Inject
    @Named("in.bytehue.osgifx.console.ui.components")
    private BundleContext                                 context;
    @Inject
    private CommandService                                commandService;
    private TableRowDataFeatures<XReferenceDTO>           selectedReference;
    private AtomicBoolean                                 areReferenceTableNodesLoader;

    @FXML
    public void initialize() {
        areReferenceTableNodesLoader = new AtomicBoolean();
        logger.atDebug().log("FXML controller has been initialized");
    }

    void initControls(final XComponentDTO component) {
        registerButtonHandlers(component);
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

        initConditionalComponents(component);

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

        Fx.addContextMenuToCopyContent(pidsList);
        Fx.addContextMenuToCopyContent(interfacesList);
        Fx.addContextMenuToCopyContent(propertiesTable);
        Fx.addContextMenuToCopyContent(boundServicesTable);
        Fx.addContextMenuToCopyContent(unboundServicesTable);

        Fx.disableSelectionModel(referencesTable);
    }

    private void initConditionalComponents(final XComponentDTO component) {
        enableComponentButton.setDisable(!"DISABLED".equals(component.state));
        disableComponentButton.setDisable("DISABLED".equals(component.state));
    }

    private void createReferenceExpandedTable(final XComponentDTO component) {
        final GridPane                              expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/sub-expander-column-content.fxml");
        final ReferenceDetailsFxController          controller     = loader.getController();
        final TableRowExpanderColumn<XReferenceDTO> expanderColumn = new TableRowExpanderColumn<>(expandedReference -> {
                                                                       controller.initControls(expandedReference.getValue());
                                                                       if (selectedReference != null) {
                                                                           selectedReference.toggleExpanded();
                                                                       }
                                                                       selectedReference = expandedReference;
                                                                       return expandedNode;
                                                                   });

        final TableColumn<XReferenceDTO, String> nameColumn = new TableColumn<>("Name");

        nameColumn.setPrefWidth(200);
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final TableColumn<XReferenceDTO, String> interfaceColumn = new TableColumn<>("Interface");

        interfaceColumn.setPrefWidth(550);
        interfaceColumn.setCellValueFactory(new DTOCellValueFactory<>("interfaceName", String.class));

        if (!areReferenceTableNodesLoader.get()) {
            referencesTable.getColumns().add(expanderColumn);
            referencesTable.getColumns().add(nameColumn);
            referencesTable.getColumns().add(interfaceColumn);
        }

        referencesTable.setItems(FXCollections.observableArrayList(component.references));

        areReferenceTableNodesLoader.set(true);
    }

    private void registerButtonHandlers(final XComponentDTO component) {
        enableComponentButton.setOnAction(a -> {
            logger.atInfo().log("Component enable request has been sent for %s", component.name);
            // to enable a component, we need the name primarily as there is no associated component ID
            commandService.execute(COMPONENT_ENABLE_COMMAND_ID, createCommandMap(component.name, null));
        });
        disableComponentButton.setOnAction(a -> {
            logger.atInfo().log("Component disable request has been sent for %s", component.id);
            // to disable a component, we need the id primarily as there is already an associated component ID
            commandService.execute(COMPONENT_DISABLE_COMMAND_ID, createCommandMap(null, component.id));
        });
    }

    private Map<String, Object> createCommandMap(final String name, final String id) {
        final Map<String, Object> properties = new HashMap<>();
        properties.computeIfAbsent("name", key -> name);
        properties.computeIfAbsent("id", key -> id);
        return properties;
    }

    private void applyTableFilters() {
        TableFilter.forTableView(referencesTable).apply();
        TableFilter.forTableView(propertiesTable).apply();
        TableFilter.forTableView(boundServicesTable).apply();
        TableFilter.forTableView(unboundServicesTable).apply();
    }

}
