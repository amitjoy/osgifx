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
package com.osgifx.console.ui.conditions;

import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_CONDITION_FILTER_EVENT_TOPIC;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XConditionDTO;
import com.osgifx.console.agent.dto.XConditionState;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.dto.SearchFilterDTO;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.NullTableViewSelectionModel;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

public final class ConditionsFxController {

    @Log
    @Inject
    private FluentLogger                        logger;
    @Inject
    @LocalInstance
    private FXMLLoader                          loader;
    @FXML
    private TableView<XConditionDTO>            conditionsTable;
    @Inject
    @Named("is_connected")
    private boolean                             isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                             isSnapshot;
    @Inject
    private DataProvider                        dataProvider;
    @Inject
    @OSGiBundle
    private BundleContext                       context;
    @Inject
    private ThreadSynchronize                   threadSync;
    @Inject
    @Optional
    private Supervisor                          supervisor;
    private FilteredList<XConditionDTO>         filteredList;
    private TableRowDataFeatures<XConditionDTO> previouslyExpanded;
    private boolean                             isInitialized;

    @FXML
    public void initialize() {
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(conditionsTable);
            return;
        }
        try {
            if (!isInitialized) {
                createControls();
                isInitialized = true;
            }
            logger.atDebug().log("Conditions FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("Conditions FXML controller could not be initialized");
        }
    }

    @Inject
    @Optional
    @SuppressWarnings("unchecked")
    public void onFilterUpdateEvent(@UIEventTopic(UPDATE_CONDITION_FILTER_EVENT_TOPIC) final SearchFilterDTO filter) {
        logger.atInfo().log("Update filter event received");
        filteredList.setPredicate((Predicate<XConditionDTO>) filter.predicate);
    }

    private void createControls() {
        final var expandedNode   = (GridPane) Fx.loadFXML(loader, context, "/fxml/expander-column-content.fxml");
        final var controller     = (ConditionDetailsFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XConditionDTO>(current -> {
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

        final var identifierColumn = new TableColumn<XConditionDTO, String>("Identifier");
        identifierColumn.setCellValueFactory(new DTOCellValueFactory<>("identifier", String.class));
        Fx.addCellFactory(identifierColumn, b -> b.satisfiedComponents.isEmpty() && b.unsatisfiedComponents.isEmpty(),
                Color.SLATEBLUE, Color.BLACK);

        final var stateColumn = new TableColumn<XConditionDTO, XConditionState>("State");
        stateColumn.setCellValueFactory(new DTOCellValueFactory<>("state", XConditionState.class));
        Fx.addCellFactory(stateColumn, s -> s.state == XConditionState.MOCKED, Color.ORANGE, Color.BLACK);

        final var providerBundleIdColumn = new TableColumn<XConditionDTO, String>("Provider Bundle ID");
        providerBundleIdColumn.setCellValueFactory(p -> {
            final var id = p.getValue().providerBundleId;
            return new SimpleStringProperty(id == -1 ? "" : String.valueOf(id));
        });

        final var providerBundleSymbolicNameColumn = new TableColumn<XConditionDTO, String>("Provider Bundle Symbolic Name");
        providerBundleSymbolicNameColumn.setCellValueFactory(p -> {
            final var id = p.getValue().providerBundleId;
            if (id == -1) {
                return new SimpleStringProperty("");
            }
            final var bundle = dataProvider.bundles().stream().filter(b -> b.id == id).findFirst()
                    .map(b -> b.symbolicName).orElse("");
            return new SimpleStringProperty(bundle);
        });

        conditionsTable.getColumns().add(expanderColumn);
        conditionsTable.getColumns().add(identifierColumn);
        conditionsTable.getColumns().add(stateColumn);
        conditionsTable.getColumns().add(providerBundleIdColumn);
        conditionsTable.getColumns().add(providerBundleSymbolicNameColumn);

        conditionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        conditionsTable.setSelectionModel(new NullTableViewSelectionModel<>(conditionsTable));

        filteredList = new FilteredList<>(dataProvider.conditions());
        threadSync.asyncExec(() -> {
            conditionsTable.setItems(filteredList);
            TableFilter.forTableView(conditionsTable).lazy(true).apply();
            conditionsTable.getSortOrder().add(identifierColumn);
            conditionsTable.sort();
        });
    }

}
