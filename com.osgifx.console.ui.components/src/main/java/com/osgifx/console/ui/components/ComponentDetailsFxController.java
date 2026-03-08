/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.ui.components;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_COMPONENTS_TOPIC;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.command.CommandService;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XReferenceDTO;
import com.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import com.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.beans.binding.When;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public final class ComponentDetailsFxController {

    private static final String COMPONENT_ENABLE_COMMAND_ID  = "com.osgifx.console.application.command.component.enable";
    private static final String COMPONENT_DISABLE_COMMAND_ID = "com.osgifx.console.application.command.component.disable";

    @FXML
    private TextField                                     idLabel;
    @FXML
    private TextField                                     componentNameLabel;
    @FXML
    private TextField                                     stateLabel;
    @FXML
    private TextField                                     bundleLabel;
    @FXML
    private TextField                                     bundleIdLabel;
    @FXML
    private TextField                                     factoryLabel;
    @FXML
    private TextField                                     scopeLabel;
    @FXML
    private TextField                                     classLabel;
    @FXML
    private TextField                                     policyLabel;
    @FXML
    private TextArea                                      failureText;
    @FXML
    private TextField                                     activateLabel;
    @FXML
    private TextField                                     deactivateLabel;
    @FXML
    private TextField                                     modifiedLabel;
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
    @OSGiBundle
    private BundleContext                                 context;
    @Inject
    private CommandService                                commandService;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                                       isSnapshotAgent;
    @Inject
    private ThreadSynchronize                             threadSync;
    private XComponentDTO                                 selectedComponent;
    private TableRowDataFeatures<XReferenceDTO>           previouslyExpanded;
    private AtomicBoolean                                 areReferenceTableNodesLoader;
    private boolean                                       isInitialized;

    @FXML
    public void initialize() {
        areReferenceTableNodesLoader = new AtomicBoolean();
        logger.atDebug().log("FXML controller has been initialized");
    }

    void initControls(final XComponentDTO component) {
        selectedComponent = component;
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
        failureText.setText(component.failure);
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
        if (!isInitialized) {
            applyTableFilters();

            Fx.addContextMenuToCopyContent(pidsList);
            Fx.addContextMenuToCopyContent(interfacesList);
            Fx.addContextMenuToCopyContent(propertiesTable);
            Fx.addContextMenuToCopyContent(boundServicesTable);
            Fx.addContextMenuToCopyContent(unboundServicesTable);

            Fx.disableSelectionModel(referencesTable);
            referencesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            isInitialized = true;
        }
    }

    private void initConditionalComponents(final XComponentDTO component) {
        final var isSnapshot        = new SimpleBooleanProperty(isSnapshotAgent);
        final var isSnapshotBinding = new When(isSnapshot).then(true).otherwise(false);

        final var state            = component.state;
        final var isEnabled        = new SimpleBooleanProperty(!"DISABLED".equals(state));
        final var isEnabledBinding = new When(isEnabled).then(true).otherwise(false);

        final var isUnsatisfiedReference     = new SimpleBooleanProperty("UNSATISFIED_REFERENCE".equals(state));
        final var isUnsatisfiedConfiguration = new SimpleBooleanProperty("UNSATISFIED_CONFIGURATION".equals(state));
        final var isFailedActivation         = new SimpleBooleanProperty("FAILED_ACTIVATION".equals(state));

        final var isLifecycleDisabled = isUnsatisfiedReference.or(isUnsatisfiedConfiguration).or(isFailedActivation);

        enableComponentButton.disableProperty().bind(isSnapshotBinding.or(isEnabledBinding).or(isLifecycleDisabled));
        disableComponentButton.disableProperty()
                .bind(isSnapshotBinding.or(isEnabledBinding.not()).or(isLifecycleDisabled));
    }

    @Inject
    @Optional
    private void updateOnDataRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_COMPONENTS_TOPIC) final String data) {
        if (selectedComponent == null) {
            return;
        }
        final var updatedComponent = selectedComponent;
        threadSync.asyncExec(() -> initControls(updatedComponent));
    }

    private void createReferenceExpandedTable(final XComponentDTO component) {
        final var expandedNode   = (GridPane) Fx.loadFXML(loader, context, "/fxml/sub-expander-column-content.fxml");
        final var controller     = (ReferenceDetailsFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XReferenceDTO>(current -> {
                                     if (previouslyExpanded != null
                                             && current.getValue() == previouslyExpanded.getValue()) {
                                         return expandedNode;
                                     }
                                     if (previouslyExpanded != null && previouslyExpanded.isExpanded()) {
                                         previouslyExpanded.toggleExpanded();
                                     }
                                     controller.initControls(current.getValue());
                                     previouslyExpanded = current;
                                     return expandedNode;
                                 });
        expanderColumn.setPrefWidth(48);
        expanderColumn.setMaxWidth(48);
        expanderColumn.setMinWidth(48);

        final var nameColumn = new TableColumn<XReferenceDTO, String>("Name");

        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final var interfaceColumn = new TableColumn<XReferenceDTO, String>("Interface");

        interfaceColumn.setCellValueFactory(new DTOCellValueFactory<>("interfaceName", String.class));

        if (!areReferenceTableNodesLoader.get()) {
            referencesTable.getColumns().add(expanderColumn);
            referencesTable.getColumns().add(nameColumn);
            referencesTable.getColumns().add(interfaceColumn);
        }

        referencesTable.setItems(FXCollections.observableArrayList(component.references));
        referencesTable.getSortOrder().add(nameColumn);
        referencesTable.sort();

        areReferenceTableNodesLoader.set(true);
    }

    private void registerButtonHandlers(final XComponentDTO component) {
        enableComponentButton.setOnAction(_ -> {
            logger.atInfo().log("Component enable request has been sent for %s", component.name);
            // to enable a component, we need the name primarily as there is no associated
            // component ID
            commandService.execute(COMPONENT_ENABLE_COMMAND_ID, createCommandMap(component.name, null));
        });
        disableComponentButton.setOnAction(_ -> {
            logger.atInfo().log("Component disable request has been sent for %s", component.id);
            // to disable a component, we need the id primarily as there is already an
            // associated component ID
            commandService.execute(COMPONENT_DISABLE_COMMAND_ID, createCommandMap(null, String.valueOf(component.id)));
        });
    }

    private Map<String, Object> createCommandMap(final String name, final String id) {
        final Map<String, Object> properties = Maps.newHashMap();

        properties.computeIfAbsent("name", _ -> name);
        properties.computeIfAbsent("id", _ -> id);

        return properties;
    }

    private void applyTableFilters() {
        TableFilter.forTableView(referencesTable).apply();

        TableFilter.forTableView(propertiesTable).apply();
        propertiesTable.getSortOrder().add(propertiesTableColumn1);
        propertiesTable.sort();

        TableFilter.forTableView(boundServicesTable).apply();
        boundServicesTable.getSortOrder().add(boundServicesNameColumn);
        boundServicesTable.sort();

        TableFilter.forTableView(unboundServicesTable).apply();
        unboundServicesTable.getSortOrder().add(unboundServicesNameColumn);
        unboundServicesTable.sort();
    }

}
