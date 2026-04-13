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
package com.osgifx.console.ai.provider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.osgifx.console.ai.GeneratorMode;
import com.osgifx.console.ai.GeneratorState;
import com.osgifx.console.ai.GeneratorStateListener;
import com.osgifx.console.ai.GogoCommandGenerator;
import com.osgifx.console.ai.GogoExampleRetriever;
import com.osgifx.console.ai.LLMProviderConfig;
import com.osgifx.console.constants.FxConstants;

@Component
public final class GogoCommandGeneratorProvider implements GogoCommandGenerator {

    private static final int    MAX_EXAMPLES    = 5;
    private static final String CONFIG_FILENAME = "llm-provider.properties";
    private static final String AI_DIR          = "ai";

    private static final String SYSTEM_PROMPT_PREFIX = """
            You are an Apache Felix Gogo shell command expert.
            Given a user request, output ONLY the exact gogo command(s) to run.
            Adapt parameters from the reference examples to match the user's actual values.
            Output the command(s) only - no explanations, no markdown.

            Reference examples:
            """;

    @Reference
    private GogoExampleRetriever exampleRetriever;

    @Reference
    private LoggerFactory loggerFactory;

    private FluentLogger                       logger;
    private final List<GeneratorStateListener> listeners    = new CopyOnWriteArrayList<>();
    private volatile GeneratorState            currentState = GeneratorState.READY;
    private volatile GeneratorMode             currentMode  = GeneratorMode.SMART_MATCH;
    private volatile LLMProviderConfig         providerConfig;
    private Path                               configFile;
    private HttpClient                         httpClient;

    @Activate
    void activate() {
        logger     = FluentLogger.of(loggerFactory.createLogger(getClass().getName()));
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        final var  wsDir = System.getProperty(FxConstants.WORKSPACE_PROPERTY);
        final Path aiDir;
        if (wsDir != null && !wsDir.isBlank()) {
            aiDir = Path.of(wsDir, AI_DIR);
        } else {
            aiDir = Path.of(System.getProperty("user.home"), ".osgifx", AI_DIR);
        }
        configFile = aiDir.resolve(CONFIG_FILENAME);

        loadPersistedConfig();

        logger.atInfo().log("GogoCommandGenerator activated in %s mode", currentMode);
    }

    @Deactivate
    void deactivate() {
        listeners.clear();
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

    @Override
    public CompletableFuture<String> generate(final String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return CompletableFuture.completedFuture("");
        }
        return switch (currentMode) {
            case SMART_MATCH -> generateSmartMatch(prompt);
            case AI_PROVIDER -> generateWithLLM(prompt);
        };
    }

