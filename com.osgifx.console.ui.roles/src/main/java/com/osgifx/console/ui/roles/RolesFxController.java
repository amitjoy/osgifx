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
package com.osgifx.console.ui.roles;

import static com.osgifx.console.event.topics.RoleActionEventTopics.ROLE_CREATED_EVENT_TOPIC;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.controlsfx.control.table.TableRowExpanderColumn.TableRowDataFeatures;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.roles.dialog.RoleCreateDialog;
import com.osgifx.console.util.fx.DTOCellValueFactory;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

public final class RolesFxController {

    @Log
    @Inject
    private FluentLogger        logger;
    @Inject
    @LocalInstance
    private FXMLLoader          loader;
    @FXML
    private TableView<XRoleDTO> table;
    @Inject
    @OSGiBundle
    private BundleContext       context;
    @Inject
    @Named("is_connected")
    private boolean             isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean             isSnapshotAgent;
    @Inject
    private DataProvider        dataProvider;
    @Inject
    private IEclipseContext     eclipseContext;
    @Inject
    private Executor            executor;
    @Inject
    @Optional
    private Supervisor          supervisor;
    @Inject
    private ThreadSynchronize   threadSync;
    @Inject
    private IEventBroker        eventBroker;
    @FXML
    private Button              addRoleButton;

    private TableRowDataFeatures<XRoleDTO> previouslyExpanded;
    private boolean                        isInitialized;

    @FXML
    public void initialize() {
        initButtonIcons();
        if (!isConnected) {
            Fx.addTablePlaceholderWhenDisconnected(table);
            updateButtonStates();
            return;
        }
        if (!isCapabilityAvailable("USER_ADMIN")) {
            Fx.addTablePlaceholderWhenFeatureUnavailable(table, "User Admin");
            updateButtonStates();
            return;
        }
        try {
            if (!isInitialized) {
                createControls();
                Fx.disableSelectionModel(table);
                isInitialized = true;
            }
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
    public void addNewRole() {
        final var dialog = new RoleCreateDialog();
        ContextInjectionFactory.inject(dialog, eclipseContext);
        logger.atInfo().log("Injected role create dialog to eclipse context");
        dialog.init();

        final var role = dialog.showAndWait();
        if (role.isPresent()) {
            final Task<Void> createTask = new Task<>() {

                @Override
                protected Void call() throws Exception {
                    try {
                        final var dto      = role.get();
                        final var name     = dto.name();
                        final var roleType = dto.type();

                        final var agent = supervisor.getAgent();
                        if (agent == null) {
                            logger.atInfo().log("Agent not connected");
                            return null;
                        }

                        final var response = agent.createRole(name, roleType);

                        if (response.result == XResultDTO.SUCCESS) {
                            eventBroker.post(ROLE_CREATED_EVENT_TOPIC, name);
                            threadSync.asyncExec(() -> Fx.showSuccessNotification("New Role",
                                    "Role - '" + name + "' has been successfully created"));
                            logger.atInfo().log("Role - '%s' has been successfully created", name);
                        } else {
                            threadSync.asyncExec(() -> Fx.showErrorNotification("New Role", response.response));
                            logger.atError().log("Role - '%s' cannot be created", response.response);
                        }
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Role cannot be created");
                        threadSync.asyncExec(() -> FxDialog.showExceptionDialog(e, getClass().getClassLoader()));
                    }
                    return null;
                }
            };
            executor.runAsync(createTask);
        }
    }

    private void initButtonIcons() {
        addRoleButton.setGraphic(createIcon("/graphic/icons/new-role.png"));
    }

    private void updateButtonStates() {
        final var disableActions = !isConnected || isSnapshotAgent;
        addRoleButton.setDisable(disableActions);
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
        final var controller     = (RoleEditorFxController) loader.getController();
        final var expanderColumn = new TableRowExpanderColumn<XRoleDTO>(current -> {
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

        final var roleNameColumn = new TableColumn<XRoleDTO, String>("Name");
        roleNameColumn.setCellValueFactory(new DTOCellValueFactory<>("name", String.class));

        final var roleTypeColumn = new TableColumn<XRoleDTO, String>("Type");
        roleTypeColumn.setCellValueFactory(new DTOCellValueFactory<>("type", String.class));

        table.getColumns().add(expanderColumn);
        table.getColumns().add(roleNameColumn);
        table.getColumns().add(roleTypeColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        threadSync.asyncExec(() -> {
            table.setItems(dataProvider.roles());
            TableFilter.forTableView(table).lazy(true).apply();
            table.getSortOrder().add(roleNameColumn);
            table.sort();
        });
    }

}
