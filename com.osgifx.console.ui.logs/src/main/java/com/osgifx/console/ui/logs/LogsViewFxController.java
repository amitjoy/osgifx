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
package com.osgifx.console.ui.logs;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

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
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class LogsViewFxController {

    @Log
    @Inject
    private FluentLogger                       logger;
    @Inject
    @LocalInstance
    private FXMLLoader                         loader;
    @FXML
    private TableView<XLogEntryDTO>            table;
    @Inject
    @OSGiBundle
    private BundleContext                      context;
    @Inject
    @Named("is_connected")
    private boolean                            isConnected;
    @Inject
    private DataProvider                       dataProvider;
    private TableRowDataFeatures<XLogEntryDTO> previouslyExpanded;

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
        final var expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content-for-logs.fxml");
        final var controller     = (LogDetailsFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XLogEntryDTO>(current -> {
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

        final var loggedAtColumn = new TableColumn<XLogEntryDTO, Date>("Logged At");

        loggedAtColumn.setPrefWidth(270);
        loggedAtColumn.setCellValueFactory(new DTOCellValueFactory<>("loggedAt", Date.class));

        final var logLevelColumn = new TableColumn<XLogEntryDTO, String>("Level");

        logLevelColumn.setPrefWidth(110);
        logLevelColumn.setCellValueFactory(new DTOCellValueFactory<>("level", String.class));

        final var messageColumn = new TableColumn<XLogEntryDTO, String>("Message");

        messageColumn.setPrefWidth(750);
        messageColumn.setCellValueFactory(new DTOCellValueFactory<>("message", String.class));
        Fx.addCellFactory(messageColumn, c -> "ERROR".equalsIgnoreCase(c.level), Color.MEDIUMVIOLETRED, Color.BLACK);

        table.getColumns().add(expanderColumn);
        table.getColumns().add(loggedAtColumn);
        table.getColumns().add(logLevelColumn);
        table.getColumns().add(messageColumn);

        final var logs = dataProvider.logs();
        table.setItems(logs);

        TableFilter.forTableView(table).lazy(true).apply();
        sortByLoggedAt(loggedAtColumn);
    }

    private void sortByLoggedAt(final TableColumn<XLogEntryDTO, Date> column) {
        column.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(column);
        table.sort();
    }

}
