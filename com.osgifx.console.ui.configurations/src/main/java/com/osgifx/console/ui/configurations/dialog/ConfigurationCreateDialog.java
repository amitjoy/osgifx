/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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
package com.osgifx.console.ui.configurations.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;
import org.eclipse.fx.core.Triple;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Lists;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.util.converter.ValueConverter;
import com.osgifx.console.util.fx.FxDialog;
import com.osgifx.console.util.fx.PropertiesForm;
import com.osgifx.console.util.fx.PropertiesForm.FormContent;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class ConfigurationCreateDialog extends Dialog<ConfigurationDTO> {

    @Log
    @Inject
    private FluentLogger         logger;
    private final ValueConverter converter = new ValueConverter();

    public void init() {
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Create New Configuration for OSGi Configuration Admin");
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane
                .setGraphic(new ImageView(this.getClass().getResource("/graphic/images/configuration.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final var txtPid = (CustomTextField) TextFields.createClearableTextField();
        txtPid.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));

        final var txtFactoryPid = (CustomTextField) TextFields.createClearableTextField();
        txtFactoryPid.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);

        content.getChildren().add(lbMessage);
        content.getChildren().add(txtPid);
        content.getChildren().add(txtFactoryPid);

        final var form = new PropertiesForm(dialogPane);
        form.addFieldPair(content);

        dialogPane.setContent(content);

        final var createButtonType = new ButtonType("Create", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(createButtonType);

        final var createButton = (Button) dialogPane.lookupButton(createButtonType);
        createButton.setOnAction(actionEvent -> {
            try {
                lbMessage.setVisible(false);
                lbMessage.setManaged(false);
                hide();
            } catch (final Exception ex) {
                lbMessage.setVisible(true);
                lbMessage.setManaged(true);
                lbMessage.setText(ex.getMessage());
                FxDialog.showExceptionDialog(ex, getClass().getClassLoader());
            }
        });

        createButton.disableProperty()
                .bind(txtPid.textProperty().isEmpty().and(txtFactoryPid.textProperty().isEmpty()));

        final var pidCaption        = "Configuration PID";
        final var factoryPidCaption = "Factory PID";

        txtPid.setPromptText(pidCaption);
        txtFactoryPid.setPromptText(factoryPidCaption);

        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            if (data == ButtonData.CANCEL_CLOSE) {
                return null;
            }
            try {
                return data == ButtonData.OK_DONE ? getInput(txtPid, txtFactoryPid, form.entries()) : null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Configuration values cannot be converted");
                throw e;
            }
        });
    }

    private ConfigurationDTO getInput(final CustomTextField txtPid,
                                      final CustomTextField txtFactoryPid,
                                      final Map<FormContent, Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>>> entries) {
        final List<ConfigValue> properties = Lists.newArrayList();
        entries.forEach((k, v) -> {
            var configKey   = v.value1.get();
            var configValue = v.value2.get();
            var configType  = v.value3.get();
            if (StringUtils.isBlank(configKey) || StringUtils.isBlank(configValue)) {
                return;
            }
            configKey   = configKey.strip();
            configValue = configValue.strip();
            if (configType == null) {
                configType = XAttributeDefType.STRING;
            }
            final var convertedValue = converter.convert(configValue, configType);
            properties.add(ConfigValue.create(configKey, convertedValue, configType));
        });
        return new ConfigurationDTO(txtPid.getText(), txtFactoryPid.getText(), properties);
    }

}
