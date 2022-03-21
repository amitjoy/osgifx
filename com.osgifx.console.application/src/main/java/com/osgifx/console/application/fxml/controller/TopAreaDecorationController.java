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
package com.osgifx.console.application.fxml.controller;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;

public final class TopAreaDecorationController {

	@Log
	@Inject
	private FluentLogger logger;
	@FXML
	private ToolBar      decorationArea;

	@FXML
	public void initialize() {
		logger.atInfo().log("FXML controller has been initialized");
	}

	@FXML
	public void handleClose(final ActionEvent event) {
		Platform.exit();
	}

	Stage getStage() {
		return (Stage) decorationArea.getScene().getWindow();
	}
}
