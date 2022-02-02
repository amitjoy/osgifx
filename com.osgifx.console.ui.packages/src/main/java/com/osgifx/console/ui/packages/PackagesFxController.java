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
package com.osgifx.console.ui.packages;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.data.provider.DataProvider)")
public final class PackagesFxController {

    @Log
    @Inject
    private FluentLogger                     logger;
    @Inject
    @LocalInstance
    private FXMLLoader                       loader;
    @FXML
    private TableView<PackageDTO>            table;
    @Inject
    private DataProvider                     dataProvider;
    @Inject
    @OSGiBundle
    private BundleContext                    context;
    private TableRowDataFeatures<PackageDTO> selectedPackage;

    @FXML
    public void initialize() {
        createControls();
        Fx.disableSelectionModel(table);
        logger.atDebug().log("FXML controller has been initialized");
    }

    private void createControls() {
        final GridPane                           expandedNode   = (GridPane) Fx.loadFXML(loader, context,
                "/fxml/expander-column-content.fxml");
        final PackageDetailsFxController         controller     = loader.getController();
        final TableRowExpanderColumn<PackageDTO> expanderColumn = new TableRowExpanderColumn<>(expandedPackage -> {
                                                                    controller.initControls(expandedPackage.getValue());
                                                                    if (selectedPackage != null && selectedPackage.isExpanded()) {
                                                                        selectedPackage.toggleExpanded();
                                                                    }
                                                                    selectedPackage = expandedPackage;
                                                                    return expandedNode;
                                                                });

        final TableColumn<PackageDTO, String> nameColumn = new TableColumn<>("Name");

        nameColumn.setPrefWidth(450);
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final TableColumn<PackageDTO, String> versionColumn = new TableColumn<>("Version");

        versionColumn.setPrefWidth(450);
        versionColumn.setCellValueFactory(new DTOCellValueFactory<>("version", String.class));

        final TableColumn<PackageDTO, String> hasDuplicatesColumn = new TableColumn<>("Is Duplicate Export?");

        hasDuplicatesColumn.setPrefWidth(200);
        hasDuplicatesColumn.setCellValueFactory(new DTOCellValueFactory<>("isDuplicateExport", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(versionColumn);
        table.getColumns().add(hasDuplicatesColumn);

        final ObservableList<XBundleDTO> bundles = dataProvider.bundles();
        table.setItems(PackageHelper.prepareList(bundles, context));
        Fx.sortBy(table, nameColumn);

        TableFilter.forTableView(table).apply();
    }

}
