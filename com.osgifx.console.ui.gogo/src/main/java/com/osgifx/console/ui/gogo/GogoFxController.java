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
package com.osgifx.console.ui.gogo;

import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.controlsfx.control.textfield.TextFields;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.annotation.bundle.Requirement;

import com.google.common.base.Throwables;
import com.osgifx.console.agent.Agent;
import com.osgifx.console.supervisor.Supervisor;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

@Requirement(effective = "active", namespace = SERVICE_NAMESPACE, filter = "(objectClass=com.osgifx.console.supervisor.Supervisor)")
public final class GogoFxController {

    @Log
    @Inject
    private FluentLogger       logger;
    @FXML
    private TextField          input;
    @FXML
    private TextArea           output;
    @Inject
    private Supervisor         supervisor;
    @Inject
    private GogoConsoleHistory history;
    private Agent              agent;
    private int                historyPointer;

    @FXML
    public void initialize() {
        historyPointer = 0;
        agent          = supervisor.getAgent();
        Set<String> gogoCommands;
        if (agent == null || (gogoCommands = agent.getGogoCommands()) == null) {
            logger.atWarning().log("Agent is not connected");
            return;
        }
        logger.atDebug().log("FXML controller has been initialized");
        TextFields.bindAutoCompletion(input, gogoCommands);
    }

    @FXML
    private synchronized void handleInput(final KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
        case ENTER:
            final var command = input.getText();
            if ("clear".equals(command)) {
                output.clear();
                input.clear();
                return;
            }
            if (command.trim().isEmpty()) {
                return;
            }
            output.appendText("$ " + command + System.lineSeparator());
            executeGogoCommand(command);
            break;
        case UP:
            if (historyPointer == 0) {
                historyPointer = history.size();
            }
            historyPointer--;
            input.setText(history.get(historyPointer));
            input.selectAll();
            input.selectEnd(); // Does not change anything seemingly
            break;
        case DOWN:
            if (historyPointer == history.size() - 1) {
                break;
            }
            historyPointer++;
            input.setText(history.get(historyPointer));
            input.selectAll();
            input.selectEnd(); // Does not change anything seemingly
            break;
        default:
            break;
        }
    }

    private void executeGogoCommand(final String command) {
        final Task<String> task = new Task<>() {

            @Override
            protected String call() throws Exception {
                String outputText;
                try {
                    if (agent == null || (outputText = agent.execGogoCommand(command)) == null) {
                        logger.atWarning().log("Agent is not connected");
                        outputText = "Agent is not connected";
                    }
                    logger.atInfo().log("Command '%s' has been successfully executed", command);
                } catch (final Exception e) {
                    logger.atInfo().withException(e).log("Command '%s' cannot be executed properly", command);
                    outputText = Throwables.getStackTraceAsString(e);
                }
                return outputText;
            }
        };
        task.setOnSucceeded(t -> {
            output.appendText(task.getValue());
            output.appendText(System.lineSeparator());
            history.add(command);
            historyPointer = history.size();
            input.clear();
            logger.atInfo().log("Task for command '%s' has been succeeded", command);
        });
        CompletableFuture.runAsync(task);
    }

}