    private CompletableFuture<String> generateSmartMatch(final String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            final var examples = exampleRetriever.retrieve(prompt, 1);
            if (examples.isEmpty()) {
                return "";
            }
            return examples.getFirst().commands();
        });
    }

    private CompletableFuture<String> generateWithLLM(final String prompt) {
        final var config = providerConfig;
        if (config == null || config.apiKey() == null || config.apiKey().isBlank()) {
            return CompletableFuture.completedFuture("# AI Provider not configured. Please set API key.");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                final var examples      = exampleRetriever.retrieve(prompt, MAX_EXAMPLES);
                final var examplesBlock = examples.stream().map(e -> "Q: " + e.intent() + "\nA: " + e.commands())
                        .collect(Collectors.joining("\n"));
                final var systemPrompt  = SYSTEM_PROMPT_PREFIX + examplesBlock;
                return callLLMApi(config, systemPrompt, prompt);
            } catch (final Exception e) {
                logger.atError().withException(e).log("LLM generation failed");
                setState(GeneratorState.ERROR);
                return "# Error: " + e.getMessage();
            }
        });
    }

    private String callLLMApi(final LLMProviderConfig config,
                              final String systemPrompt,
                              final String userPrompt) throws IOException, InterruptedException {

        final var url         = config.baseUrl().replaceAll("/+$", "") + "/chat/completions";
        final var requestBody = """
                {
                    "model": "%s",
                    "messages": [
                        {"role": "system", "content": %s},
                        {"role": "user", "content": %s}
                    ],
                    "temperature": 0.3,
                    "max_tokens": 512
                }
                """.formatted(escapeJson(config.modelName()), jsonString(systemPrompt), jsonString(userPrompt));

        final var request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)).timeout(Duration.ofSeconds(60)).build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.atError().log("LLM API error: HTTP %d - %s", response.statusCode(), response.body());
            throw new IOException("LLM API error: HTTP " + response.statusCode());
        }

        return extractContent(response.body());
    }

    private String extractContent(final String jsonResponse) {
        final var idx = jsonResponse.indexOf("\"content\"");
        if (idx < 0) {
            return jsonResponse;
        }
        final var colonIdx = jsonResponse.indexOf(':', idx + 9);
        if (colonIdx < 0) {
            return jsonResponse;
        }
        final var openQuote = jsonResponse.indexOf('"', colonIdx + 1);
        if (openQuote < 0) {
            return jsonResponse;
        }
        final var sb = new StringBuilder();
        for (var i = openQuote + 1; i < jsonResponse.length(); i++) {
            final var c = jsonResponse.charAt(i);
            if (c == '\\' && i + 1 < jsonResponse.length()) {
                final var next = jsonResponse.charAt(i + 1);
                switch (next) {
                    case '"' -> {
                        sb.append('"');
                        i++;
                    }
                    case '\\' -> {
                        sb.append('\\');
                        i++;
                    }
                    case 'n' -> {
                        sb.append('\n');
                        i++;
                    }
                    case 't' -> {
                        sb.append('\t');
                        i++;
                    }
                    case 'r' -> {
                        sb.append('\r');
                        i++;
                    }
                    default -> sb.append(c);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return cleanResponse(sb.toString());
    }

    private String cleanResponse(final String response) {
        if (response == null || response.isBlank()) {
            return "";
        }
        var cleaned = response.strip();

        if (cleaned.startsWith("```")) {
            final var endFence = cleaned.indexOf('\n');
            if (endFence >= 0) {
                cleaned = cleaned.substring(endFence + 1);
            }
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        cleaned = cleaned.replaceFirst("(?i)^(commands?|answer|output|result|a)\\s*:\\s*", "");

        return cleaned.strip();
    }

    private String escapeJson(final String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
                "\\t");
    }

    private String jsonString(final String s) {
        return "\"" + escapeJson(s) + "\"";
    }

    @Override
    public void addStateListener(final GeneratorStateListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeStateListener(final GeneratorStateListener listener) {
        listeners.remove(listener);
    }

    @Override
    public GeneratorState currentState() {
        return currentState;
    }

    @Override
    public GeneratorMode currentMode() {
        return currentMode;
    }

    @Override
    public void setMode(final GeneratorMode mode) {
        currentMode = mode;
        if (mode == GeneratorMode.SMART_MATCH) {
            setState(GeneratorState.READY);
        } else {
            if (providerConfig != null && providerConfig.apiKey() != null && !providerConfig.apiKey().isBlank()) {
                setState(GeneratorState.READY);
            } else {
                setState(GeneratorState.NOT_CONFIGURED);
            }
        }
        persistState();
        logger.atInfo().log("Generator mode changed to %s", mode);
    }

    @Override
    public void configure(final LLMProviderConfig config) {
        providerConfig = config;
        if (currentMode == GeneratorMode.AI_PROVIDER) {
            if (config != null && config.apiKey() != null && !config.apiKey().isBlank()) {
                setState(GeneratorState.READY);
            } else {
                setState(GeneratorState.NOT_CONFIGURED);
            }
        }
        persistState();
        logger.atInfo().log("LLM provider configured: %s", config != null ? config.providerName() : "cleared");
    }

    @Override
    public LLMProviderConfig currentConfig() {
        return providerConfig;
    }

    private void setState(final GeneratorState state) {
        currentState = state;
        for (final var listener : listeners) {
            listener.onStateChanged(state);
        }
    }

    private void persistState() {
        try {
            Files.createDirectories(configFile.getParent());
            final var props = new Properties();
            props.setProperty("mode", currentMode.name());
            final var config = providerConfig;
            if (config != null) {
                props.setProperty("provider", config.providerName() != null ? config.providerName() : "");
                props.setProperty("baseUrl", config.baseUrl() != null ? config.baseUrl() : "");
                props.setProperty("apiKey", config.apiKey() != null ? config.apiKey() : "");
                props.setProperty("model", config.modelName() != null ? config.modelName() : "");
            }
            try (final var out = Files.newOutputStream(configFile)) {
                props.store(out, "OSGi.fx AI Provider Configuration");
            }
        } catch (final IOException e) {
            logger.atError().withException(e).log("Failed to persist AI provider config");
        }
    }

    private void loadPersistedConfig() {
        if (!Files.exists(configFile)) {
            return;
        }
        try {
            final var props = new Properties();
            try (final var in = Files.newInputStream(configFile)) {
                props.load(in);
            }
            final var mode     = props.getProperty("mode", "SMART_MATCH");
            final var provider = props.getProperty("provider", "");
            final var baseUrl  = props.getProperty("baseUrl", "");
            final var apiKey   = props.getProperty("apiKey", "");
            final var model    = props.getProperty("model", "");

            try {
                currentMode = GeneratorMode.valueOf(mode);
            } catch (final IllegalArgumentException ignored) {
                currentMode = GeneratorMode.SMART_MATCH;
            }

            if (!baseUrl.isBlank() && !apiKey.isBlank()) {
                providerConfig = new LLMProviderConfig(provider, baseUrl, apiKey, model);
            }

            if (currentMode == GeneratorMode.AI_PROVIDER && providerConfig == null) {
                currentMode  = GeneratorMode.SMART_MATCH;
                currentState = GeneratorState.READY;
            } else if (currentMode == GeneratorMode.AI_PROVIDER) {
                currentState = GeneratorState.READY;
            } else {
                currentState = GeneratorState.READY;
            }

            logger.atInfo().log("Loaded persisted config: mode=%s, provider=%s", currentMode, provider);
        } catch (final IOException e) {
            logger.atWarning().withException(e).log("Failed to load AI provider config");
        }
    }

}
