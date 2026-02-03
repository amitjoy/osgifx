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
package com.osgifx.console.ui.events.dialog;

import static com.google.common.base.Verify.verify;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static com.osgifx.console.util.fx.ConsoleFxHelper.validateTopic;
import static org.controlsfx.validation.Validator.createPredicateValidator;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;
import org.controlsfx.validation.ValidationSupport;
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

public final class SendEventDialog extends Dialog<EventDTO> {

    @Log
    @Inject
    private FluentLogger            logger;
    private final ValueConverter    converter         = new ValueConverter();
    private final ValidationSupport validationSupport = new ValidationSupport();

    public void init() {
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Send OSGi Events");
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/events.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final var txtTopic = (CustomTextField) TextFields.createClearableTextField();
        txtTopic.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));
        validationSupport.registerValidator(txtTopic,
                createPredicateValidator(value -> validateTopic(value.toString()), "Invalid Event Topic"));

        final var isSyncToggle = new ToggleSwitch("Is Synchronous?");

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);

        content.getChildren().add(lbMessage);
        content.getChildren().add(isSyncToggle);
        content.getChildren().add(txtTopic);

        final var form = new PropertiesForm(dialogPane);
        form.addFieldPair(content);

        dialogPane.setContent(content);

        final var sendButtonType = new ButtonType("Send", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(sendButtonType);

        final var sendButton = (Button) dialogPane.lookupButton(sendButtonType);
        sendButton.setOnAction(actionEvent -> {
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
        final var pidCaption = "Topic";

        txtTopic.setPromptText(pidCaption);
        sendButton.disableProperty().bind(txtTopic.textProperty().isEmpty().or(validationSupport.invalidProperty()));

        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            if (data == ButtonData.CANCEL_CLOSE) {
                return null;
            }
            try {
                verify(!validationSupport.isInvalid(), "Topic validation failed");
                return data == ButtonData.OK_DONE ? getInput(txtTopic, isSyncToggle, form.entries()) : null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Topic configuration values cannot be converted");
                throw e;
            }
        });
    }

    private EventDTO getInput(final CustomTextField txtTopic,
                              final ToggleSwitch isSyncToggle,
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
        return new EventDTO(txtTopic.getText(), isSyncToggle.isSelected(), properties);
    }

}
