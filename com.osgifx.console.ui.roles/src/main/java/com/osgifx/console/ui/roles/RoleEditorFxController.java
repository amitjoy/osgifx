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
package com.osgifx.console.ui.roles;

import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.event.topics.RoleActionEventTopics.ROLE_DELETED_EVENT_TOPIC;
import static com.osgifx.console.event.topics.RoleActionEventTopics.ROLE_UPDATED_EVENT_TOPIC;
import static com.osgifx.console.ui.roles.dialog.PropertiesConfigurationDialog.ConfigurationType.CREDENTIALS;
import static com.osgifx.console.ui.roles.dialog.PropertiesConfigurationDialog.ConfigurationType.PROPERTIES;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.internal.events.EventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.dlsc.formsfx.model.structure.DataField;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.MultiSelectionField;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.view.controls.SimpleCheckBoxControl;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XRoleDTO.Type;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.roles.helper.RolesConfigTextControl;
import com.osgifx.console.ui.roles.helper.RolesHelper;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class RoleEditorFxController {

    private static final String KV_DESCRIPTION        = "key=value pairs separated by line breaks";
    private static final String KV_VALIDATION_MESSAGE = "key-value pairs cannot be validated";

    @Log
    @Inject
    private FluentLogger logger;
    @Inject
    private DataProvider dataProvider;
    @FXML
    private BorderPane   rootPanel;
    @Inject
    @Optional
    private Supervisor   supervisor;
    @Inject
    private EventBroker  eventBroker;
    @FXML
    private Button       cancelButton;
    @FXML
    private Button       saveRoleButton;
    @FXML
    private Button       deleteRoleButton;
    @Inject
    @Named("is_snapshot_agent")
    private boolean      isSnapshotAgent;
    private Form         form;
    private FormRenderer formRenderer;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    void initControls(final XRoleDTO role) {
        if (formRenderer != null) {
            rootPanel.getChildren().remove(formRenderer);
        }
        formRenderer = createForm(role);
        initButtons(role);
        rootPanel.setCenter(formRenderer);
    }

    private void initButtons(final XRoleDTO role) {
        final var roleName = role.name;
        deleteRoleButton.setOnAction(event -> {
            logger.atInfo().log("Role deletion request has been sent for role '%s'", roleName);
            deleteRole(roleName);
        });
        saveRoleButton.setOnAction(event -> {
            logger.atInfo().log("Role updation request has been sent for role '%s'", roleName);
            updateRole(role);
        });
        cancelButton.setOnAction(e -> form.reset());
        cancelButton.disableProperty().bind(form.changedProperty().not());
        saveRoleButton.disableProperty().bind(form.changedProperty().not().or(form.validProperty().not()));
    }

    private void deleteRole(final String roleName) {
        final var result = supervisor.getAgent().removeRole(roleName);
        if (result.result == SUCCESS) {
            logger.atInfo().log(result.response);
            eventBroker.post(ROLE_DELETED_EVENT_TOPIC, roleName);
            Fx.showSuccessNotification("Role Deletion", "Role has been deleted successfully");
        } else if (result.result == SKIPPED) {
            logger.atWarning().log(result.response);
            FxDialog.showWarningDialog("Role Deletion", result.response, getClass().getClassLoader());
        } else {
            logger.atError().log(result.response);
            FxDialog.showErrorDialog("Role Deletion", result.response, getClass().getClassLoader());
        }
    }

    private void updateRole(final XRoleDTO role) {
        final var newRole = initNewRole(role);
        final var result  = supervisor.getAgent().updateRole(newRole);

        if (result.result == SUCCESS) {
            logger.atInfo().log(result.response);
            eventBroker.post(ROLE_UPDATED_EVENT_TOPIC, role);
            Fx.showSuccessNotification("Role Updation", "Role has been updated successfully");
        } else if (result.result == SKIPPED) {
            logger.atWarning().log(result.response);
            FxDialog.showWarningDialog("Role Updation", result.response, getClass().getClassLoader());
        } else {
            logger.atError().log(result.response);
            FxDialog.showErrorDialog("Role Updation", result.response, getClass().getClassLoader());
        }
    }

    private FormRenderer createForm(final XRoleDTO role) {
        // @formatter:off
        form = Form.of(
                      Section.of(initGenericFields(role).toArray(new Field[0]))
                             .title("Generic Configuration"),
                      Section.of(initFields(role).toArray(new Field[0]))
                             .title("Specific Configuration"))
                   .title("Role Configuration");
        // @formatter:on
        final var renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        return renderer;
    }

    private List<Field<?>> initGenericFields(final XRoleDTO role) {
        // @formatter:off
        final Field<?> roleNameField = Field.ofStringType(role.name)
                                            .label("Name")
                                            .editable(false);

        final Field<?> roleTypeField = Field.ofStringType(role.type.name())
                                            .label("Type")
                                            .editable(false);
        // @formatter:on
        return List.of(roleNameField, roleTypeField);
    }

    private List<Field<?>> initFields(final XRoleDTO role) {
        final var properties  = role.properties;
        final var credentials = role.credentials;

        final var props = properties == null ? Map.of() : properties;
        final var creds = credentials == null ? Map.of() : credentials;

        // @formatter:off
        final Field<?> propertiesField =
                Field.ofStringType(RolesHelper.mapToString(props))
                     .multiline(true)
                     .label("Properties")
                     .render(new RolesConfigTextControl(PROPERTIES))
                     .valueDescription(KV_DESCRIPTION)
                     .validate(CustomValidator.forPredicate(this::validateKeyValuePairs, KV_VALIDATION_MESSAGE))
                     .editable(!isSnapshotAgent);

        final Field<?> credentialsField =
                Field.ofStringType(RolesHelper.mapToString(creds))
                     .multiline(true)
                     .label("Credentials")
                     .render(new RolesConfigTextControl(CREDENTIALS))
                     .valueDescription(KV_DESCRIPTION)
                     .validate(CustomValidator.forPredicate(this::validateKeyValuePairs, KV_VALIDATION_MESSAGE))
                     .editable(!isSnapshotAgent);

        if (role.type == Type.GROUP) {
            final var allExistingRoles = getAllExistingRoles(role);

            final Field<?> basicMembersField =
                    Field.ofMultiSelectionType(allExistingRoles, getSelections(allExistingRoles, role.basicMembers))
                         .render(new SimpleCheckBoxControl<>())
                         .label("Basic Members")
                         .editable(!isSnapshotAgent);

            final Field<?> requiredMembersField =
                    Field.ofMultiSelectionType(allExistingRoles, getSelections(allExistingRoles, role.requiredMembers))
                         .render(new SimpleCheckBoxControl<>())
                         .label("Required Members")
                         .editable(!isSnapshotAgent);

            return List.of(propertiesField, credentialsField, basicMembersField, requiredMembersField);
        }
        // @formatter:on
        return List.of(propertiesField, credentialsField);
    }

    private boolean validateKeyValuePairs(final String value) {
        try {
            if (value.isBlank()) {
                return true;
            }
            RolesHelper.prepareKeyValuePairs(value);
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    private XRoleDTO initNewRole(final XRoleDTO role) {
        final var newRole = new XRoleDTO();

        newRole.name            = role.name;
        newRole.type            = role.type;
        newRole.properties      = initProperties();
        newRole.credentials     = initCredentials();
        newRole.basicMembers    = initBasicMembers(role);
        newRole.requiredMembers = initRequiredMembers(role);

        return newRole;
    }

    private Map<String, Object> initProperties() {
        final Field<?> field = form.getFields().get(2);
        return RolesHelper.prepareKeyValuePairs(((DataField<?, ?, ?>) field).getValue());
    }

    private Map<String, Object> initCredentials() {
        final Field<?> field = form.getFields().get(3);
        return RolesHelper.prepareKeyValuePairs(((DataField<?, ?, ?>) field).getValue());
    }

    private List<XRoleDTO> initBasicMembers(final XRoleDTO role) {
        final var field = role.type == Type.GROUP ? form.getFields().get(4) : null;
        if (field == null) {
            return null;
        }
        return prepareMembers(((MultiSelectionField<?>) field).getSelection());
    }

    private List<XRoleDTO> initRequiredMembers(final XRoleDTO role) {
        final var field = role.type == Type.GROUP ? form.getFields().get(5) : null;
        if (field == null) {
            return null;
        }
        return prepareMembers(((MultiSelectionField<?>) field).getSelection());
    }

    private List<XRoleDTO> prepareMembers(final List<? extends Object> selections) {
        return selections.stream().map(Object::toString).map(this::getRoleByName).toList();
    }

    private XRoleDTO getRoleByName(final String name) {
        final var allRoles = dataProvider.roles();
        return allRoles.stream().filter(r -> r.name.equals(name)).findFirst().orElse(null);
    }

    private List<String> getAllExistingRoles(final XRoleDTO role) {
        final var allRoles = dataProvider.roles();
        // a role cannot add itself as its member
        // @formatter:off
        return allRoles.stream()
                       .filter(r -> !r.name.equals(role.name))
                       .map(r -> r.name)
                       .toList();
        // @formatter:on
    }

    private List<Integer> getSelections(final List<String> shownMembers, final List<XRoleDTO> configuredMembers) {
        if (configuredMembers == null) {
            return List.of();
        }
        // @formatter:off
        return configuredMembers.stream()
                                .filter(m -> shownMembers.contains(m.name))
                                .map(m -> shownMembers.indexOf(m.name))
                                .toList();
        // @formatter:on
    }

}
