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
package com.osgifx.console.ui.packages;

import javax.inject.Inject;

import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.dto.BundleDTO;

import com.osgifx.console.data.provider.PackageDTO;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class PackageDetailsFxController {

    @FXML
    private Label                          nameLabel;
    @FXML
    private Label                          versionLabel;
    @FXML
    private ToggleSwitch                   duplicateLabel;
    @FXML
    private TableView<BundleDTO>           exportersTable;
    @FXML
    private TableColumn<BundleDTO, String> exportersTableIdColumn;
    @FXML
    private TableColumn<BundleDTO, String> exportersTableBsnColumn;
    @FXML
    private TableView<BundleDTO>           importersTable;
    @FXML
    private TableColumn<BundleDTO, String> importersTableIdColumn;
    @FXML
    private TableColumn<BundleDTO, String> importersTableBsnColumn;
    @Log
    @Inject
    private FluentLogger                   logger;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    void initControls(final PackageDTO pkg) {
        nameLabel.setText(pkg.name);
        versionLabel.setText(pkg.version);
        duplicateLabel.setSelected(pkg.isDuplicateExport);

        exportersTableIdColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        exportersTableBsnColumn.setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        exportersTable.setItems(FXCollections.observableArrayList(pkg.exporters));

        importersTableIdColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        importersTableBsnColumn.setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        importersTable.setItems(FXCollections.observableArrayList(pkg.importers));

        TableFilter.forTableView(exportersTable).apply();
        TableFilter.forTableView(importersTable).apply();

        Fx.addContextMenuToCopyContent(exportersTable);
        Fx.addContextMenuToCopyContent(importersTable);
    }

}
