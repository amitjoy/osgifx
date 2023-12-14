/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class LogConfigurationsFxController {

    @Log
    @Inject
    private FluentLogger                                  logger;
    @Inject
    @LocalInstance
    private FXMLLoader                                    loader;
    @FXML
    private TableView<XBundleLoggerContextDTO>            table;
    @Inject
    @OSGiBundle
    private BundleContext                                 context;
    @Inject
    @Named("is_connected")
    private boolean                                       isConnected;
    @Inject
    private DataProvider                                  dataProvider;
    private TableRowDataFeatures<XBundleLoggerContextDTO> previouslyExpanded;

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
        final var expandedNode   = Fx.loadFXML(loader, context,
                "/fxml/expander-column-content-for-log-configurations.fxml");
        final var controller     = (LogConfigurationEditorFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XBundleLoggerContextDTO>(current -> {
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

        final var nameColumn = new TableColumn<XBundleLoggerContextDTO, String>("Logger Context Name");

        nameColumn.setPrefWidth(650);
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final var hasCustomLogLevelsColumn = new TableColumn<XBundleLoggerContextDTO, Boolean>("Has Custom Log Levels?");

        hasCustomLogLevelsColumn.setPrefWidth(250);
        hasCustomLogLevelsColumn.setCellValueFactory(c -> new SimpleBooleanProperty(!c.getValue().logLevels.isEmpty()));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(hasCustomLogLevelsColumn);

        final var loggerContexts = dataProvider.loggerContexts();
        table.setItems(loggerContexts);

        TableFilter.forTableView(table).lazy(true).apply();
    }

}
