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
package com.osgifx.console.ui.bundles.model;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

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

    public static List<XImpactDTO> calculateStartImpact(final Collection<XBundleDTO> bundlesToStart,
                                                        final Collection<XBundleDTO> allBundles,
                                                        final Collection<XComponentDTO> allComponents) {
        final List<XImpactDTO>               impacts      = new ArrayList<>();
        final Graph<XBundleDTO, DefaultEdge> graph        = buildBundleDependencyGraph(allBundles);
        final Set<Long>                      toStartIds   = bundlesToStart.stream().map(b -> b.id).collect(toSet());
        final Set<String>                    alreadyAdded = bundlesToStart.stream().map(b -> b.symbolicName)
                .collect(toSet());

        final Set<String> alreadyAddedComponents = new HashSet<>();

        for (final XBundleDTO bundle : bundlesToStart) {
            // 1. Trace Package Dependencies (WIRING)
            final BreadthFirstIterator<XBundleDTO, DefaultEdge> bfs = new BreadthFirstIterator<>(graph, bundle);
            while (bfs.hasNext()) {
                final XBundleDTO affected = bfs.next();
                if (!toStartIds.contains(affected.id) && !alreadyAdded.contains(affected.symbolicName)) {
                    final boolean isNotActive = !"ACTIVE".equalsIgnoreCase(affected.state)
                            && !"STARTING".equalsIgnoreCase(affected.state);
                    if (isNotActive) {
                        impacts.add(new XImpactDTO(affected.symbolicName, "ACTIVATION",
                                                   "Permits activation once dependencies are available."));
                    } else {
                        impacts.add(new XImpactDTO(affected.symbolicName, "DEPENDENCY",
                                                   "Static wiring to this provider is already established."));
                    }
                    alreadyAdded.add(affected.symbolicName);
                }
            }
        }

        // 2. Trace SCR Component Dependencies
        if (allComponents != null) {
            final Set<String> servicesToBeRegistered = new HashSet<>();

            // 1. Services from starting components
            final Set<String> componentServices = allComponents.stream()
                    .filter(c -> toStartIds.contains(c.registeringBundleId)).filter(c -> c.serviceInterfaces != null)
                    .flatMap(c -> c.serviceInterfaces.stream()).collect(toSet());
            servicesToBeRegistered.addAll(componentServices);

            // 2. Services from starting bundles themselves
            final Set<String> bundleServices = bundlesToStart.stream().filter(b -> b.registeredServices != null)
                    .flatMap(b -> b.registeredServices.stream()).map(s -> s.objectClass).collect(toSet());
            servicesToBeRegistered.addAll(bundleServices);

            for (final XComponentDTO component : allComponents) {
                // Skip components already in the starting bundles or already added
                if (toStartIds.contains(component.registeringBundleId)
                        || alreadyAddedComponents.contains(component.name)) {
                    if (toStartIds.contains(component.registeringBundleId)) {
                        impacts.add(new XImpactDTO(component.name, "ACTIVATION",
                                                   "Component will be activated as its hosting bundle is starting."));
                        alreadyAddedComponents.add(component.name);
                    }
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
        }
        return impacts;
    }

    public static List<XImpactDTO> calculateStopImpact(final String action,
                                                       final Collection<XBundleDTO> bundlesToStop,
                                                       final Collection<XBundleDTO> allBundles,
                                                       final Collection<XComponentDTO> allComponents) {
        final List<XImpactDTO>               impacts = new ArrayList<>();
        final Graph<XBundleDTO, DefaultEdge> graph   = buildBundleDependencyGraph(allBundles);

        final Set<Long>   bundlesToStopIds       = bundlesToStop.stream().map(b -> b.id).collect(toSet());
        final Set<String> alreadyAddedComponents = new HashSet<>();

        for (final XBundleDTO bundle : bundlesToStop) {
            // 1. Trace Package Dependencies (WIRING)
            final BreadthFirstIterator<XBundleDTO, DefaultEdge> bfs = new BreadthFirstIterator<>(graph, bundle);
            while (bfs.hasNext()) {
                final XBundleDTO affected = bfs.next();
                if (!bundlesToStopIds.contains(affected.id)) {
                    if ("UNINSTALL".equalsIgnoreCase(action)) {
                        impacts.add(
                                new XImpactDTO(affected.symbolicName, "STALE_WIRING",
                                               "Wirings will remain 'stale' after uninstallation until a Package Refresh is performed."));
                    } else {
                        impacts.add(
                                new XImpactDTO(affected.symbolicName, "DEPENDENCY",
                                               "Static wiring persists in memory even after the provider bundle is stopped."));
                    }
                }
            }
            // 2. Trace Service Dependencies (BINDING)
            if (bundle.registeredServices != null) {
                final Set<Long>   registeredServiceIds        = bundle.registeredServices.stream().map(s -> s.id)
                        .collect(toSet());
                final Set<String> registeredServiceInterfaces = bundle.registeredServices.stream()
                        .map(s -> s.objectClass).collect(toSet());

                for (final XBundleDTO consumer : allBundles) {
                    if (bundlesToStopIds.contains(consumer.id) || consumer.usedServices == null) {
                        continue;
                    }
                    final boolean usesService = consumer.usedServices.stream()
                            .anyMatch(s -> registeredServiceIds.contains(s.id));
                    if (usesService) {
                        impacts.add(
                                new XImpactDTO(consumer.symbolicName, "SERVICE_UNBIND",
                                               "Service consumers will be unbound immediately upon stopping the target."));
                    }
                }
                // 3. Trace SCR Component Dependencies
                if (allComponents != null) {
                    final Set<String> bundlesToStopSn = bundlesToStop.stream().map(b -> b.symbolicName)
                            .collect(toSet());
                    for (final XComponentDTO component : allComponents) {
                        // Skip components in the stopping bundles or already added
                        if (bundlesToStopIds.contains(component.registeringBundleId)
                                || alreadyAddedComponents.contains(component.name)) {
                            continue;
                        }

                        if (component.satisfiedReferences != null) {
                            for (final var ref : component.satisfiedReferences) {
                                if (matchesAny(ref.objectClass, registeredServiceInterfaces)) {
                                    final var refDef = component.references.stream()
                                            .filter(r -> r.name.equals(ref.name)).findFirst();

                                    if (refDef.isPresent()) {
                                        final String  cardinality = refDef.get().cardinality;
                                        final boolean isMandatory = cardinality.startsWith("1");

                                        if (isMandatory) {
                                            // Check if there are other providers for this mandatory service
                                            final long otherProvidersCount = allComponents.stream()
                                                    .filter(c -> !alreadyAddedComponents.contains(c.name))
                                                    .filter(c -> !bundlesToStopSn.contains(c.registeringBundle)) // current
                                                                                                                 // component's
                                                                                                                 // bundle
                                                                                                                 // might
                                                                                                                 // be
                                                                                                                 // stopping
                                                    .filter(c -> c.state.equals("SATISFIED")
                                                            || c.state.equals("ACTIVE"))
                                                    .filter(c -> c.serviceInterfaces != null
                                                            && matchesAny(ref.objectClass, c.serviceInterfaces))
                                                    .count();

                                            // Also check bundles that are not stopping and registered this service
                                            final long otherBundleProvidersCount = allBundles.stream()
                                                    .filter(b -> !bundlesToStopSn.contains(b.symbolicName))
                                                    .filter(b -> b.registeredServices != null)
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
                                                impacts.add(
                                                        new XImpactDTO(component.name, "SERVICE_UNBIND",
                                                                       "One of the providers for mandatory service reference '"
                                                                               + ref.name + "' will be gone."));
                                                alreadyAddedComponents.add(component.name);
                                            }
                                        } else {
                                            impacts.add(new XImpactDTO(component.name, "SERVICE_UNBIND",
                                                                       "Optional service reference '" + ref.name
                                                                               + "' will be unbound."));
                                            alreadyAddedComponents.add(component.name);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return impacts;
    }

    private static Graph<XBundleDTO, DefaultEdge> buildBundleDependencyGraph(final Collection<XBundleDTO> allBundles) {
        final Graph<XBundleDTO, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
        allBundles.forEach(graph::addVertex);

        for (final XBundleDTO bundle : allBundles) {
            if (bundle.importedPackages == null) {
                continue;
            }
            final Set<String> importedPackages = bundle.importedPackages.stream().map(p -> p.name).collect(toSet());
            for (final XBundleDTO provider : allBundles) {
                if (bundle == provider || provider.exportedPackages == null) {
                    continue;
                }
                final boolean providesPackage = provider.exportedPackages.stream()
                        .anyMatch(p -> importedPackages.contains(p.name));
                if (providesPackage) {
                    graph.addEdge(provider, bundle);
                }
            }
        }
        return graph;
    }
}
