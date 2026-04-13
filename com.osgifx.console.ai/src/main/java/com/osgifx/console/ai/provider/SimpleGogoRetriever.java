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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.osgifx.console.ai.GogoExample;
import com.osgifx.console.ai.GogoExampleRetriever;

@Component
public final class SimpleGogoRetriever implements GogoExampleRetriever {

    // BM25 parameters
    private static final double K1 = 1.2;
    private static final double B  = 0.75;

    private static final Map<String, String> SYNONYMS = Map.ofEntries(Map.entry("bundle", "bundle"),
            Map.entry("bundles", "bundle"), Map.entry("jar", "bundle"), Map.entry("jars", "bundle"),
            Map.entry("component", "component"), Map.entry("components", "component"), Map.entry("ds", "component"),
            Map.entry("scr", "component"), Map.entry("service", "service"), Map.entry("services", "service"),
            Map.entry("config", "configuration"), Map.entry("configs", "configuration"),
            Map.entry("configuration", "configuration"), Map.entry("configurations", "configuration"),
            Map.entry("pid", "pid"), Map.entry("pids", "pid"), Map.entry("property", "property"),
            Map.entry("properties", "property"), Map.entry("package", "package"), Map.entry("packages", "package"),
            Map.entry("log", "log"), Map.entry("logs", "log"), Map.entry("capability", "capability"),
            Map.entry("capabilities", "capability"), Map.entry("requirement", "requirement"),
            Map.entry("requirements", "requirement"), Map.entry("start", "start"), Map.entry("begin", "start"),
            Map.entry("launch", "start"), Map.entry("stop", "stop"), Map.entry("halt", "stop"),
            Map.entry("kill", "stop"), Map.entry("install", "install"), Map.entry("deploy", "install"),
            Map.entry("uninstall", "uninstall"), Map.entry("remove", "uninstall"), Map.entry("delete", "uninstall"),
            Map.entry("list", "query"), Map.entry("show", "query"), Map.entry("display", "query"),
            Map.entry("find", "query"), Map.entry("search", "query"), Map.entry("discover", "query"),
            Map.entry("grep", "grep"), Map.entry("refresh", "refresh"), Map.entry("reload", "refresh"),
            Map.entry("restart", "restart"), Map.entry("update", "update"), Map.entry("resolve", "resolve"),
            Map.entry("diagnose", "diagnose"), Map.entry("diag", "diagnose"), Map.entry("debug", "diagnose"),
            Map.entry("inspect", "inspect"), Map.entry("examine", "inspect"), Map.entry("env", "env"),
            Map.entry("environment", "env"), Map.entry("memory", "memory"), Map.entry("gc", "gc"),
            Map.entry("garbage", "gc"), Map.entry("header", "header"), Map.entry("headers", "header"),
            Map.entry("variable", "variable"), Map.entry("var", "variable"), Map.entry("enable", "enable"),
            Map.entry("disable", "disable"));

    private static final Set<String> STOP_WORDS = Set.of("a", "an", "the", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should", "may", "might",
            "shall", "can", "to", "of", "in", "for", "on", "with", "at", "by", "from", "as", "into", "through",
            "during", "before", "after", "and", "but", "or", "nor", "not", "so", "yet", "both", "either", "neither",
            "each", "every", "all", "any", "few", "more", "most", "other", "some", "such", "no", "only", "same", "than",
            "too", "very", "just", "about", "above", "below", "between", "up", "down", "out", "off", "over", "under",
            "again", "further", "then", "once", "here", "there", "when", "where", "why", "how", "what", "which", "who",
            "whom", "this", "that", "these", "those", "i", "me", "my", "we", "our", "you", "your", "he", "him", "his",
            "she", "her", "it", "its", "they", "them", "their");

    private final List<GogoExample> examples = new ArrayList<>();
    private List<List<String>>      docTokens;                   // pre-tokenized + normalized tokens per example
    private Map<String, Double>     idfCache;                    // term -> IDF score
    private double                  avgDocLength;                // average document length for BM25

    @Activate
    void activate() {
        loadExamples();
        buildIndex();
    }

