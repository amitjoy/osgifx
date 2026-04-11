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
package com.osgifx.console.ui.graph;

import static com.osgifx.console.constants.FxConstants.WORKSPACE_PROPERTY;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Manages loading, saving, and applying abbreviation rules for graph labels.
 * <p>
 * Rules are persisted as a JSON file ({@code graph-abbreviations.json}) inside
 * the OSGi.fx workspace directory.
 */
public final class AbbreviationSettings {

    private static final Path SETTINGS_FILE = Paths.get(System.getProperty(WORKSPACE_PROPERTY),
            "graph-abbreviations.json");

    private List<AbbreviationRule> rules;

    public AbbreviationSettings() {
        rules = load();
    }

    /**
     * Returns an unmodifiable view of the current abbreviation rules.
     */
    public List<AbbreviationRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    /**
     * Replaces the current rules and persists them to disk.
     */
    public void setRules(final List<AbbreviationRule> newRules) {
        rules = new ArrayList<>(newRules);
        save(rules);
    }

    /**
     * Applies all abbreviation rules to the given label. Rules are applied in
     * order; only the first matching prefix is replaced.
     *
     * @param label the original label
     * @return the abbreviated label, or the original if no rule matches
     */
    public String applyAbbreviations(final String label) {
        if (label == null || rules.isEmpty()) {
            return label;
        }
        for (final AbbreviationRule rule : rules) {
            if (rule.prefix() != null && !rule.prefix().isEmpty() && label.startsWith(rule.prefix())) {
                final var replacement = rule.replacement() != null ? rule.replacement() : "";
                return replacement + label.substring(rule.prefix().length());
            }
        }
        return label;
    }

    private List<AbbreviationRule> load() {
        if (!Files.exists(SETTINGS_FILE)) {
            return new ArrayList<>();
        }
        try (var reader = Files.newBufferedReader(SETTINGS_FILE, StandardCharsets.UTF_8)) {
            final var                    type   = new TypeToken<List<AbbreviationRule>>() {
                                                }.getType();
            final List<AbbreviationRule> loaded = createGson().fromJson(reader, type);
            return loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
        } catch (final Exception e) {
            return new ArrayList<>();
        }
    }

    private void save(final List<AbbreviationRule> rulesToSave) {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            try (var writer = Files.newBufferedWriter(SETTINGS_FILE, StandardCharsets.UTF_8)) {
                createGson().toJson(rulesToSave, writer);
            }
        } catch (final Exception e) {
            // silently ignore — best effort persistence
        }
    }

    private static Gson createGson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

}
