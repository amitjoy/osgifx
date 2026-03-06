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
package com.osgifx.console.ui.mcp;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.ui.mcp.dto.McpToolDTO;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public final class ToolDetailsFxController {

    @Log
    @Inject
    private FluentLogger logger;
    @FXML
    private TextField        nameLabel;
    @FXML
    private TextField        descriptionLabel;
    @FXML
    private TextArea     schemaTextArea;

    @FXML
    public void initialize() {
        logger.atDebug().log("Tool details controller has been initialized");
    }

    public void initControls(final McpToolDTO tool) {
        nameLabel.setText(tool.nameProperty().get());
        descriptionLabel.setText(tool.descriptionProperty().get());
        schemaTextArea.setText(tool.schemaProperty().get());
    }

}
