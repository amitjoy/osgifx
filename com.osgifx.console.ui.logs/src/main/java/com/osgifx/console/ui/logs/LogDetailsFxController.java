/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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

import java.util.Date;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

import com.osgifx.console.agent.dto.XLogEntryDTO;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public final class LogDetailsFxController {

	@Log
	@Inject
	private FluentLogger logger;
	@FXML
	private Label        receivedAtLabel;
	@FXML
	private Label        levelLabel;
	@FXML
	private Label        bundleLabel;
	@FXML
	private Label        loggerLabel;
	@FXML
	private Label        threadLabel;
	@FXML
	private TextArea     messageText;
	@FXML
	private TextArea     exceptionText;
	private Converter    converter;

	@FXML
	public void initialize() {
		converter = Converters.standardConverter();
		logger.atDebug().log("FXML controller has been initialized");
	}

	public void initControls(final XLogEntryDTO logEntry) {
		receivedAtLabel.setText(formatReceivedAt(logEntry.loggedAt));
		levelLabel.setText(logEntry.level);
		loggerLabel.setText(logEntry.logger);
		threadLabel.setText(logEntry.threadInfo);
		bundleLabel.setText(logEntry.bundle.symbolicName);
		messageText.setText(logEntry.message);
		if (logEntry.exception != null) {
			exceptionText.setText(logEntry.exception);
		}
	}

	private String formatReceivedAt(final long receivedAt) {
		if (receivedAt == 0) {
			return "No received timestamp";
		}
		return converter.convert(receivedAt).to(Date.class).toString();
	}

}
