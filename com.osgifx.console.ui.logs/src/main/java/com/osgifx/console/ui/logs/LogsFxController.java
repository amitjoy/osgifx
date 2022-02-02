/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class LogsFxController {

    private static final String EVENT_TOPIC = "com/osgifx/clear/logs";

    @Log
    @Inject
    private FluentLogger                       logger;
    @Inject
    @LocalInstance
    private FXMLLoader                         loader;
    @FXML
    private TableView<XLogEntryDTO>            table;
    @Inject
    private DataProvider                       dataProvider;
    @Inject
    @OSGiBundle
    private BundleContext                      context;
    private TableRowDataFeatures<XLogEntryDTO> selectedLog;

    @FXML
    public void initialize() {
        try {
            createControls();
            Fx.disableSelectionModel(table);
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void createControls() {
        final GridPane                             expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final LogDetailsFxController               controller     = loader.getController();
        final TableRowExpanderColumn<XLogEntryDTO> expanderColumn = new TableRowExpanderColumn<>(expandedLog -> {
                                                                      controller.initControls(expandedLog.getValue());
                                                                      if (selectedLog != null && selectedLog.isExpanded()) {
                                                                          selectedLog.toggleExpanded();
                                                                      }
                                                                      selectedLog = expandedLog;
                                                                      return expandedNode;
                                                                  });

        final TableColumn<XLogEntryDTO, Date> loggedAtColumn = new TableColumn<>("Logged At");

        loggedAtColumn.setPrefWidth(270);
        loggedAtColumn.setCellValueFactory(new DTOCellValueFactory<>("loggedAt", Date.class));

        final TableColumn<XLogEntryDTO, String> logLevelColumn = new TableColumn<>("Level");

        logLevelColumn.setPrefWidth(110);
        logLevelColumn.setCellValueFactory(new DTOCellValueFactory<>("level", String.class));

        final TableColumn<XLogEntryDTO, String> messageColumn = new TableColumn<>("Message");

        messageColumn.setPrefWidth(750);
        messageColumn.setCellValueFactory(new DTOCellValueFactory<>("message", String.class));
        Fx.addCellFactory(messageColumn, c -> "ERROR".equalsIgnoreCase(c.level), Color.MEDIUMVIOLETRED, Color.BLACK);

        table.getColumns().add(expanderColumn);
        table.getColumns().add(loggedAtColumn);
        table.getColumns().add(logLevelColumn);
        table.getColumns().add(messageColumn);

        final ObservableList<XLogEntryDTO> logs = dataProvider.logs();
        table.setItems(logs);

        TableFilter.forTableView(table).apply();
        sortByLoggedAt(loggedAtColumn);
    }

    @Inject
    @Optional
    private void clearTableEvent(@UIEventTopic(EVENT_TOPIC) final String data) {
        table.setItems(FXCollections.emptyObservableList());
        final ObservableList<XLogEntryDTO> logs = dataProvider.logs();
        logs.clear();
        table.setItems(logs);
        logger.atInfo().log("Cleared logs table successfully");
    }

    private void sortByLoggedAt(final TableColumn<XLogEntryDTO, Date> column) {
        column.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(column);
        table.sort();
    }

}
