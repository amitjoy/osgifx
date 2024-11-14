/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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

import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_COMPONENT_FILTER_EVENT_TOPIC;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.google.mu.util.Substring;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.dto.SearchFilterDTO;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

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
    private FilteredList<XComponentDTO>         filteredList;
    private TableRowDataFeatures<XComponentDTO> previouslyExpanded;

    @FXML
    public void initialize() {
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            return;
        }
        try {
            createControls();
            Fx.disableSelectionModel(table);
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
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

        final var idColumn = new TableColumn<XComponentDTO, Integer>("ID");

        idColumn.setPrefWidth(90);
        idColumn.setCellValueFactory(new DTOCellValueFactory<>("id", Integer.class));

        final var componentNameColumn = new TableColumn<XComponentDTO, String>("Name");

        componentNameColumn.setPrefWidth(800);
        componentNameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final var stateColumn = new TableColumn<XComponentDTO, String>("State");

        stateColumn.setPrefWidth(200);
        stateColumn.setCellValueFactory(new DTOCellValueFactory<>("state", String.class));

        final var conditionIdColumn = new TableColumn<XComponentDTO, String>("Condition ID");

        conditionIdColumn.setPrefWidth(180);
        conditionIdColumn.setCellValueFactory(this::parseConditionId);

        table.getColumns().add(expanderColumn);
        table.getColumns().add(idColumn);
        table.getColumns().add(componentNameColumn);
        table.getColumns().add(stateColumn);
        table.getColumns().add(conditionIdColumn);

        filteredList = new FilteredList<>(dataProvider.components());
        table.setItems(filteredList);

        TableFilter.forTableView(table).lazy(true).apply();
    }

    @Inject
    @Optional
    @SuppressWarnings("unchecked")
    public void onFilterUpdateEvent(@UIEventTopic(UPDATE_COMPONENT_FILTER_EVENT_TOPIC) final SearchFilterDTO filter) {
        logger.atInfo().log("Update filter event received");
        filteredList.setPredicate((Predicate<? super XComponentDTO>) filter.predicate);
    }

    private ReadOnlyStringProperty parseConditionId(final CellDataFeatures<XComponentDTO, String> p) {
        final var property = p.getValue().properties.get("osgi.ds.satisfying.condition.target");
        final var value    = new SimpleStringProperty();
        if (property != null) {
            // @formatter:off
            Substring.between("(", ")")
                     .from(property)
                     .ifPresent(e -> Substring.first("=")
                                              .splitThenTrim(e)
                                              .ifPresent((n, k) -> value.set(k)));
            // @formatter:on
        }
        return value;
    }

}
