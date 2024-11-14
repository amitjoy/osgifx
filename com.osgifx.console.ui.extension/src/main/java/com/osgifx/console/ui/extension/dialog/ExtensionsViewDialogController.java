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
package com.osgifx.console.ui.extension.dialog;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;

import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.FxDialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class ExtensionsViewDialogController {

    @Log
    @Inject
    private FluentLogger                              logger;
    @FXML
    private TableView<DeploymentPackageDTO>           extensionsList;
    @FXML
    private TableColumn<DeploymentPackageDTO, String> nameColumn;
    @FXML
    private TableColumn<DeploymentPackageDTO, String> displayNameColumn;
    @FXML
    private TableColumn<DeploymentPackageDTO, String> versionColumn;
    @Inject
    private DeploymentAdmin                           deploymentAdmin;
    @Inject
    private IWorkbench                                workbench;
    @Inject
    private ThreadSynchronize                         threadSync;
    private ObservableList<DeploymentPackageDTO>      extensions;

    @FXML
    public void initialize() {
        final var installedExtensions = deploymentAdmin.listDeploymentPackages();
        extensions = FXCollections.observableArrayList(Stream.of(installedExtensions).map(this::toDTO).toList());
        extensionsList.setItems(extensions);
        logger.atDebug().log("FXML controller has been initialized");

        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        displayNameColumn.setCellValueFactory(new DTOCellValueFactory<>("displayName", String.class));
        versionColumn.setCellValueFactory(new DTOCellValueFactory<>("version", String.class));

        initContextMenu();
    }

    private void initContextMenu() {
        final var item = new MenuItem("Uninstall");
        item.setOnAction(event -> {
            final var dp = extensionsList.getSelectionModel().getSelectedItem();
            try {
                final var isRemoved = removeDeploymentPackage(dp);
                if (isRemoved) {
                    threadSync.asyncExec(() -> {
                        final var header = "Extension Uninstallation";
                        FxDialog.showInfoDialog(header, dp.name + " has been uninstalled", getClass().getClassLoader());
                        extensions.clear();

                        final var existingExtensions = deploymentAdmin.listDeploymentPackages();
                        extensions.addAll(Stream.of(existingExtensions).map(this::toDTO).toList());

                        // show information about the restart of the application
                        FxDialog.showInfoDialog(header,
                                "The application must be restarted, therefore, will be shut down right away",
                                getClass().getClassLoader(), btn -> workbench.restart());
                    });
                }
            } catch (final Exception e) {
                threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
            }
        });
        final var menu = new ContextMenu();
        menu.getItems().add(item);
        extensionsList.setContextMenu(menu);
    }

    private DeploymentPackageDTO toDTO(final DeploymentPackage deploymentPackage) {
        final var dto = new DeploymentPackageDTO();

        dto.name        = deploymentPackage.getName();
        dto.displayName = deploymentPackage.getDisplayName();
        dto.version     = deploymentPackage.getVersion().toString();

        return dto;
    }

    private boolean removeDeploymentPackage(final DeploymentPackageDTO dp) throws Exception {
        for (final DeploymentPackage deploymentPackage : deploymentAdmin.listDeploymentPackages()) {
            final var name    = deploymentPackage.getName();
            final var version = deploymentPackage.getVersion().toString();
            if (name.equals(dp.name) && version.equals(dp.version)) {
                deploymentPackage.uninstall();
                return true;
            }
        }
        return false;
    }

}
