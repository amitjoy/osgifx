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
package com.osgifx.console.ui.bundles;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.table.TableFilter;
import org.eclipse.fx.core.command.CommandService;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleInfoDTO;
import com.osgifx.console.agent.dto.XPackageDTO;
import com.osgifx.console.agent.dto.XServiceInfoDTO;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;

import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class BundleDetailsFxController {

    private static final String AGENT_BUNDLE_BSN            = "com.osgifx.console.agent";
    private static final String BUNDLE_START_COMMAND_ID     = "com.osgifx.console.application.command.bundle.start";
    private static final String BUNDLE_STOP_COMMAND_ID      = "com.osgifx.console.application.command.bundle.stop";
    private static final String BUNDLE_UNINSTALL_COMMAND_ID = "com.osgifx.console.application.command.bundle.uninstall";

    @FXML
    private Label                                      idLabel;
    @FXML
    private Label                                      bsnLabel;
    @FXML
    private Label                                      stateLabel;
    @FXML
    private Label                                      versionLabel;
    @FXML
    private Label                                      locationLabel;
    @FXML
    private Label                                      categoryLabel;
    @FXML
    private ToggleSwitch                               fragmentLabel;
    @FXML
    private Label                                      lasModifiedLabel;
    @FXML
    private Label                                      docLabel;
    @FXML
    private Label                                      vendorLabel;
    @FXML
    private Label                                      revisionsLabel;
    @FXML
    private ToggleSwitch                               isPersistentlyStartedLabel;
    @FXML
    private ToggleSwitch                               isActivationPolicyUsedLabel;
    @FXML
    private Label                                      descLabel;
    @FXML
    private Label                                      startLevelLabel;
    @FXML
    private Label                                      startDurationLabel;
    @FXML
    private Button                                     startBundleButton;
    @FXML
    private Button                                     stopBundleButton;
    @FXML
    private Button                                     uninstallBundleButton;
    @FXML
    private TableView<XPackageDTO>                     exportedPackagesNameTable;
    @FXML
    private TableColumn<XPackageDTO, String>           exportedPackagesNameTableColumn;
    @FXML
    private TableColumn<XPackageDTO, String>           exportedPackagesVersionTableColumn;
    @FXML
    private TableView<XServiceInfoDTO>                 registeredServicesTable;
    @FXML
    private TableColumn<XServiceInfoDTO, String>       registeredServicesIdTableColumn;
    @FXML
    private TableColumn<XServiceInfoDTO, String>       registeredServicesClassTableColumn;
    @FXML
    private TableView<Entry<String, String>>           manifestHeadersTable;
    @FXML
    private TableColumn<Entry<String, String>, String> manifestHeadersTableColumn1;
    @FXML
    private TableColumn<Entry<String, String>, String> manifestHeadersTableColumn2;
    @FXML
    private TableView<XPackageDTO>                     importedPackagesTable;
    @FXML
    private TableColumn<XPackageDTO, String>           importedPackagesNameTableColumn;
    @FXML
    private TableColumn<XPackageDTO, String>           importedPackagesVersionTableColumn;
    @FXML
    private TableView<XBundleInfoDTO>                  wiredBundlesAsProviderTable;
    @FXML
    private TableColumn<XBundleInfoDTO, String>        wiredBundlesAsProviderIdTableColumn;
    @FXML
    private TableColumn<XBundleInfoDTO, String>        wiredBundlesAsProviderBsnTableColumn;
    @FXML
    private TableView<XBundleInfoDTO>                  wiredBundlesAsRequirerTable;
    @FXML
    private TableColumn<XBundleInfoDTO, String>        wiredBundlesAsRequirerIdTableColumn;
    @FXML
    private TableColumn<XBundleInfoDTO, String>        wiredBundlesAsRequirerBsnTableColumn;
    @FXML
    private TableView<XServiceInfoDTO>                 usedServicesTable;
    @FXML
    private TableColumn<XServiceInfoDTO, String>       usedServicesIdTableColumn;
    @FXML
    private TableColumn<XServiceInfoDTO, String>       usedServicesClassTableColumn;
    @FXML
    private TableView<XBundleInfoDTO>                  hostBundlesTable;
    @FXML
    private TableColumn<XBundleInfoDTO, String>        hostBundlesIdTableColumn;
    @FXML
    private TableColumn<XBundleInfoDTO, String>        hostBundlesBsnTableColumn;
    @FXML
    private TableView<XBundleInfoDTO>                  attachedFragmentsTable;
    @FXML
    private TableColumn<XBundleInfoDTO, String>        attachedFragmentsIdTableColumn;
    @FXML
    private TableColumn<XBundleInfoDTO, String>        attachedFragmentsBsnTableColumn;
    @Log
    @Inject
    private FluentLogger                               logger;
    @Inject
    private CommandService                             commandService;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                                    isSnapshotAgent;
    private Converter                                  converter;

    @FXML
    public void initialize() {
        converter = Converters.standardConverter();
        logger.atDebug().log("FXML controller has been initialized");
    }

    void initControls(final XBundleDTO bundle) {
        idLabel.setText(String.valueOf(bundle.id));
        stateLabel.setText(bundle.state);
        bsnLabel.setText(bundle.symbolicName);
        versionLabel.setText(bundle.version);
        locationLabel.setText(bundle.location);
        categoryLabel.setText(bundle.category);

        initFragment(bundle);
        initIsPersistentlyStarted(bundle);
        initIsActivationPolicyUsed(bundle);
        registerButtonHandlers(bundle);

        lasModifiedLabel.setText(formatLastModified(bundle.lastModified));
        docLabel.setText(bundle.documentation);
        vendorLabel.setText(bundle.vendor);
        revisionsLabel.setText(String.valueOf(bundle.revisions));
        descLabel.setText(bundle.description);
        startLevelLabel.setText(String.valueOf(bundle.startLevel));

        if (bundle.startDurationInMillis != -1) {
            startDurationLabel.setText(String.valueOf(bundle.startDurationInMillis) + " ms");
        } else {
            startDurationLabel.setText("<IGNORED>");
        }

        exportedPackagesNameTableColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        exportedPackagesVersionTableColumn.setCellValueFactory(new DTOCellValueFactory<>("version", String.class));
        exportedPackagesNameTable.setItems(FXCollections.observableArrayList(bundle.exportedPackages));

        importedPackagesNameTableColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));
        importedPackagesVersionTableColumn.setCellValueFactory(new DTOCellValueFactory<>("version", String.class));
        importedPackagesTable.setItems(FXCollections.observableArrayList(bundle.importedPackages));

        wiredBundlesAsProviderIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        wiredBundlesAsProviderBsnTableColumn
                .setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        wiredBundlesAsProviderTable.setItems(FXCollections.observableArrayList(bundle.wiredBundlesAsProvider));

        wiredBundlesAsRequirerIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        wiredBundlesAsRequirerBsnTableColumn
                .setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        wiredBundlesAsRequirerTable.setItems(FXCollections.observableArrayList(bundle.wiredBundlesAsRequirer));

        registeredServicesIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        registeredServicesClassTableColumn.setCellValueFactory(new DTOCellValueFactory<>("objectClass", String.class));
        registeredServicesTable.setItems(FXCollections.observableArrayList(bundle.registeredServices));

        usedServicesIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        usedServicesClassTableColumn.setCellValueFactory(new DTOCellValueFactory<>("objectClass", String.class));
        usedServicesTable.setItems(FXCollections.observableArrayList(bundle.usedServices));

        hostBundlesIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        hostBundlesBsnTableColumn.setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        hostBundlesTable.setItems(FXCollections.observableArrayList(bundle.hostBundles));

        attachedFragmentsIdTableColumn.setCellValueFactory(new DTOCellValueFactory<>("id", String.class));
        attachedFragmentsBsnTableColumn.setCellValueFactory(new DTOCellValueFactory<>("symbolicName", String.class));
        attachedFragmentsTable.setItems(FXCollections.observableArrayList(bundle.fragmentsAttached));

        manifestHeadersTableColumn1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));
        manifestHeadersTableColumn2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getValue()));
        manifestHeadersTable.setItems(FXCollections.observableArrayList(bundle.manifestHeaders.entrySet()));

        applyTableFilters();

        Fx.addContextMenuToCopyContent(exportedPackagesNameTable);
        Fx.addContextMenuToCopyContent(registeredServicesTable);
        Fx.addContextMenuToCopyContent(manifestHeadersTable);
        Fx.addContextMenuToCopyContent(importedPackagesTable);
        Fx.addContextMenuToCopyContent(wiredBundlesAsProviderTable);
        Fx.addContextMenuToCopyContent(wiredBundlesAsRequirerTable);
        Fx.addContextMenuToCopyContent(usedServicesTable);
        Fx.addContextMenuToCopyContent(hostBundlesTable);
        Fx.addContextMenuToCopyContent(attachedFragmentsTable);
    }

    private String formatLastModified(final long lastModified) {
        if (lastModified == 0) {
            return "No last modification";
        }
        return converter.convert(lastModified).to(Date.class).toString();
    }

    private void initFragment(final XBundleDTO bundle) {
        final BooleanProperty isSnapshot        = new SimpleBooleanProperty(isSnapshotAgent);
        final var             isSnapshotBinding = new When(isSnapshot).then(true).otherwise(false);

        final BooleanProperty isFragment        = new SimpleBooleanProperty(bundle.isFragment);
        final var             isFragmentBinding = new When(isFragment).then(true).otherwise(false);

        final BooleanProperty isActive        = new SimpleBooleanProperty("ACTIVE".equals(bundle.state));
        final var             isActiveBinding = new When(isActive).then(true).otherwise(false);

        final BooleanProperty isResolved        = new SimpleBooleanProperty("RESOLVED".equals(bundle.state));
        final var             isResolvedBinding = new When(isResolved).then(true).otherwise(false);

        final BooleanProperty isInstalled        = new SimpleBooleanProperty("INSTALLED".equals(bundle.state));
        final var             isInstalledBinding = new When(isInstalled).then(true).otherwise(false);

        final BooleanProperty isAgent        = new SimpleBooleanProperty(AGENT_BUNDLE_BSN.equals(bundle.symbolicName));
        final var             isAgentBinding = new When(isAgent).then(true).otherwise(false);

        startBundleButton.disableProperty().bind(isSnapshotBinding.or(isFragmentBinding).or(isActiveBinding));
        stopBundleButton.disableProperty()
                .bind(isSnapshotBinding.or(isResolvedBinding).or(isInstalledBinding).or(isAgentBinding));
        uninstallBundleButton.disableProperty().bind(isSnapshotBinding.or(isAgentBinding));

        fragmentLabel.setSelected(bundle.isFragment);
    }

    private void initIsPersistentlyStarted(final XBundleDTO bundle) {
        isPersistentlyStartedLabel.setSelected(bundle.isPersistentlyStarted);
    }

    private void initIsActivationPolicyUsed(final XBundleDTO bundle) {
        isActivationPolicyUsedLabel.setSelected(bundle.isActivationPolicyUsed);
    }

    private void registerButtonHandlers(final XBundleDTO bundle) {
        startBundleButton.setOnAction(a -> {
            logger.atInfo().log("Bundle start request has been sent for %s", bundle.id);
            commandService.execute(BUNDLE_START_COMMAND_ID, createCommandMap(bundle.id));
        });
        stopBundleButton.setOnAction(a -> {
            logger.atInfo().log("Bundle stop request has been sent for %s", bundle.id);
            commandService.execute(BUNDLE_STOP_COMMAND_ID, createCommandMap(bundle.id));
        });
        uninstallBundleButton.setOnAction(a -> {
            logger.atInfo().log("Bundle uninstall request has been sent for %s", bundle.id);
            commandService.execute(BUNDLE_UNINSTALL_COMMAND_ID, createCommandMap(bundle.id));
        });
    }

    private void applyTableFilters() {
        TableFilter.forTableView(exportedPackagesNameTable).apply();
        TableFilter.forTableView(registeredServicesTable).apply();
        TableFilter.forTableView(manifestHeadersTable).apply();
        TableFilter.forTableView(importedPackagesTable).apply();
        TableFilter.forTableView(wiredBundlesAsProviderTable).apply();
        TableFilter.forTableView(wiredBundlesAsRequirerTable).apply();
        TableFilter.forTableView(usedServicesTable).apply();
        TableFilter.forTableView(hostBundlesTable).apply();
        TableFilter.forTableView(attachedFragmentsTable).apply();
    }

    private Map<String, Object> createCommandMap(final long value) {
        return Map.of("id", value);
    }

}
