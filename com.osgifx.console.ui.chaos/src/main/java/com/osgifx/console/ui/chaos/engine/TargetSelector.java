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
package com.osgifx.console.ui.chaos.engine;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.data.provider.DataProvider;

public final class TargetSelector {

    private TargetSelector() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<Victim> selectVictims(final ChaosConfig config,
                                             final DataProvider dataProvider,
                                             final Set<String> activeVictimIds) {
        return selectVictims(config, dataProvider, activeVictimIds, true);
    }

    public static List<Victim> selectVictims(final ChaosConfig config,
                                             final DataProvider dataProvider,
                                             final Set<String> activeVictimIds,
                                             final boolean applyConcurrencyLimit) {
        final List<Victim> eligibleTargets = new java.util.ArrayList<>();

        if (config.targetType == ChaosConfig.TargetType.BUNDLES || config.targetType == ChaosConfig.TargetType.BOTH) {
            final List<XBundleDTO> bundles = dataProvider.bundles();
            eligibleTargets.addAll(bundles.stream().filter(b -> isEligibleBundle(b, config))
                    .filter(b -> !activeVictimIds.contains(String.valueOf(b.id)))
                    .map(b -> new Victim(String.valueOf(b.id), b.symbolicName, ChaosEvent.TargetType.BUNDLE)).toList());
        }

        if (config.targetType == ChaosConfig.TargetType.COMPONENTS
                || config.targetType == ChaosConfig.TargetType.BOTH) {
            final List<XComponentDTO> components = dataProvider.components();
            eligibleTargets.addAll(components.stream().filter(c -> isEligibleComponent(c, config))
                    .filter(c -> !activeVictimIds.contains(String.valueOf(c.id)))
                    .map(c -> new Victim(String.valueOf(c.id), c.name, ChaosEvent.TargetType.COMPONENT)).toList());
        }

        Collections.shuffle(eligibleTargets);
        if (applyConcurrencyLimit) {
            return eligibleTargets.stream().limit(config.concurrency).toList();
        }
        return eligibleTargets;
    }

    private static boolean isEligibleBundle(final XBundleDTO bundle, final ChaosConfig config) {
        if (bundle.id == 0) {
            return false;
        }
        final String bsn = bundle.symbolicName;
        if (bsn.startsWith("org.osgi.") || bsn.startsWith("org.apache.felix.") || bsn.startsWith("org.eclipse.")
                || bsn.startsWith("com.osgifx.")) {
            return false;
        }
        return applyUserFilters(bsn, config);
    }

    private static boolean isEligibleComponent(final XComponentDTO component, final ChaosConfig config) {
        final String name = component.name;
        if (name.startsWith("org.osgi.") || name.startsWith("org.apache.felix.") || name.startsWith("org.eclipse.")
                || name.startsWith("com.osgifx.")) {
            return false;
        }
        return applyUserFilters(name, config);
    }

    private static String globToRegex(final String glob) {
        final StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            final char c = glob.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                case '[':
                case ']':
                case '{':
                case '}':
                case '\\':
                    regex.append('\\').append(c);
                    break;
                default:
                    regex.append(c);
            }
        }
        regex.append("$");
        return regex.toString();
    }

    private static boolean applyUserFilters(final String name, final ChaosConfig config) {
        if (config.inclusionFilter != null && !config.inclusionFilter.isEmpty()) {
            final String[] patterns = config.inclusionFilter.split(",");
            boolean        matched  = false;
            for (final String pattern : patterns) {
                try {
                    final String regex = globToRegex(pattern.trim());
                    if (Pattern.compile(regex).matcher(name).matches()) {
                        matched = true;
                        break;
                    }
                } catch (final Exception e) {
                    // Ignore if invalid pattern
                }
            }
            if (!matched) {
                return false;
            }
        }
        if (config.exclusionFilter != null && !config.exclusionFilter.isEmpty()) {
            final String[] patterns = config.exclusionFilter.split(",");
            for (final String pattern : patterns) {
                try {
                    final String regex = globToRegex(pattern.trim());
                    if (Pattern.compile(regex).matcher(name).matches()) {
                        return false;
                    }
                } catch (final Exception e) {
                    // Ignore if invalid pattern
                }
            }
        }
        return true;
    }

    public static class Victim {
        public String                id;
        public String                name;
        public ChaosEvent.TargetType type;

        public Victim(String id, String name, ChaosEvent.TargetType type) {
            this.id   = id;
            this.name = name;
            this.type = type;
        }
    }

}
