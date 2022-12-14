/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.ui.events;

import static javafx.scene.control.TableColumn.SortType.DESCENDING;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

public final class EventsFxController {

    @Log
    @Inject
    private FluentLogger                    logger;
    @Inject
    @LocalInstance
    private FXMLLoader                      loader;
    @FXML
    private TableView<XEventDTO>            table;
    @Inject
    @OSGiBundle
    private BundleContext                   context;
    @Inject
    @Named("is_connected")
    private boolean                         isConnected;
    @Inject
    private DataProvider                    dataProvider;
    private TableRowDataFeatures<XEventDTO> previouslyExpanded;

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
        final var controller     = (EventDetailsFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XEventDTO>(current -> {
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

        final var receivedAtColumn = new TableColumn<XEventDTO, Date>("Received At");

        receivedAtColumn.setPrefWidth(290);
        receivedAtColumn.setCellValueFactory(new DTOCellValueFactory<>("received", Date.class));

        final var topicColumn = new TableColumn<XEventDTO, String>("Topic");

        topicColumn.setPrefWidth(650);
        topicColumn.setCellValueFactory(new DTOCellValueFactory<>("topic", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(receivedAtColumn);
        table.getColumns().add(topicColumn);

        final var events = dataProvider.events();
        table.setItems(events);

        TableFilter.forTableView(table).lazy(true).apply();
        Fx.initSortOrder(table, receivedAtColumn, DESCENDING);
    }

}
