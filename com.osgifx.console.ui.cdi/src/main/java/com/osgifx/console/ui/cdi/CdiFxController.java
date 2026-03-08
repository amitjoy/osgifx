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
package com.osgifx.console.ui.cdi;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_CAPABILITIES_TOPIC;

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

import com.osgifx.console.agent.dto.XCdiContainerDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class CdiFxController {

    @Log
    @Inject
    private FluentLogger                           logger;
    @Inject
    @LocalInstance
    private FXMLLoader                             loader;
    @FXML
    private TableView<XCdiContainerDTO>            table;
    @Inject
    @OSGiBundle
    private BundleContext                          context;
    @Inject
    @Named("is_connected")
    private boolean                                isConnected;
    @Inject
    private DataProvider                           dataProvider;
    @Inject
    private ThreadSynchronize                      threadSync;
    private TableRowDataFeatures<XCdiContainerDTO> previouslyExpanded;
    private boolean                                isInitialized;

    @FXML
    public void initialize() {
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            return;
        }
        if (!isCapabilityAvailable("CDI")) {
            Fx.addTablePlaceholderWhenFeatureUnavailable(table, "CDI");
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
        final var expandedNode   = (TabPane) Fx.loadFXML(loader, context, "/fxml/expander-column-content.fxml");
        final var controller     = (CdiDetailsFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XCdiContainerDTO>(current -> {
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

        final var idColumn = new TableColumn<XCdiContainerDTO, String>("ID");
        idColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));

        final var bundleIdColumn = new TableColumn<XCdiContainerDTO, String>("Bundle ID");
        bundleIdColumn.setCellValueFactory(
                new DTOCellValueFactory<>("bundleId", String.class, s -> String.valueOf(s.bundleId)));

        final var componentsCountColumn = new TableColumn<XCdiContainerDTO, String>("Components");
        componentsCountColumn.setCellValueFactory(
                new DTOCellValueFactory<>("components", String.class,
                                          s -> String.valueOf(s.components == null ? 0 : s.components.size())));

        final var errorsColumn = new TableColumn<XCdiContainerDTO, String>("Errors");
        errorsColumn.setCellValueFactory(
                new DTOCellValueFactory<>("errors", String.class, s -> (s.errors == null || s.errors.isEmpty()) ? "None"
                        : String.valueOf(s.errors.size())));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(idColumn);
        table.getColumns().add(bundleIdColumn);
        table.getColumns().add(componentsCountColumn);
        table.getColumns().add(errorsColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        threadSync.asyncExec(() -> {
            table.setItems(dataProvider.cdiContainers());
            TableFilter.forTableView(table).lazy(true).apply();
            threadSync.asyncExec(() -> {
                table.getSortOrder().add(idColumn);
                table.sort();
            });
        });
    }

    @Inject
    @Optional
    private void updateOnCapabilitiesRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_CAPABILITIES_TOPIC) final String data) {
        threadSync.asyncExec(this::initialize);
    }

}
