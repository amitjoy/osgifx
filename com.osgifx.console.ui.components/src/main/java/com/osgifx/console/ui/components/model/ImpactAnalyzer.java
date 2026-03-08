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
package com.osgifx.console.ui.components.model;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XComponentDTO;

public final class ImpactAnalyzer {

    private ImpactAnalyzer() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    private static boolean isMatch(final String interface1, final String interface2) {
        if (interface1 == null || interface2 == null) {
            return false;
        }
        final var parts1 = interface1.split(",");
        final var parts2 = interface2.split(",");
        for (final var p1 : parts1) {
            for (final var p2 : parts2) {
                if (p1.trim().equals(p2.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesAny(final String target, final Collection<String> interfaces) {
        if (target == null || interfaces == null) {
            return false;
        }
        return interfaces.stream().anyMatch(i -> isMatch(target, i));
    }

    public static List<XImpactDTO> calculateDisableImpact(final Collection<XComponentDTO> componentsToDisable,
                                                          final Collection<XComponentDTO> allComponents,
                                                          final Collection<XBundleDTO> allBundles) {
        final List<XImpactDTO> impacts                = new ArrayList<>();
        final Set<Long>        toDisableIds           = componentsToDisable.stream().map(c -> c.id).collect(toSet());
        final Set<String>      alreadyAddedComponents = new HashSet<>();

        final Set<String> registeredServiceInterfaces = componentsToDisable.stream()
                .filter(c -> c.serviceInterfaces != null).flatMap(c -> c.serviceInterfaces.stream()).collect(toSet());

        for (final XComponentDTO component : allComponents) {
            // Skip components in the disabling list or already added
            if (toDisableIds.contains(component.id) || alreadyAddedComponents.contains(component.name)) {
                continue;
            }

            if (component.satisfiedReferences != null) {
                for (final var ref : component.satisfiedReferences) {
                    if (matchesAny(ref.objectClass, registeredServiceInterfaces)) {
                        final var refDef = component.references.stream().filter(r -> r.name.equals(ref.name))
                                .findFirst();

                        if (refDef.isPresent()) {
                            final String  cardinality = refDef.get().cardinality;
                            final boolean isMandatory = cardinality.startsWith("1");

                            if (isMandatory) {
                                // Check if there are other providers for this mandatory service
                                final long otherProvidersCount = allComponents.stream()
                                        .filter(c -> !alreadyAddedComponents.contains(c.name))
                                        .filter(c -> !toDisableIds.contains(c.id))
                                        .filter(c -> c.state.equals("SATISFIED") || c.state.equals("ACTIVE"))
                                        .filter(c -> c.serviceInterfaces != null
                                                && matchesAny(ref.objectClass, c.serviceInterfaces))
                                        .count();

                                // Also check bundles
                                final long otherBundleProvidersCount = allBundles == null ? 0
                                        : allBundles.stream().filter(b -> b.registeredServices != null)
                                                .filter(b -> b.registeredServices.stream()
                                                        .anyMatch(s -> isMatch(ref.objectClass, s.objectClass)))
                                                .count();

                                if (otherProvidersCount + otherBundleProvidersCount == 0) {
                                    impacts.add(
                                            new XImpactDTO(component.name, "DEACTIVATION",
                                                           "Mandatory service reference '" + ref.name
                                                                   + "' will be unsatisfied, leading to component deactivation."));
                                    alreadyAddedComponents.add(component.name);
                                } else {
                                    impacts.add(new XImpactDTO(component.name, "SERVICE_UNBIND",
                                                               "One of the providers for mandatory service reference '"
                                                                       + ref.name + "' will be gone."));
                                    alreadyAddedComponents.add(component.name);
                                }
                            } else {
                                impacts.add(
                                        new XImpactDTO(component.name, "SERVICE_UNBIND", "Optional service reference '"
                                                + ref.name + "' will be unbound."));
                                alreadyAddedComponents.add(component.name);
                            }
                        }
                    }
                }
            }
        }
        return impacts;
    }

    public static List<XImpactDTO> calculateEnableImpact(final Collection<XComponentDTO> componentsToEnable,
                                                         final Collection<XComponentDTO> allComponents,
                                                         final Collection<XBundleDTO> allBundles) {
        final List<XImpactDTO> impacts                = new ArrayList<>();
        final Set<Long>        toEnableIds            = componentsToEnable.stream().map(c -> c.id).collect(toSet());
        final Set<String>      alreadyAddedComponents = new HashSet<>();

        final Set<String> servicesToBeRegistered = componentsToEnable.stream().filter(c -> c.serviceInterfaces != null)
                .flatMap(c -> c.serviceInterfaces.stream()).collect(toSet());

        for (final XComponentDTO component : allComponents) {
            // Skip components in the enabling list or already added
            if (toEnableIds.contains(component.id) || alreadyAddedComponents.contains(component.name)) {
                continue;
            }

            if (component.references == null) {
                continue;
            }

            boolean isAffected = false;
            for (final var refDef : component.references) {
                if (refDef.interfaceName != null && matchesAny(refDef.interfaceName, servicesToBeRegistered)) {
                    isAffected = true;
                    break;
                }
            }

            if (isAffected) {
                final boolean isNotActive = !"ACTIVE".equalsIgnoreCase(component.state)
                        && !"SATISFIED".equalsIgnoreCase(component.state);

                String impactType  = "DEPENDENCY";
                String description = "Component will bind to additional service providers once available.";

                if (isNotActive) {
                    impactType  = "SATISFACTION";
                    description = "New service availability may satisfy this component's references.";
                }
                impacts.add(new XImpactDTO(component.name, impactType, description));
                alreadyAddedComponents.add(component.name);
            }
        }
        return impacts;
    }

}
