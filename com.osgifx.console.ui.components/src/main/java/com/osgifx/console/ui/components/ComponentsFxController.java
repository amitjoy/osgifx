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

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_CAPABILITIES_TOPIC;
import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_COMPONENT_FILTER_EVENT_TOPIC;
import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_CONDITION_FILTER_EVENT_TOPIC;
import static org.eclipse.e4.ui.workbench.modeling.EPartService.PartState.ACTIVATE;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConditionDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.dto.SearchFilterDTO;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public final class ComponentsFxController {

    @Log
    @Inject
    private FluentLogger                        logger;
    @Inject
    @LocalInstance
    private FXMLLoader                          loader;
    @FXML
    private TableView<XComponentDTO>            table;
    @Inject
    @OSGiBundle
    private BundleContext                       context;
    @Inject
    @Named("is_connected")
    private boolean                             isConnected;
    @Inject
    private DataProvider                        dataProvider;
    @Inject
    @Optional
    private EPartService                        partService;
    @Inject
    private IEventBroker                        eventBroker;
    @Inject
    private ThreadSynchronize                   threadSync;
    private FilteredList<XComponentDTO>         filteredList;
    private TableRowDataFeatures<XComponentDTO> previouslyExpanded;
    private boolean                             isInitialized;

    @FXML
    public void initialize() {
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            return;
        }
        if (!isCapabilityAvailable("SCR")) {
            Fx.addTablePlaceholderWhenFeatureUnavailable(table, "Declarative Services");
            return;
        }
        try {
            if (!isInitialized) {
                createControls();
                Fx.disableSelectionModel(table);
                isInitialized = true;
            }
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    private boolean isCapabilityAvailable(final String capabilityId) {
        return dataProvider.runtimeCapabilities().stream().anyMatch(c -> capabilityId.equals(c.id) && c.isAvailable);
    }

    private void createControls() {
        final var expandedNode   = (GridPane) Fx.loadFXML(loader, context, "/fxml/expander-column-content.fxml");
        final var controller     = (ComponentDetailsFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XComponentDTO>(current -> {
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

        final var idColumn = new TableColumn<XComponentDTO, Integer>("ID");

        idColumn.setCellValueFactory(new DTOCellValueFactory<>("id", Integer.class));

        final var componentNameColumn = new TableColumn<XComponentDTO, String>("Name");

        componentNameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final var stateColumn = new TableColumn<XComponentDTO, String>("State");

        stateColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));

        final var satisfyingConditionColumn = new TableColumn<XComponentDTO, String>("Satisfying Condition");

        satisfyingConditionColumn.setCellValueFactory(this::getSatisfyingConditionTarget);
        satisfyingConditionColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty() || "(osgi.condition.id=true)".equals(item)) {
                    setText(null);
                    setGraphic(null);
                } else {
                    final var filters = item.split(", ");
                    final var box     = new HBox();
                    for (int i = 0; i < filters.length; i++) {
                        final var conditionFilter = filters[i];
                        final var link            = new Hyperlink(conditionFilter);
                        link.setPadding(Insets.EMPTY);
                        link.setBorder(Border.EMPTY);
                        link.setOnAction(_ -> {
                            final var filter = new SearchFilterDTO();
                            filter.description = "Condition Filter: " + conditionFilter;
                            filter.predicate   = (Predicate<XConditionDTO>) c -> conditionFilter.equals(c.identifier);

                            partService.showPart("com.osgifx.console.application.tab.conditions", ACTIVATE);
                            eventBroker.post(UPDATE_CONDITION_FILTER_EVENT_TOPIC, filter);
                        });
                        box.getChildren().add(link);
                        if (i < filters.length - 1) {
                            box.getChildren().add(new javafx.scene.control.Label(", "));
                        }
                    }
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        table.getColumns().add(expanderColumn);
        table.getColumns().add(idColumn);
        table.getColumns().add(componentNameColumn);
        table.getColumns().add(stateColumn);
        table.getColumns().add(satisfyingConditionColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        filteredList = new FilteredList<>(dataProvider.components());
        threadSync.asyncExec(() -> {
            table.setItems(filteredList);
            TableFilter.forTableView(table).lazy(true).apply();
            table.getSortOrder().add(componentNameColumn);
            table.sort();
        });
    }

    @Inject
    @Optional
    @SuppressWarnings("unchecked")
    public void onFilterUpdateEvent(@UIEventTopic(UPDATE_COMPONENT_FILTER_EVENT_TOPIC) final SearchFilterDTO filter) {
        logger.atInfo().log("Update filter event received");
        filteredList.setPredicate((Predicate<? super XComponentDTO>) filter.predicate);
    }

    private ReadOnlyStringProperty getSatisfyingConditionTarget(final CellDataFeatures<XComponentDTO, String> p) {
        final var component = p.getValue();
        if (component.satisfyingConditionTargets == null || component.satisfyingConditionTargets.isEmpty()) {
            return new SimpleStringProperty("");
        }
        final String joined = component.satisfyingConditionTargets.stream()
                .filter(t -> !t.isEmpty() && !"(osgi.condition.id=true)".equals(t)).collect(Collectors.joining(", "));
        return new SimpleStringProperty(joined);
    }

    @Inject
    @Optional
    private void updateOnDataRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_CAPABILITIES_TOPIC) final String data) {
        if (table == null) {
            return;
        }
        threadSync.asyncExec(this::initialize);
    }

}
