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
package com.osgifx.console.ui.gogo;

import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_CAPABILITIES_TOPIC;

import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Named;

import org.controlsfx.control.textfield.TextFields;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.base.Throwables;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

public final class GogoFxController {

    private final ReentrantLock lock = new ReentrantLock();

    @Log
    @Inject
    private FluentLogger       logger;
    @Inject
    private Executor           executor;
    @FXML
    private TextField          input;
    @FXML
    private TextArea           output;
    @Inject
    private GogoConsoleHistory history;
    @Inject
    @Optional
    private Supervisor         supervisor;
    @Inject
    @Named("is_snapshot_agent")
    private boolean            isSnapshotAgent;
    @Inject
    private DataProvider       dataProvider;
    @Inject
    @Named("is_connected")
    private boolean            isConnected;
    @Inject
    private ThreadSynchronize  threadSync;
    private int                historyPointer;

    @FXML
    public void initialize() {
        historyPointer = 0;
        final var parent = (BorderPane) output.getParent();
        if (!isConnected) {
            parent.setCenter(Fx.createDisconnectedPlaceholder());
            input.setDisable(true);
            return;
        }
        if (isSnapshotAgent) {
            parent.setCenter(Fx.createSnapshotPlaceholder());
            input.setDisable(true);
            return;
        }
        if (!isCapabilityAvailable("GOGO")) {
            parent.setCenter(Fx.createFeatureUnavailablePlaceholder("Apache Felix Gogo"));
            input.setDisable(true);
            return;
        }
        final var agent = supervisor != null ? supervisor.getAgent() : null;
        if (agent == null) {
            logger.atWarning().log("Agent not connected");
            return;
        }
        parent.setCenter(output);
        input.setDisable(false);
        executor.runAsync(() -> {
            final var gogoCommands = agent.getGogoCommands();
            if (gogoCommands != null) {
                threadSync.asyncExec(() -> TextFields.bindAutoCompletion(input, gogoCommands));
            }
        });
        logger.atDebug().log("FXML controller has been initialized");
    }

    private boolean isCapabilityAvailable(final String capabilityId) {
        return dataProvider.runtimeCapabilities().stream().anyMatch(c -> capabilityId.equals(c.id) && c.isAvailable);
    }

    @FXML
    private void handleInput(final KeyEvent keyEvent) {
        lock.lock();
        try {
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
        } finally {
            lock.unlock();
        }
    }

    private void executeGogoCommand(final String command) {
        final Task<String> task = new Task<>() {

            @Override
            protected String call() throws Exception {
                String outputText;
                try {
                    final var currentAgent = supervisor != null ? supervisor.getAgent() : null;
                    if (isSnapshotAgent) {
                        logger.atWarning().log("No command execution in snapshot agent mode");
                        outputText = "You cannot execute command in snapshot agent mode";
                    } else if (currentAgent == null || (outputText = currentAgent.execGogoCommand(command)) == null) {
                        logger.atWarning().log("Agent is not connected");
                        outputText = "Agent is not connected";
                    } else {
                        logger.atInfo().log("Command '%s' has been successfully executed", command);
                    }
                } catch (final Exception e) {
                    logger.atInfo().withException(e).log("Command '%s' cannot be executed properly", command);
                    outputText = Throwables.getStackTraceAsString(e);
                }
                return outputText;
            }
        };
        task.setOnSucceeded(_ -> {
            output.appendText(task.getValue());
            output.appendText(System.lineSeparator());
            history.add(command);
            historyPointer = history.size();
            input.clear();
            logger.atInfo().log("Task for command '%s' has been succeeded", command);
        });
        executor.runAsync(task);
    }

    @Inject
    @Optional
    private void updateOnCapabilitiesRetrievedEvent(@UIEventTopic(DATA_RETRIEVED_CAPABILITIES_TOPIC) final String data) {
        if (input == null) {
            return;
        }
        threadSync.asyncExec(this::initialize);
    }

}
