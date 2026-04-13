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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
import com.osgifx.console.ai.GeneratorMode;
import com.osgifx.console.ai.GeneratorState;
import com.osgifx.console.ai.GogoCommandGenerator;
import com.osgifx.console.ai.GogoExampleRetriever;
import com.osgifx.console.ai.LLMProviderConfig;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.util.fx.Fx;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public final class GogoFxController {

    private final ReentrantLock   lock       = new ReentrantLock();
    private final BooleanProperty generating = new SimpleBooleanProperty(false);

    @Log
    @Inject
    private FluentLogger         logger;
    @Inject
    private Executor             executor;
    @FXML
    private SplitPane            rootSplitPane;
    @FXML
    private TextField            input;
    @FXML
    private TextArea             output;
    @FXML
    private BorderPane           aiPanel;
    @FXML
    private BorderPane           terminalPane;
    @FXML
    private TextArea             promptInput;
    @FXML
    private TextArea             stagingArea;
    @FXML
    private Button               generateBtn;
    @FXML
    private Button               clearBtn;
    @FXML
    private Button               executeAllBtn;
    @FXML
    private Button               executeSelectionBtn;
    @FXML
    private Button               stepBtn;
    @FXML
    private Button               resetBtn;
    @FXML
    private Button               copyBtn;
    @FXML
    private Label                aiStatusLabel;
    @FXML
    private ComboBox<String>     modeCombo;
    @FXML
    private VBox                 providerConfigBox;
    @FXML
    private ComboBox<String>     providerCombo;
    @FXML
    private PasswordField        apiKeyInput;
    @FXML
    private TextField            baseUrlInput;
    @FXML
    private TextField            modelInput;
    @FXML
    private Button               saveConfigBtn;
    @FXML
    private Button               resetConfigBtn;
    @Inject
    private GogoConsoleHistory   history;
    @Inject
    @Optional
    private Supervisor           supervisor;
    @Inject
    @Named("is_snapshot_agent")
    private boolean              isSnapshotAgent;
    @Inject
    private DataProvider         dataProvider;
    @Inject
    @Named("is_connected")
    private boolean              isConnected;
    @Inject
    private ThreadSynchronize    threadSync;
    @Inject
    @Optional
    private GogoCommandGenerator commandGenerator;
    @Inject
    @Optional
    private GogoExampleRetriever exampleRetriever;
    private int                  historyPointer;
    private int                  currentStatementIndex;
    private List<GogoStatement>  currentStatements;

    // @formatter:off
    private static final Map<String, String[]> PROVIDER_PRESETS = Map.ofEntries(
            Map.entry("OpenAI",      new String[] { "https://api.openai.com/v1",                                    "gpt-4o-mini" }),
            Map.entry("Gemini",      new String[] { "https://generativelanguage.googleapis.com/v1beta/openai",      "gemini-2.0-flash" }),
            Map.entry("Groq",        new String[] { "https://api.groq.com/openai/v1",                               "llama-3.1-70b-versatile" }),
            Map.entry("Grok",        new String[] { "https://api.x.ai/v1",                                          "grok-3-mini" }),
            Map.entry("DeepSeek",    new String[] { "https://api.deepseek.com/v1",                                  "deepseek-chat" }),
            Map.entry("Mistral",     new String[] { "https://api.mistral.ai/v1",                                    "mistral-small-latest" }),
            Map.entry("OpenRouter",  new String[] { "https://openrouter.ai/api/v1",                                 "openai/gpt-4o-mini" }),
            Map.entry("Perplexity",  new String[] { "https://api.perplexity.ai/v1",                                 "sonar" }),
            Map.entry("Together AI", new String[] { "https://api.together.xyz/v1",                                  "meta-llama/Llama-3-70b-chat-hf" }),
            Map.entry("GitHub",      new String[] { "https://models.inference.ai.azure.com",                         "gpt-4o" }),
            Map.entry("SambaNova",   new String[] { "https://api.sambanova.ai/v1",                                  "Llama-3.1-405B-Instruct" }),
            Map.entry("Cerebras",    new String[] { "https://api.cerebras.ai/v1",                                   "llama-3.3-70b" }),
            Map.entry("Fireworks",   new String[] { "https://api.fireworks.ai/inference/v1",                         "accounts/fireworks/models/llama-v3p1-70b-instruct" }),
            Map.entry("Novita AI",   new String[] { "https://api.novita.ai/v3/openai",                              "meta-llama/llama-3.1-70b-instruct" }),
            Map.entry("SiliconFlow", new String[] { "https://api.siliconflow.cn/v1",                                "deepseek-ai/DeepSeek-V3" }),
            Map.entry("Lepton AI",   new String[] { "https://api.lepton.ai/v1",                                     "llama3-70b" }),
            Map.entry("Minimax",     new String[] { "https://api.minimax.chat/v1",                                  "abab6.5-chat" }),
            Map.entry("Ollama",      new String[] { "http://localhost:11434/v1",                                     "qwen2.5-coder:7b" }),
            Map.entry("LM Studio",   new String[] { "http://localhost:1234/v1",                                      "" }),
            Map.entry("LiteLLM",     new String[] { "http://localhost:4000",                                         "" }),
            Map.entry("LocalAI",     new String[] { "http://localhost:8080/v1",                                      "" }),
            Map.entry("vLLM",        new String[] { "http://localhost:8000/v1",                                      "" }),
            Map.entry("llama.cpp",   new String[] { "http://localhost:8080/v1",                                      "" }),
            Map.entry("Custom",      new String[] { "",                                                              "" })
    );
    // @formatter:on

    @FXML
    public void initialize() {
        historyPointer        = 0;
        currentStatementIndex = 0;
        currentStatements     = List.of();

        if (!isConnected) {
            showFullTabPlaceholder(Fx.createDisconnectedPlaceholder());
            return;
        }
        if (isSnapshotAgent) {
            showFullTabPlaceholder(Fx.createSnapshotPlaceholder());
            return;
        }
        if (!isCapabilityAvailable("GOGO")) {
            showFullTabPlaceholder(Fx.createFeatureUnavailablePlaceholder("Apache Felix Gogo"));
            return;
        }
        final var agent = supervisor != null ? supervisor.getAgent() : null;
        if (agent == null) {
            logger.atWarning().log("Agent not connected");
            return;
        }
        terminalPane.setCenter(output);
        input.setDisable(false);
        executor.runAsync(() -> {
            final var gogoCommands = agent.getGogoCommands();
            if (gogoCommands != null) {
                threadSync.asyncExec(() -> TextFields.bindAutoCompletion(input, gogoCommands));
            }
        });

        // AI panel initialization (left side)
        initializeAiPanel();
        setupKeyboardShortcuts();

        logger.atDebug().log("FXML controller has been initialized");
    }

    private void showFullTabPlaceholder(final Node placeholder) {
        rootSplitPane.getItems().clear();
        rootSplitPane.getItems().add(placeholder);
    }

    private void initializeAiPanel() {
        // Mode combo setup
        modeCombo.setItems(FXCollections.observableArrayList("Smart Match", "AI Provider"));

        // Provider combo setup
        providerCombo.setItems(FXCollections.observableArrayList("OpenAI", "Gemini", "Groq", "Grok", "DeepSeek",
                "Mistral", "OpenRouter", "Perplexity", "Together AI", "GitHub", "SambaNova", "Cerebras", "Fireworks",
                "Novita AI", "SiliconFlow", "Lepton AI", "Minimax", "Ollama", "LM Studio", "LiteLLM", "LocalAI", "vLLM",
                "llama.cpp", "Custom"));
        providerCombo.setVisibleRowCount(providerCombo.getItems().size());
        providerCombo.setOnAction(_ -> {
            final var selected = providerCombo.getValue();
            if (selected != null && PROVIDER_PRESETS.containsKey(selected)) {
                final var preset = PROVIDER_PRESETS.get(selected);
                baseUrlInput.setText(preset[0]);
                modelInput.setText(preset[1]);
            }
        });

        if (commandGenerator != null) {
            // Set mode combo from persisted state
            final var mode = commandGenerator.currentMode();
            modeCombo.setValue(mode == GeneratorMode.AI_PROVIDER ? "AI Provider" : "Smart Match");
            updateProviderConfigVisibility(mode);

            // Populate provider config fields from persisted state
            final var config = commandGenerator.currentConfig();
            if (config != null) {
                providerCombo.setValue(config.providerName());
                apiKeyInput.setText(config.apiKey());
                baseUrlInput.setText(config.baseUrl());
                modelInput.setText(config.modelName());
            }

            updateAiStatus(commandGenerator.currentState());
            commandGenerator.addStateListener(state -> Platform.runLater(() -> updateAiStatus(state)));
        } else {
            modeCombo.setValue("Smart Match");
            updateAiStatus(GeneratorState.READY);
        }

        modeCombo.setOnAction(_ -> {
            final var selected = modeCombo.getValue();
            final var newMode  = "AI Provider".equals(selected) ? GeneratorMode.AI_PROVIDER : GeneratorMode.SMART_MATCH;
            updateProviderConfigVisibility(newMode);
            if (commandGenerator != null) {
                commandGenerator.setMode(newMode);
            }
        });

        // Bind button states to content
        final var promptEmpty  = promptInput.textProperty().isEmpty();
        final var stagingEmpty = stagingArea.textProperty().isEmpty();

        // Generate/Clear depend on prompt content
        generateBtn.disableProperty().bind(promptEmpty.or(generating));
        clearBtn.disableProperty().bind(promptEmpty.and(stagingEmpty));

        // Execution buttons depend on connection AND staging content
        final var hasConnection = isConnected && !isSnapshotAgent;
        if (!hasConnection) {
            executeAllBtn.setDisable(true);
            executeSelectionBtn.setDisable(true);
            stepBtn.setDisable(true);
            resetBtn.setDisable(true);
        } else {
            executeAllBtn.disableProperty().bind(stagingEmpty);
            stepBtn.disableProperty().bind(stagingEmpty);
            resetBtn.disableProperty().bind(stagingEmpty);
            // Run Selection needs selected text — bind via listener
            executeSelectionBtn.setDisable(true);
            stagingArea.selectedTextProperty().addListener((_, _, newVal) -> {
                executeSelectionBtn.setDisable(newVal == null || newVal.isEmpty());
            });
        }

        // Copy depends on staging content only
        copyBtn.disableProperty().bind(stagingEmpty);
    }

    private void updateProviderConfigVisibility(final GeneratorMode mode) {
        final var showConfig = mode == GeneratorMode.AI_PROVIDER;
        providerConfigBox.setVisible(showConfig);
        providerConfigBox.setManaged(showConfig);
    }

    private void updateAiStatus(final GeneratorState state) {
        aiStatusLabel.getStyleClass().removeAll("ready", "loading", "error");
        switch (state) {
            case NOT_CONFIGURED:
                aiStatusLabel.setText("Not configured");
                aiStatusLabel.getStyleClass().add("loading");
                break;
            case READY:
                final var mode = commandGenerator != null ? commandGenerator.currentMode() : GeneratorMode.SMART_MATCH;
                aiStatusLabel.setText(mode == GeneratorMode.AI_PROVIDER ? "AI Provider ready" : "Smart Match ready");
                aiStatusLabel.getStyleClass().add("ready");
                break;
            case ERROR:
                aiStatusLabel.setText("Error");
                aiStatusLabel.getStyleClass().add("error");
                break;
        }
    }

    @FXML
    private void handleSaveConfig() {
        if (commandGenerator == null) {
            return;
        }
        final var provider = providerCombo.getValue();
        final var apiKey   = apiKeyInput.getText();
        final var baseUrl  = baseUrlInput.getText();
        final var model    = modelInput.getText();

        final var hasApiKey  = apiKey != null && !apiKey.trim().isEmpty();
        final var hasBaseUrl = baseUrl != null && !baseUrl.trim().isEmpty();

        if (!hasApiKey || !hasBaseUrl) {
            // Clear config when required fields are empty
            commandGenerator.configure(null);
            logger.atInfo().log("AI provider configuration cleared");
            return;
        }

        final var config = new LLMProviderConfig(provider != null ? provider : "Custom", baseUrl.trim(), apiKey.trim(),
                                                 model != null ? model.trim() : "");

        commandGenerator.configure(config);
        commandGenerator.setMode(GeneratorMode.AI_PROVIDER);
        modeCombo.setValue("AI Provider");
        logger.atInfo().log("AI provider configuration saved: %s", provider);
    }

    @FXML
    private void handleResetConfig() {
        if (commandGenerator == null) {
            return;
        }
        providerCombo.setValue(null);
        apiKeyInput.clear();
        baseUrlInput.clear();
        modelInput.clear();
        commandGenerator.configure(null);
        commandGenerator.setMode(GeneratorMode.SMART_MATCH);
        modeCombo.setValue("Smart Match");
        updateProviderConfigVisibility(GeneratorMode.SMART_MATCH);
        logger.atInfo().log("AI provider configuration reset");
    }

    private void setupKeyboardShortcuts() {
        rootSplitPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Ctrl+Enter -> Generate
            if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN).match(event)
                    && !new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
                            .match(event)) {
                handleGenerate();
                event.consume();
            }
            // Ctrl+Shift+Enter -> Execute All
            else if (new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN)
                    .match(event)) {
                handleExecuteAll();
                event.consume();
            }
            // Ctrl+E -> Execute Selection
            else if (new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN).match(event)) {
                handleExecuteSelection();
                event.consume();
            }
            // F8 -> Step
            else if (event.getCode() == KeyCode.F8) {
                handleStep();
                event.consume();
            }
            // Escape -> Clear
            else if (event.getCode() == KeyCode.ESCAPE) {
                handleClear();
                event.consume();
            }
        });
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

    // ============ AI Panel Handlers ============

    @FXML
    private void handleGenerate() {
        if (commandGenerator == null) {
            return;
        }
        final var prompt = promptInput.getText();
        if (prompt == null || prompt.trim().isEmpty()) {
            return;
        }
        generating.set(true);
        aiStatusLabel.setText("Generating...");
        aiStatusLabel.getStyleClass().removeAll("ready", "loading", "error");
        aiStatusLabel.getStyleClass().add("loading");

        final CompletableFuture<String> future = commandGenerator.generate(prompt);
        future.thenAccept(result -> Platform.runLater(() -> {
            stagingArea.setText(result);
            currentStatementIndex = 0;
            currentStatements     = GogoStatementSplitter.split(result);
            generating.set(false);
            updateAiStatus(GeneratorState.READY);
            logger.atInfo().log("AI generated commands for prompt: '%s'", prompt);
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                stagingArea.setText("# Error generating commands: " + ex.getMessage());
                generating.set(false);
                updateAiStatus(GeneratorState.ERROR);
                logger.atWarning().withException(ex).log("AI generation failed for prompt: '%s'", prompt);
            });
            return null;
        });
    }

    @FXML
    private void handleClear() {
        promptInput.clear();
        stagingArea.clear();
        currentStatementIndex = 0;
        currentStatements     = List.of();
    }

    @FXML
    private void handleExecuteAll() {
        final var commands = stagingArea.getText();
        if (commands == null || commands.trim().isEmpty()) {
            return;
        }
        output.appendText("$ " + commands + System.lineSeparator());
        executeGogoCommand(commands);
    }

    @FXML
    private void handleExecuteSelection() {
        final var selected = stagingArea.getSelectedText();
        if (selected == null || selected.trim().isEmpty()) {
            return;
        }
        output.appendText("$ " + selected + System.lineSeparator());
        executeGogoCommand(selected);
    }

    @FXML
    private void handleStep() {
        if (currentStatements.isEmpty()) {
            final var text = stagingArea.getText();
            if (text != null && !text.trim().isEmpty()) {
                currentStatements     = GogoStatementSplitter.split(text);
                currentStatementIndex = 0;
            }
        }
        if (currentStatementIndex >= currentStatements.size()) {
            return;
        }
        final var statement = currentStatements.get(currentStatementIndex);

        // Highlight current statement in staging area
        stagingArea.selectRange(statement.startOffset(), statement.endOffset());

        output.appendText("$ " + statement.text() + System.lineSeparator());

        final var          stmtIndex = currentStatementIndex;
        final Task<String> task      = new Task<>() {
                                         @Override
                                         protected String call() throws Exception {
                                             String outputText;
                                             try {
                                                 final var currentAgent = supervisor != null ? supervisor.getAgent()
                                                         : null;
                                                 if (isSnapshotAgent) {
                                                     outputText = "You cannot execute command in snapshot agent mode";
                                                 } else if (currentAgent == null || (outputText = currentAgent
                                                         .execGogoCommand(statement.text())) == null) {
                                                     outputText = "Agent is not connected";
                                                 }
                                             } catch (final Exception e) {
                                                 outputText = Throwables.getStackTraceAsString(e);
                                                 throw e;
                                             }
                                             return outputText;
                                         }
                                     };
        task.setOnSucceeded(_ -> {
            output.appendText(task.getValue());
            output.appendText(System.lineSeparator());
            currentStatementIndex = stmtIndex + 1;
            logger.atInfo().log("Step %d executed successfully", stmtIndex + 1);
        });
        task.setOnFailed(_ -> {
            output.appendText("Step failed: " + task.getException().getMessage() + System.lineSeparator());
            logger.atWarning().log("Step %d failed", stmtIndex + 1);
        });
        executor.runAsync(task);
    }

    @FXML
    private void handleReset() {
        currentStatementIndex = 0;
        final var text = stagingArea.getText();
        if (text != null && !text.trim().isEmpty()) {
            currentStatements = GogoStatementSplitter.split(text);
        }
    }

    @FXML
    private void handleCopy() {
        final var commands = stagingArea.getText();
        if (commands == null || commands.trim().isEmpty()) {
            return;
        }
        final var content = new ClipboardContent();
        content.putString(commands);
        Clipboard.getSystemClipboard().setContent(content);
    }

    // ============ Command Execution ============

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
