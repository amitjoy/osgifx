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
package com.osgifx.console.ui.events.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import java.util.List;
import java.util.Set;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.osgifx.console.util.fx.ConsoleFxHelper;
import com.osgifx.console.util.fx.FxDialog;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class TopicEntryDialog extends Dialog<Set<String>> {

	private final List<PropertiesForm> entries           = Lists.newArrayList();
	private final ValidationSupport    validationSupport = new ValidationSupport();

	public void init() {
		final var dialogPane = getDialogPane();

		initStyle(StageStyle.UNDECORATED);
		dialogPane.setHeaderText("Receive Events from Topics");
		dialogPane.getStylesheets().add(getClass().getClassLoader().getResource(STANDARD_CSS).toExternalForm());
		dialogPane.setGraphic(new ImageView(getClass().getClassLoader().getResource("/graphic/images/event-receive.png").toString()));
		dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

		final var lbMessage = new Label("");
		lbMessage.getStyleClass().addAll("message-banner");
		lbMessage.setVisible(false);
		lbMessage.setManaged(false);

		final var content = new VBox(10);
		addFieldPair(content);

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

	private Set<String> getInput() {
		final Set<String> topics = Sets.newHashSet();
		for (final PropertiesForm form : entries) {
			final var value = form.textTopic.getText();
			if (Strings.isNullOrEmpty(value)) {
				continue;
			}
			topics.add(value);
		}
		return topics;
	}

	private class PropertiesForm extends HBox {

		private final Button btnAddField;
		private final Button btnRemoveField;

		private final CustomTextField textTopic;

		public PropertiesForm(final VBox parent) {
			setAlignment(Pos.CENTER_LEFT);
			setSpacing(5);

			textTopic = (CustomTextField) TextFields.createClearableTextField();
			textTopic.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));
			textTopic.setPromptText("Event Topic");
			validationSupport.registerValidator(textTopic, Validator.createPredicateValidator(value -> {
				try {
					ConsoleFxHelper.validateTopic(value.toString().trim());
					return true;
				} catch (final Exception e) {
					return false;
				}
			}, "Invalid Event Topic"));

			btnAddField    = new Button();
			btnRemoveField = new Button();

			btnAddField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.PLUS));
			btnRemoveField.setGraphic(new Glyph("FontAwesome", FontAwesome.Glyph.MINUS));

			btnAddField.setOnAction(e -> addFieldPair(parent));
			btnRemoveField.setOnAction(e -> removeFieldPair(parent, this));

			getChildren().addAll(textTopic, btnAddField, btnRemoveField);
			entries.add(this);
		}

		private void removeFieldPair(final VBox content, final PropertiesForm form) {
			if (content.getChildren().size() > 1) {
				content.getChildren().remove(form);
				getDialogPane().getScene().getWindow().sizeToScene();
			}
			entries.remove(form);
		}
	}

	private void addFieldPair(final VBox content) {
		content.getChildren().add(new PropertiesForm(content));
		getDialogPane().getScene().getWindow().sizeToScene();
	}

}