    private void loadExamples() {
        try (final var is = getClass().getClassLoader().getResourceAsStream("resources/gogo-examples.txt");
                final var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String    line;
            String    currentPrompt = null;
            final var commandLines  = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines between entries
                if (line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("P: ")) {
                    // Save previous entry
                    if (currentPrompt != null && !commandLines.isEmpty()) {
                        examples.add(new GogoExample(currentPrompt, commandLines.toString().trim()));
                    }
                    currentPrompt = line.substring(3).trim();
                    commandLines.setLength(0);
                } else if (line.startsWith("A: ")) {
                    commandLines.append(line.substring(3).trim());
                } else if (!line.isEmpty() && currentPrompt != null) {
                    // Continuation line for multi-line commands
                    commandLines.append("\n").append(line);
                }
            }
            // Save last entry
            if (currentPrompt != null && !commandLines.isEmpty()) {
                examples.add(new GogoExample(currentPrompt, commandLines.toString().trim()));
            }

        } catch (final IOException e) {
            throw new RuntimeException("Failed to load gogo-examples.txt", e);
        }
    }

    private void buildIndex() {
        final var n = examples.size();

        // Pre-tokenize and normalize all documents
        docTokens = new ArrayList<>(n);
        for (final var example : examples) {
            docTokens.add(normalize(tokenize(example.intent())));
        }

        // Compute document frequency for each term
        final var df = new HashMap<String, Integer>();
        for (final var tokens : docTokens) {
            // Count each term once per document (use distinct set)
            tokens.stream().distinct().forEach(t -> df.merge(t, 1, Integer::sum));
        }

        // Compute IDF: ln((N - df + 0.5) / (df + 0.5) + 1)
        idfCache = new HashMap<>();
        for (final var entry : df.entrySet()) {
            final var term     = entry.getKey();
            final var docFreq  = entry.getValue();
            final var idfScore = Math.log((n - docFreq + 0.5) / (docFreq + 0.5) + 1.0);
            idfCache.put(term, idfScore);
        }

        // Compute average document length
        var totalLength = 0;
        for (final var tokens : docTokens) {
            totalLength += tokens.size();
        }
        avgDocLength = n > 0 ? (double) totalLength / n : 1.0;
    }

    @Override
    public List<GogoExample> retrieve(final String prompt, final int maxResults) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return List.of();
        }

        final var queryTerms = normalize(tokenize(prompt));
        if (queryTerms.isEmpty()) {
            return List.of();
        }

        final var scored = new HashMap<GogoExample, Double>();

        for (var i = 0; i < examples.size(); i++) {
            final var tokens = docTokens.get(i);
            final var docLen = tokens.size();

            // Count term frequencies in this document
            final var tf = new HashMap<String, Integer>();
            for (final var t : tokens) {
                tf.merge(t, 1, Integer::sum);
            }

            // BM25 score: sum over query terms
            var score = 0.0;
            for (final var qt : queryTerms) {
                final var freq = tf.getOrDefault(qt, 0);
                if (freq == 0) {
                    // Check for substring/partial match — count as fractional hit
                    var partialHit = false;
                    for (final var dt : tf.keySet()) {
                        if (dt.contains(qt) || qt.contains(dt)) {
                            partialHit = true;
                            break;
                        }
                    }
                    if (!partialHit) {
                        continue;
                    }
                    // Treat partial match as tf=0.5
                    final var idf    = idfCache.getOrDefault(qt, idfForUnseen());
                    final var tfNorm = (0.5 * (K1 + 1.0)) / (0.5 + K1 * (1.0 - B + B * docLen / avgDocLength));
                    score += idf * tfNorm;
                    continue;
                }

                final var idf    = idfCache.getOrDefault(qt, idfForUnseen());
                final var tfNorm = (freq * (K1 + 1.0)) / (freq + K1 * (1.0 - B + B * docLen / avgDocLength));
                score += idf * tfNorm;
            }

            if (score > 0) {
                scored.put(examples.get(i), score);
            }
        }

        return scored.entrySet().stream()
                .sorted(Comparator.<Map.Entry<GogoExample, Double>, Double> comparing(Map.Entry::getValue).reversed())
                .limit(maxResults).map(Map.Entry::getKey).toList();
    }

    private double idfForUnseen() {
        // IDF for a term not in the corpus — treat as appearing in 0 documents
        final var n = examples.size();
        return Math.log((n + 0.5) / 0.5 + 1.0);
    }

    private List<String> tokenize(final String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[\\s,;.!?()\\[\\]{}|/\\\\]+"))
                .filter(s -> !s.isEmpty()).filter(s -> !STOP_WORDS.contains(s)).toList();
    }

    private List<String> normalize(final List<String> tokens) {
        return tokens.stream().map(t -> SYNONYMS.getOrDefault(t, t)).distinct().toList();
    }

}
