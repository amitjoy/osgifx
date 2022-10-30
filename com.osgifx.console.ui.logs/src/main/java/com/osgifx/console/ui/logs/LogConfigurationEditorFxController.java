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
package com.osgifx.console.ui.logs;

import static com.dlsc.formsfx.model.validators.CustomValidator.forPredicate;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.event.topics.LoggerContextActionEventTopics.LOGGER_CONTEXT_UPDATED_EVENT_TOPIC;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.ui.services.internal.events.EventBroker;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.service.log.LogLevel;

import com.dlsc.formsfx.model.structure.DataField;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import com.google.common.collect.Lists;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.ui.logs.helper.LoggerConfigTextControl;
import com.osgifx.console.ui.logs.helper.LogsHelper;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public final class LogConfigurationEditorFxController {

    private static final String KV_DESCRIPTION        = "key=value pairs separated by line breaks, for example, a.b.c=WARN";
    private static final String KV_VALIDATION_MESSAGE = "key=value pairs cannot be validated";

    @Log
    @Inject
    private FluentLogger            logger;
    @FXML
    private GridPane                mainPane;
    @Inject
    @Named("is_connected")
    private boolean                 isConnected;
    @Inject
    private Supervisor              supervisor;
    @Inject
    private EventBroker             eventBroker;
    @FXML
    private BorderPane              rootPanel;
    @FXML
    private Button                  cancelButton;
    @FXML
    private Button                  saveLogConfigButton;
    private Form                    form;
    private FormRenderer            formRenderer;
    private XBundleLoggerContextDTO loggerContext;

    @FXML
    public void initialize() {
        logger.atDebug().log("FXML controller has been initialized");
    }

    public void initControls(final XBundleLoggerContextDTO loggerContext) {
        this.loggerContext = loggerContext;
        if (formRenderer != null) {
            rootPanel.getChildren().remove(formRenderer);
        }
        formRenderer = createForm(loggerContext);
        initButtons();
        rootPanel.setCenter(formRenderer);
    }

    private void initButtons() {
        saveLogConfigButton.setOnAction(event -> saveLogConfig());
        cancelButton.setOnAction(e -> form.reset());
        cancelButton.disableProperty().bind(form.changedProperty().not());
        saveLogConfigButton.disableProperty().bind(form.changedProperty().not().or(form.validProperty().not()));
    }

    private void saveLogConfig() {
        final var name = loggerContext.name;
        logger.atInfo().log("String log configuration for context '%s'", name);
        final var result = supervisor.getAgent().updateBundleLoggerContext(name, getLogLevels());
        if (result.result == SUCCESS) {
            logger.atInfo().log(result.response);
            eventBroker.post(LOGGER_CONTEXT_UPDATED_EVENT_TOPIC, name);
            Fx.showSuccessNotification("Logger Context", "Logger context has been updated successfully");
        } else if (result.result == SKIPPED) {
            logger.atWarning().log(result.response);
            FxDialog.showWarningDialog("Logger Context", result.response, getClass().getClassLoader());
        } else {
            logger.atError().log(result.response);
            FxDialog.showErrorDialog("Logger Context", result.response, getClass().getClassLoader());
        }
    }

    private Map<String, String> getLogLevels() {
        return LogsHelper.prepareKeyValuePairs(((DataField<?, ?, ?>) form.getFields().get(2)).getValue());
    }

    private FormRenderer createForm(final XBundleLoggerContextDTO loggerContext) {
        form = Form.of(Section.of(initGenericFields(loggerContext).toArray(new Field[0])).title("Generic Information"),
                Section.of(initField(loggerContext).toArray(new Field[0])).title("Logger Context Configuration"))
                .title("Logger Context Configuration");
        final var renderer = new FormRenderer(form);

        GridPane.setColumnSpan(renderer, 2);
        GridPane.setRowIndex(renderer, 3);
        GridPane.setRowSpan(renderer, Integer.MAX_VALUE);
        GridPane.setMargin(renderer, new Insets(0, 0, 0, 50));

        return renderer;
    }

    private List<Field<?>> initGenericFields(final XBundleLoggerContextDTO loggerContext) {
        final var      rootLogLevel      = Optional.ofNullable(loggerContext.rootLogLevel).map(LogLevel::name)
                .orElse("<NOT SET>");
        final Field<?> nameField         = Field.ofStringType(loggerContext.name).label("Name").editable(false);
        final Field<?> rootLogLevelField = Field.ofStringType(rootLogLevel).label("Global Root Log Level")
                .editable(false);

        return Lists.newArrayList(nameField, rootLogLevelField);
    }

    private List<Field<?>> initField(final XBundleLoggerContextDTO loggerContext) {
        final var logLevelsX = loggerContext.logLevels;
        final var logLevels  = logLevelsX == null ? Map.of() : logLevelsX;

        // @formatter:off
        final Field<?> logLevelsField = Field.ofStringType(LogsHelper.mapToString(logLevels))
                                             .multiline(true)
                                             .render(new LoggerConfigTextControl())
                                             .label("Log Levels")
                                             .valueDescription(KV_DESCRIPTION)
                                             .validate(forPredicate(LogsHelper::validateKeyValuePairs, KV_VALIDATION_MESSAGE));
        // @formatter:on
        return List.of(logLevelsField);
    }

}
