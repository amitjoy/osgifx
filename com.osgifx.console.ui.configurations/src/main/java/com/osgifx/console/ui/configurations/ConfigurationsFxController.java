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
package com.osgifx.console.ui.configurations;

import static com.osgifx.console.event.topics.ConfigurationActionEventTopics.CONFIGURATION_UPDATED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_CONFIGURATION_FILTER_EVENT_TOPIC;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.dto.SearchFilterDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.configurations.converter.ConfigurationManager;
import com.osgifx.console.ui.configurations.dialog.ConfigurationCreateDialog;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

public final class ConfigurationsFxController {

    @Log
    @Inject
    private FluentLogger                 logger;
    @Inject
    @LocalInstance
    private FXMLLoader                   loader;
    @FXML
    private TableView<XConfigurationDTO> table;
    @Inject
    @OSGiBundle
    private BundleContext                context;
    @Inject
    @Named("is_connected")
    private boolean                      isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean                      isSnapshotAgent;
    @Inject
    private DataProvider                 dataProvider;
    @Inject
    private IEclipseContext              eclipseContext;
    @Inject
    private Executor                     executor;
    @Inject
    private ThreadSynchronize            threadSync;
    @Inject
    private IEventBroker                 eventBroker;
    @Inject
    private ConfigurationManager         configManager;
    @FXML
    private Button                       addConfigButton;

    private FilteredList<XConfigurationDTO>         filteredList;
    private TableRowDataFeatures<XConfigurationDTO> previouslyExpanded;

    @FXML
    public void initialize() {
        initButtonIcons();
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            updateButtonStates();
            return;
        }
        if (!isCapabilityAvailable("CM")) {
            Fx.addTablePlaceholderWhenFeatureUnavailable(table, "Config Admin");
            updateButtonStates();
            return;
        }
        try {
            createControls();
            Fx.disableSelectionModel(table);
            updateButtonStates();
            logger.atDebug().log("FXML controller has been initialized");
        } catch (final Exception e) {
            logger.atError().withException(e).log("FXML controller could not be initialized");
        }
    }

    private boolean isCapabilityAvailable(final String capabilityId) {
        return dataProvider.runtimeCapabilities().stream().anyMatch(c -> capabilityId.equals(c.id) && c.isAvailable);
    }

    @FXML
    public void addNewConfiguration() {
        final var dialog = new ConfigurationCreateDialog();
        ContextInjectionFactory.inject(dialog, eclipseContext);
        logger.atInfo().log("Injected configuration create dialog to eclipse context");
        dialog.init();

        final var configuration = dialog.showAndWait();
        if (configuration.isPresent()) {
            final Task<Void> createTask = new Task<>() {

                @Override
                protected Void call() throws Exception {
                    try {
                        final var dto        = configuration.get();
                        final var pid        = dto.pid();
                        final var factoryPid = dto.factoryPid();
                        final var properties = dto.properties();

                        if (StringUtils.isBlank(pid) && StringUtils.isBlank(factoryPid) || properties == null) {
                            return null;
                        }

                        final boolean result;
                        final String  effectivePID;

                        if (StringUtils.isNotBlank(pid)) {
                            effectivePID = pid.strip();
                            result       = configManager.createOrUpdateConfiguration(effectivePID, properties);
                        } else if (StringUtils.isNotBlank(factoryPid)) {
                            effectivePID = factoryPid.strip();
                            result       = configManager.createFactoryConfiguration(effectivePID, properties);
                        } else {
                            return null;
                        }

                        if (result) {
                            eventBroker.post(CONFIGURATION_UPDATED_EVENT_TOPIC, effectivePID);
                            threadSync.asyncExec(() -> Fx.showSuccessNotification("New Configuration",
                                    "Configuration - '" + effectivePID + "' has been successfully created"));
                            logger.atInfo().log("Configuration - '%s' has been successfully created", effectivePID);
                        } else {
                            threadSync.asyncExec(() -> Fx.showErrorNotification("New Configuration",
                                    "Configuration - '" + effectivePID + "' could not be created"));
                            logger.atWarning().log("Configuration - '%s' could not be created", effectivePID);
                        }
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Configuration could not be created");
                        threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                    }
                    return null;
                }
            };
            executor.runAsync(createTask);
        }
    }

    private void initButtonIcons() {
        addConfigButton.setGraphic(createIcon("/graphic/icons/new-config.png"));
    }

    private void updateButtonStates() {
        final var disableActions = !isConnected || isSnapshotAgent;
        addConfigButton.setDisable(disableActions);
    }

    private ImageView createIcon(final String path) {
        final var image     = new Image(getClass().getResourceAsStream(path));
        final var imageView = new ImageView(image);
        imageView.setFitHeight(16.0);
        imageView.setFitWidth(16.0);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private void createControls() {
        final var expandedNode   = (BorderPane) Fx.loadFXML(loader, context, "/fxml/expander-column-content.fxml");
        final var controller     = (ConfigurationEditorFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XConfigurationDTO>(current -> {
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

        final var pidColumn = new TableColumn<XConfigurationDTO, String>("PID/Factory PID");
        pidColumn.setCellValueFactory(
                new DTOCellValueFactory<>("pid", String.class,
                                          _ -> "Not created yet but property descriptor available"));
        Fx.addCellFactory(pidColumn, c -> !c.isPersisted, Color.MEDIUMVIOLETRED, Color.BLACK);

        final var nameColumn = new TableColumn<XConfigurationDTO, String>("Name");
        nameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class, s -> java.util.Optional
                .ofNullable(s.ocd).map(v -> v.name).orElse("No property descriptor available")));

        final var locationColumn = new TableColumn<XConfigurationDTO, String>("Location");
        locationColumn
                .setCellValueFactory(new DTOCellValueFactory<>("location", String.class, _ -> "No bound location"));

        final var isFactoryColumn = new TableColumn<XConfigurationDTO, String>("Is Factory?");
        isFactoryColumn.setCellValueFactory(new DTOCellValueFactory<>("isFactory", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(pidColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(locationColumn);
        table.getColumns().add(isFactoryColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        filteredList = new FilteredList<>(dataProvider.configurations());
        threadSync.asyncExec(() -> {
            table.setItems(filteredList);
            TableFilter.forTableView(table).lazy(true).apply();
            table.getSortOrder().add(pidColumn);
            table.sort();
        });
    }

    @Inject
    @Optional
    @SuppressWarnings("unchecked")
    public void onFilterUpdateEvent(@UIEventTopic(UPDATE_CONFIGURATION_FILTER_EVENT_TOPIC) final SearchFilterDTO filter) {
        logger.atInfo().log("Update filter event received");
        filteredList.setPredicate((Predicate<? super XConfigurationDTO>) filter.predicate);
    }

}
