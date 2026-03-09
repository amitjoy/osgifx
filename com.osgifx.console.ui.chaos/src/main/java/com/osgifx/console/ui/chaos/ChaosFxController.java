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
package com.osgifx.console.ui.chaos;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.chaos.engine.ChaosConfig;
import com.osgifx.console.ui.chaos.engine.ChaosEngine;
import com.osgifx.console.ui.chaos.engine.ChaosEvent;
import com.osgifx.console.ui.chaos.engine.TargetSelector;
import com.osgifx.console.ui.chaos.model.ActionLog;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;

public final class ChaosFxController implements Initializable {

    @Log
    @Inject
    private FluentLogger                   logger;
    @Inject
    @Named("is_connected")
    private boolean                        isConnected;
    @Inject
    @Optional
    private DataProvider                   dataProvider;
    @Inject
    @Optional
    private Supervisor                     supervisor;
    @FXML
    private RadioButton                    bundlesRadio;
    @FXML
    private RadioButton                    componentsRadio;
    @FXML
    private RadioButton                    bothRadio;
    @FXML
    private TextField                      inclusionFilter;
    @FXML
    private TextField                      exclusionFilter;
    @FXML
    private CheckBox                       lifecycleToggles;
    @FXML
    private Spinner<Integer>               actionInterval;
    @FXML
    private Spinner<Integer>               downtimeDuration;
    @FXML
    private Spinner<Integer>               concurrency;
    @FXML
    private Spinner<Integer>               autoStopTimer;
    @FXML
    private Button                         previewButton;
    @FXML
    private Button                         unleashButton;
    @FXML
    private Button                         haltButton;
    @FXML
    private ListView<String>               offlineList;
    @FXML
    private TableView<ActionLog>           actionTicker;
    @FXML
    private TableColumn<ActionLog, String> timeColumn;
    @FXML
    private TableColumn<ActionLog, String> iconColumn;
    @FXML
    private TableColumn<ActionLog, String> targetColumn;
    @FXML
    private TableColumn<ActionLog, String> stateColumn;

    private final ChaosEngine               engine         = new ChaosEngine();
    private final ObservableList<ActionLog> logs           = FXCollections.observableArrayList();
    private final ObservableList<String>    offlineTargets = FXCollections.observableArrayList();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        final ToggleGroup group = new ToggleGroup();
        bundlesRadio.setToggleGroup(group);
        componentsRadio.setToggleGroup(group);
        bothRadio.setToggleGroup(group);
        bothRadio.setSelected(true);

        actionInterval.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 60, 10));
        downtimeDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, 5));
        concurrency.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        autoStopTimer.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 60, 10));

        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        iconColumn.setCellValueFactory(new PropertyValueFactory<>("icon"));
        targetColumn.setCellValueFactory(new PropertyValueFactory<>("targetName"));
        stateColumn.setCellValueFactory(new PropertyValueFactory<>("state"));

        iconColumn.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(final String item, final boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 15px; -fx-alignment: center;");
                }
            }
        });

        actionTicker.setItems(logs);
        offlineList.setItems(offlineTargets);

        haltButton.setDisable(true);
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(actionTicker);
            Fx.addListViewPlaceholderWhenDisconnected(offlineList);
            unleashButton.setDisable(true);
            setControlsDisable(true);
        } else {
            actionTicker.setPlaceholder(new Label("No chaos unleashed yet..."));
            offlineList.setPlaceholder(new Label("System is stable."));
        }
        logger.atDebug().log("Chaos FX Controller initialized");
    }

    @FXML
    private void onPreview() {
        if (supervisor == null || dataProvider == null) {
            logger.atError().log("Cannot preview: services not available");
            return;
        }

        final ChaosConfig                 config  = buildConfigFromUI();
        final List<TargetSelector.Victim> victims = TargetSelector.selectVictims(config, dataProvider,
                Collections.emptySet(), false);

        final long bundles    = victims.stream().filter(v -> v.type == ChaosEvent.TargetType.BUNDLE).count();
        final long components = victims.stream().filter(v -> v.type == ChaosEvent.TargetType.COMPONENT).count();

        final String message;
        if (config.targetType == ChaosConfig.TargetType.BUNDLES) {
            message = String.format("Found %d %s ready for chaos.\n(Components excluded - only targeting Bundles)",
                    bundles, bundles == 1 ? "Bundle" : "Bundles");
        } else if (config.targetType == ChaosConfig.TargetType.COMPONENTS) {
            message = String.format("Found %d %s ready for chaos.\n(Bundles excluded - only targeting Components)",
                    components, components == 1 ? "Component" : "Components");
        } else {
            message = String.format("Found %d %s and %d %s ready for chaos.\nTotal: %d targets", bundles,
                    bundles == 1 ? "Bundle" : "Bundles", components, components == 1 ? "Component" : "Components",
                    bundles + components);
        }

        FxDialog.showInfoDialog("Chaos Preview", message, getClass().getClassLoader());
    }

    @FXML
    private void onUnleashChaos() {
        if (supervisor == null || dataProvider == null) {
            logger.atError().log("Cannot unleash chaos: services not available");
            return;
        }

        final ChaosConfig config = buildConfigFromUI();

        unleashButton.setDisable(true);
        haltButton.setDisable(false);
        setControlsDisable(true);

        engine.start(config, supervisor, dataProvider, this::handleLog);
    }

    @FXML
    private void onHaltChaos() {
        engine.stop();
        unleashButton.setDisable(false);
        haltButton.setDisable(true);
        setControlsDisable(false);
    }

    private ChaosConfig buildConfigFromUI() {
        final ChaosConfig config = new ChaosConfig();
        if (bundlesRadio.isSelected())
            config.targetType = ChaosConfig.TargetType.BUNDLES;
        else if (componentsRadio.isSelected())
            config.targetType = ChaosConfig.TargetType.COMPONENTS;
        else
            config.targetType = ChaosConfig.TargetType.BOTH;

        config.inclusionFilter  = inclusionFilter.getText();
        config.exclusionFilter  = exclusionFilter.getText();
        config.actionInterval   = actionInterval.getValue();
        config.downtimeDuration = downtimeDuration.getValue();
        config.concurrency      = concurrency.getValue();
        config.autoStopMinutes  = autoStopTimer.getValue();

        return config;
    }

    private void handleLog(final ActionLog log) {
        logs.add(0, log);
        offlineTargets.setAll(engine.getOfflineTargets().keySet());
    }

    private void setControlsDisable(final boolean disable) {
        bundlesRadio.setDisable(disable);
        componentsRadio.setDisable(disable);
        bothRadio.setDisable(disable);
        inclusionFilter.setDisable(disable);
        exclusionFilter.setDisable(disable);
        actionInterval.setDisable(disable);
        downtimeDuration.setDisable(disable);
        concurrency.setDisable(disable);
        autoStopTimer.setDisable(disable);
    }

    public void emergencyShutdown() {
        if (engine.isRunning()) {
            engine.stop();
        }
    }

    public boolean isRunning() {
        return engine.isRunning();
    }

}
