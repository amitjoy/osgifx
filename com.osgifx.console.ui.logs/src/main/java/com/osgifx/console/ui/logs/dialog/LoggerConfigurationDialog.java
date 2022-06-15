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
package com.osgifx.console.ui.logs.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.List;
import java.util.Map;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.osgi.service.log.LogLevel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.osgifx.console.ui.logs.helper.LogsHelper;
import com.osgifx.console.util.fx.FxDialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class LoggerConfigurationDialog extends Dialog<String> {

	private final List<PropertiesForm> entries = Lists.newArrayList();

	public void init(final Map<String, LogLevel> logLevels) {
		final var dialogPane = getDialogPane();

		initStyle(StageStyle.UNDECORATED);
		dialogPane.setHeaderText("Logger Context Configuration");
		dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
		dialogPane.setGraphic(new ImageView(getClass().getResource("/graphic/images/configuration.png").toString()));
		dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

		final var lbMessage = new Label("");
		lbMessage.getStyleClass().addAll("message-banner");
		lbMessage.setVisible(false);
		lbMessage.setManaged(false);

		final var content = new VBox(10);

		content.getChildren().add(lbMessage);
		if (!logLevels.isEmpty()) {
			logLevels.forEach((k, v) -> addFieldPair(content, k, v));
		} else {
			addFieldPair(content);
		}

		dialogPane.setContent(content);

		final var finishButtonType = new ButtonType("Finish", ButtonData.OK_DONE);
		dialogPane.getButtonTypes().addAll(finishButtonType);

		final var finishButton = (Button) dialogPane.lookupButton(finishButtonType);
		finishButton.setOnAction(actionEvent -> {
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
			try {
				return data == ButtonData.OK_DONE ? getInput() : null;
			} catch (final Exception e) {
				return null;
			}
		});
	}

	private String getInput() {
		final Map<String, LogLevel> logLevels = Maps.newHashMap();
		for (final PropertiesForm form : entries) {
			final var loggerConfig   = form.txtKey.getText();
			final var logLevelConfig = form.comboBox.getValue();
			if (!loggerConfig.isBlank()) {
				logLevels.put(loggerConfig, logLevelConfig);
			}
		}
		return LogsHelper.mapToString(logLevels);
	}

	private class PropertiesForm extends HBox {

		private final Button btnAddField;
		private final Button btnRemoveField;

		private final CustomTextField    txtKey;
		private final ComboBox<LogLevel> comboBox;

		public PropertiesForm(final VBox parent, final String config, final LogLevel logLevel) {
			setAlignment(Pos.CENTER_LEFT);
			setSpacing(5);

			txtKey = (CustomTextField) TextFields.createClearableTextField();
			txtKey.setLeft(new ImageView(getClass().getResource("/graphic/icons/kv.png").toExternalForm()));

			txtKey.setPromptText("Logger Configuration");
			txtKey.setText(config != null ? config : "");

			btnAddField    = new Button();
			btnRemoveField = new Button();

			final ObservableList<LogLevel> options = FXCollections.observableArrayList(LogLevel.values());
			comboBox = new ComboBox<>(options);

			comboBox.getSelectionModel().select(logLevel != null ? logLevel : LogLevel.INFO);

			btnAddField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.PLUS));
			btnRemoveField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));

			btnAddField.setOnAction(e -> addFieldPair(parent));
			btnRemoveField.setOnAction(e -> removeFieldPair(parent, this));

			getChildren().addAll(txtKey, comboBox, btnAddField, btnRemoveField);

			entries.add(this);
		}

		private void removeFieldPair(final VBox content, final PropertiesForm form) {
			if (content.getChildren().size() > 2) {
				content.getChildren().remove(form);
				getDialogPane().getScene().getWindow().sizeToScene();
				entries.remove(form);
			}
		}

	}

	private void addFieldPair(final VBox parent) {
		addFieldPair(parent, null, null);
	}

	private void addFieldPair(final VBox content, final String config, final LogLevel logLevel) {
		content.getChildren().add(new PropertiesForm(content, config, logLevel));
		getDialogPane().getScene().getWindow().sizeToScene();
	}

}
