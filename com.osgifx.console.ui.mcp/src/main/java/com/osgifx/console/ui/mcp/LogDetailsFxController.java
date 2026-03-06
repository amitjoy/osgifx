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

import java.util.function.Supplier;

import javax.inject.Inject;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.osgifx.console.ui.mcp.dto.McpLogDTO;
import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public final class LogDetailsFxController {

    @Log
    @Inject
    private FluentLogger logger;
    @FXML
    private TextField        timestampLabel;
    @FXML
    private TextField        typeLabel;
    @FXML
    private TextArea     contentTextArea;

    @FXML
    public void initialize() {
        logger.atDebug().log("Log details controller has been initialized");
    }

    public void initControls(final McpLogDTO log) {
        timestampLabel.setText(Fx.formatTime(log.timestampProperty().get()).get());
        typeLabel.setText(log.typeProperty().get());

        final var content = log.contentProperty().get();
        try {
            final var jsonElement = JsonParser.parseString(content);
            contentTextArea.setText(createGSON().get().toJson(jsonElement));
        } catch (final Exception _) {
            contentTextArea.setText(content);
        }
    }

    private Supplier<Gson> createGSON() {
        return () -> new GsonBuilder().setPrettyPrinting().create();
    }

}
