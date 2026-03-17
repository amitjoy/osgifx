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
package com.osgifx.console.ui.conditions.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.dialog.LoginDialog;
import org.eclipse.fx.core.Triple;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.collect.Maps;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.util.converter.ValueConverter;
import com.osgifx.console.util.filter.FilterParser;
import com.osgifx.console.util.filter.FilterParser.And;
import com.osgifx.console.util.filter.FilterParser.Expression;
import com.osgifx.console.util.filter.FilterParser.SimpleExpression;
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

public final class MockConditionDialog extends Dialog<Map<String, Object>> {

    @Log
    @Inject
    private FluentLogger         logger;
    private final ValueConverter converter = new ValueConverter();

    public void init(final String targetFilter) {
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Inject Mock Condition");
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/inject.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);
        content.getChildren().add(lbMessage);

        final var form = new PropertiesForm(dialogPane);

        prefillProperties(form, targetFilter, content);

        if (form.entries().isEmpty()) {
            form.addFieldPair(content);
        }

        dialogPane.setContent(content);

        final var injectButtonType = new ButtonType("Inject", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(injectButtonType);

        final var injectButton = (Button) dialogPane.lookupButton(injectButtonType);
        injectButton.setOnAction(_ -> {
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

        setResultConverter(dialogButton -> {
            final var data = dialogButton == null ? null : dialogButton.getButtonData();
            if (data == ButtonData.CANCEL_CLOSE) {
                return null;
            }
            try {
                return data == ButtonData.OK_DONE ? getInput(form.entries()) : null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Condition configuration values cannot be converted");
                throw e;
            }
        });
    }

    private void prefillProperties(final PropertiesForm form, final String targetFilter, final VBox content) {
        if (targetFilter == null || targetFilter.isBlank() || !targetFilter.startsWith("(")) {
            return;
        }
        try {
            final var parser     = new FilterParser();
            final var expression = parser.parse(targetFilter);

            extractProperties(expression).forEach((k, v) -> {
                form.addFieldPair(content, k, String.valueOf(v), XAttributeDefType.STRING);
            });
        } catch (final Exception e) {
            logger.atWarning().withException(e).log("Cannot parse filter to pre-fill properties: " + targetFilter);
        }
    }

    private Map<String, Object> extractProperties(final Expression expression) {
        final Map<String, Object> props = Maps.newHashMap();
        if (expression instanceof SimpleExpression se) {
            if (se.getOp() == FilterParser.Op.EQUAL) {
                props.put(se.getKey(), se.getValue());
            }
        } else if (expression instanceof And andExpr) {
            for (final Expression e : andExpr.getExpressions()) {
                props.putAll(extractProperties(e));
            }
        }
        return props;
    }

    private Map<String, Object> getInput(final Map<FormContent, Triple<Supplier<String>, Supplier<String>, Supplier<XAttributeDefType>>> entries) {
        final Map<String, Object> properties = Maps.newHashMap();
        entries.forEach((_, v) -> {
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
            properties.put(configKey, convertedValue);
        });
        return properties;
    }

}
