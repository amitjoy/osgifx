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
package com.osgifx.console.ui.conditions;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XConditionDTO;
import com.osgifx.console.agent.dto.XConditionState;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.conditions.dialog.MockConditionDialog;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public final class ConditionDetailsFxController {

    @Log
    @Inject
    private FluentLogger      logger;
    @FXML
    private Button            injectMockButton;
    @FXML
    private Button            revokeMockButton;
    @FXML
    private TextField         identifierLabel;
    @FXML
    private TextField         stateLabel;
    @FXML
    private TextField         providerBundleIdLabel;
    @FXML
    private TextField         providerBundleBsnLabel;
    @FXML
    private TextArea          propertiesArea;
    @FXML
    private TextArea          satisfiedComponentsArea;
    @FXML
    private TextArea          unsatisfiedComponentsArea;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshot;
    @Inject
    private DataProvider      dataProvider;
    @Inject
    private IEclipseContext   eclipseContext;
    @Inject
    private ThreadSynchronize threadSync;
    @Inject
    @Optional
    private Supervisor        supervisor;

    @FXML
    public void initialize() {
        logger.atDebug().log("Condition details FXML controller has been initialized");
    }

    public void initControls(final XConditionDTO condition) {
        identifierLabel.setText(condition.identifier);
        stateLabel.setText(condition.state.toString());

        final var id = condition.providerBundleId;
        providerBundleIdLabel.setText(id == -1 ? "" : String.valueOf(id));

        if (id == -1) {
            providerBundleBsnLabel.setText("");
        } else {
            final var bundle = dataProvider.bundles().stream().filter(b -> b.id == id).findFirst()
                    .map(b -> b.symbolicName).orElse("");
            providerBundleBsnLabel.setText(bundle);
        }

        if (condition.properties != null) {
            propertiesArea.setText(condition.properties.entrySet().stream().map(e -> e.getKey() + " = " + e.getValue())
                    .collect(Collectors.joining("\n")));
        } else {
            propertiesArea.setText("");
        }

        if (condition.satisfiedComponents != null) {
            satisfiedComponentsArea.setText(condition.satisfiedComponents.stream().collect(Collectors.joining("\n")));
        } else {
            satisfiedComponentsArea.setText("");
        }

        if (condition.unsatisfiedComponents != null) {
            unsatisfiedComponentsArea
                    .setText(condition.unsatisfiedComponents.stream().collect(Collectors.joining("\n")));
        } else {
            unsatisfiedComponentsArea.setText("");
        }

        updateButtonStates(condition);

        injectMockButton.setOnAction(_ -> {
            logger.atInfo().log("Inject mock condition request has been sent for %s", condition.identifier);
            final var dialog = new MockConditionDialog();
            ContextInjectionFactory.inject(dialog, eclipseContext);
            dialog.init(condition.identifier);

            final var result = dialog.showAndWait();
            if (result.isPresent()) {
                final var properties = result.get();
                threadSync.asyncExec(() -> {
                    try {
                        supervisor.getAgent().injectMockCondition(condition.identifier, properties);
                        Fx.showSuccessNotification("Mock Condition", "Condition successfully injected");
                        refreshData();
                    } catch (final Exception e) {
                        logger.atError().withException(e).log("Cannot inject mock condition");
                        Fx.showErrorNotification("Mock Condition", "Cannot inject mock condition");
                    }
                });
            }
        });

        revokeMockButton.setOnAction(_ -> {
            logger.atInfo().log("Revoke mock condition request has been sent for %s", condition.identifier);
            threadSync.asyncExec(() -> {
                try {
                    supervisor.getAgent().revokeMockCondition(condition.identifier);
                    Fx.showSuccessNotification("Mock Condition", "Condition successfully revoked");
                    refreshData();
                } catch (final Exception e) {
                    logger.atError().withException(e).log("Cannot revoke mock condition");
                    Fx.showErrorNotification("Mock Condition", "Cannot revoke mock condition");
                }
            });
        });
    }

    private void updateButtonStates(final XConditionDTO condition) {
        final var isMocked  = condition.state == XConditionState.MOCKED;
        final var isMissing = condition.state == XConditionState.MISSING;

        injectMockButton.setDisable(!isConnected || isSnapshot || !isMissing);
        revokeMockButton.setDisable(!isConnected || isSnapshot || !isMocked);
    }

    private void refreshData() {
        dataProvider.retrieveInfo("conditions", true);
    }
}
