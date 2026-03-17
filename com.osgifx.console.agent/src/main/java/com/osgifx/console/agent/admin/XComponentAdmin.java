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
package com.osgifx.console.agent.admin;

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.agent.helper.AgentHelper.createResult;
import static com.osgifx.console.agent.helper.AgentHelper.serviceUnavailable;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.SCR;
import static java.util.Collections.emptyMap;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.component.runtime.dto.ComponentConfigurationDTO.ACTIVE;
import static org.osgi.service.component.runtime.dto.ComponentConfigurationDTO.SATISFIED;
import static org.osgi.service.component.runtime.dto.ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION;
import static org.osgi.service.component.runtime.dto.ComponentConfigurationDTO.UNSATISFIED_REFERENCE;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XReferenceDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import com.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class XComponentAdmin extends AbstractSnapshotAdmin<XComponentDTO> {

    // --- Optimized Reflection Handles (Init Once, Use Forever) ---
    private static final MethodHandle FAILURE_GETTER;
    private static final MethodHandle PARAMETER_GETTER;
    private static final MethodHandle COLLECTION_TYPE_GETTER;

    static {
        final MethodHandles.Lookup lookup         = MethodHandles.publicLookup();
        MethodHandle               failure        = null;
        MethodHandle               parameter      = null;
        MethodHandle               collectionType = null;
        try {
            // Attempt to resolve R7+ fields safely
            failure        = lookup.findGetter(ComponentConfigurationDTO.class, "failure", String.class);
            parameter      = lookup.findGetter(ReferenceDTO.class, "parameter", Integer.class);
            collectionType = lookup.findGetter(ReferenceDTO.class, "collectionType", String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Fields not present (Older OSGi R6 environment), handles remain null
        }
        FAILURE_GETTER         = failure;
        PARAMETER_GETTER       = parameter;
        COLLECTION_TYPE_GETTER = collectionType;
    }

    private final BundleContext                                              context;
    private ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrTracker;

    private static final String SATISFYING_CONDITION_TARGET   = "osgi.ds.satisfying.condition.target";
    private static final String SATISFYING_CONDITION_REF_NAME = "osgi.ds.satisfying.condition";
    private static final String CONDITION_INTERFACE           = "org.osgi.service.condition.Condition";

    @Inject
    public XComponentAdmin(final BundleContext context,
                           final BinaryCodec codec,
                           final SnapshotDecoder decoder,
                           final ScheduledExecutorService executor) {
        super(codec, decoder, executor);
        this.context = context;
    }

    public void init() {
        scrTracker = new ServiceTracker<>(context, ServiceComponentRuntime.class,
                                          new ServiceTrackerCustomizer<ServiceComponentRuntime, ServiceComponentRuntime>() {

                                              @Override
                                              public ServiceComponentRuntime addingService(final ServiceReference<ServiceComponentRuntime> reference) {
                                                  final ServiceComponentRuntime service = context.getService(reference);
                                                  // Trigger immediate update on start
                                                  scheduleUpdate(getChangeCount(reference));
                                                  return service;
                                              }

                                              @Override
                                              public void modifiedService(final ServiceReference<ServiceComponentRuntime> reference,
                                                                          final ServiceComponentRuntime service) {
                                                  // This is the heartbeat of the system.
                                                  // Every time 'service.changecount' increments, the timer is reset.
                                                  scheduleUpdate(getChangeCount(reference));
                                              }

                                              @Override
                                              public void removedService(final ServiceReference<ServiceComponentRuntime> reference,
                                                                         final ServiceComponentRuntime service) {
                                                  // Critical: Clear cache immediately if SCR stops.
                                                  // Prevents UI from showing "ghost" components that no longer exist.
                                                  invalidate();
                                                  context.ungetService(reference);
                                              }
                                          });
        scrTracker.open();
    }

    @Override
    public void stop() {
        super.stop();
        if (scrTracker != null) {
            scrTracker.close();
        }
    }

    @Override
    protected List<XComponentDTO> map() throws Exception {
        final ServiceComponentRuntime scr = getServiceComponentRuntime();
        if (scr == null) {
            return null;
        }
        final List<XComponentDTO> allDTOs = new ArrayList<>();

        // Optimization: Iterate descriptions directly
        for (final ComponentDescriptionDTO desc : scr.getComponentDescriptionDTOs()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            final Collection<ComponentConfigurationDTO> configs = scr.getComponentConfigurationDTOs(desc);
            if (configs.isEmpty()) {
                allDTOs.add(toDTO(null, desc));
            } else {
                for (final ComponentConfigurationDTO config : configs) {
                    allDTOs.add(toDTO(config, desc));
                }
            }
        }
        return allDTOs;
    }

    public ServiceComponentRuntime getServiceComponentRuntime() {
        return scrTracker == null ? null : scrTracker.getService();
    }

    // Helper to read property
    private long getChangeCount(final ServiceReference<?> ref) {
        final Object prop = ref.getProperty("service.changecount");
        return prop instanceof Long ? (Long) prop : 0L;
    }

    // --- Optimized Enable/Disable (Scope Narrowing) ---

    public XResultDTO enableComponent(final long id) {
        return toggleComponent(id, true);
    }

    public XResultDTO disableComponent(final long id) {
        return toggleComponent(id, false);
    }

    private XResultDTO toggleComponent(final long id, final boolean enable) {
        final ServiceComponentRuntime scr = getServiceComponentRuntime();
        if (scr == null) {
            logger.atDebug().msg(serviceUnavailable(SCR)).log();
            return createResult(SKIPPED, serviceUnavailable(SCR));
        }

        // Optimization 1: Locate cached info
        final XComponentDTO cachedDto = findCachedDTO(id);

        Collection<ComponentDescriptionDTO> descriptionDTOs = null;

        // Optimization 2: Surgical Lookup
        if (cachedDto != null) {
            final Bundle bundle = context.getBundle(cachedDto.registeringBundleId);
            if (bundle != null) {
                descriptionDTOs = scr.getComponentDescriptionDTOs(bundle);
            }
        }

        // Fallback: Full scan if cache is stale or missing
        if (descriptionDTOs == null) {
            descriptionDTOs = scr.getComponentDescriptionDTOs();
        }

        final StringBuilder builder = new StringBuilder();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            // Optimization 3: NAME CHECK
            // If we found the cached DTO, we KNOW the component name.
            // Skip all descriptions that don't match.
            if (cachedDto != null && !dto.name.equals(cachedDto.name)) {
                continue;
            }

            // Only fetch configs for the matching description (or all if cache missed)
            final Collection<ComponentConfigurationDTO> configurationDTOs = scr.getComponentConfigurationDTOs(dto);
            for (final ComponentConfigurationDTO configDTO : configurationDTOs) {
                if (configDTO.id == id) {
                    if (!enable && isCriticalComponent(dto)) {
                        return createResult(ERROR, "Component '" + dto.name + "' is critical and cannot be disabled");
                    }
                    try {
                        final Promise<Void> promise = enable ? scr.enableComponent(dto) : scr.disableComponent(dto);
                        promise.getValue(); // Blocks intentionally (User Action)
                        performSnapshot(pendingChangeCount.get());
                    } catch (final Exception e) {
                        builder.append(e.getMessage()).append(System.lineSeparator());
                    }
                    final String action   = enable ? "enabled" : "disabled";
                    final String response = builder.toString();
                    return response.isEmpty()
                            ? createResult(SUCCESS, "Component with id '" + id + "' has been successfully " + action)
                            : createResult(ERROR, response);
                }
            }
        }
        return createResult(SUCCESS, "Component with id '" + id + "' has not been found");
    }

    private boolean isCriticalComponent(final ComponentDescriptionDTO dto) {
        return dto.name.startsWith("com.osgifx.") || dto.name.startsWith("org.eclipse.");
    }

    private XComponentDTO findCachedDTO(final long componentId) {
        final List<XComponentDTO> all = get();
        for (final XComponentDTO dto : all) {
            if (dto.id == componentId) {
                return dto;
            }
        }
        return null;
    }

    public XResultDTO enableComponent(final String name) {
        return toggleComponent(name, true);
    }

    public XResultDTO disableComponent(final String name) {
        return toggleComponent(name, false);
    }

    private XResultDTO toggleComponent(final String name, final boolean enable) {
        final ServiceComponentRuntime scr = getServiceComponentRuntime();
        if (scr == null) {
            logger.atDebug().msg(serviceUnavailable(SCR)).log();
            return createResult(SKIPPED, serviceUnavailable(SCR));
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            if (dto.name.equals(name)) {
                if (!enable && isCriticalComponent(dto)) {
                    return createResult(ERROR, "Component '" + dto.name + "' is critical and cannot be disabled");
                }
                try {
                    final Promise<Void> promise = enable ? scr.enableComponent(dto) : scr.disableComponent(dto);
                    promise.getValue();
                    performSnapshot(pendingChangeCount.get());
                } catch (final Exception e) {
                    builder.append(e.getMessage()).append(System.lineSeparator());
                }
                final String action   = enable ? "enabled" : "disabled";
                final String response = builder.toString();
                return response.isEmpty()
                        ? createResult(SUCCESS, "Component with name '" + name + "' has been successfully " + action)
                        : createResult(ERROR, response);
            }
        }
        return createResult(SUCCESS, "Component with name '" + name + "' has not been found");
    }

    // --- Optimized DTO Conversion (No Streams) ---
    private XComponentDTO toDTO(final ComponentConfigurationDTO compConfDTO,
                                final ComponentDescriptionDTO compDescDTO) {
        final XComponentDTO dto = new XComponentDTO();

        // Bundle Info
        dto.registeringBundle   = compDescDTO.bundle.symbolicName;
        dto.registeringBundleId = compDescDTO.bundle.id;

        // Basics
        dto.id                  = (compConfDTO != null) ? compConfDTO.id : -1L;
        dto.name                = compDescDTO.name;
        dto.state               = (compConfDTO != null) ? mapToState(compConfDTO.state) : "DISABLED";
        dto.factory             = compDescDTO.factory;
        dto.scope               = compDescDTO.scope;
        dto.implementationClass = compDescDTO.implementationClass;
        dto.configurationPolicy = compDescDTO.configurationPolicy;

        // Lists (Copy without Streams)
        dto.serviceInterfaces = listFromArray(compDescDTO.serviceInterfaces);
        dto.configurationPid  = listFromArray(compDescDTO.configurationPid);

        // Properties (Optimization: Simple Loop)
        if (compConfDTO != null && compConfDTO.properties != null) {
            final int                 size  = compConfDTO.properties.size();
            final Map<String, String> props = new HashMap<>((int) (size / 0.75f) + 1);
            for (final Map.Entry<String, Object> entry : compConfDTO.properties.entrySet()) {
                props.put(entry.getKey(), arrayToString(entry.getValue()));
            }
            dto.properties = props;
        } else if (compDescDTO.properties != null) {
            final int                 size  = compDescDTO.properties.size();
            final Map<String, String> props = new HashMap<>((int) (size / 0.75f) + 1);
            for (final Map.Entry<String, Object> entry : compDescDTO.properties.entrySet()) {
                props.put(entry.getKey(), arrayToString(entry.getValue()));
            }
            dto.properties = props;
        } else {
            dto.properties = emptyMap();
        }

        // References (Optimization: Loop instead of Stream)
        if (compDescDTO.references != null) {
            final List<XReferenceDTO> refs = new ArrayList<>(compDescDTO.references.length);
            for (final ReferenceDTO ref : compDescDTO.references) {
                refs.add(toRef(ref));
            }
            dto.references = refs;
        } else {
            dto.references = new ArrayList<>();
        }

        // R7 Field Access (Fast MethodHandle)
        dto.failure    = getR7FieldString(compConfDTO, FAILURE_GETTER);
        dto.activate   = compDescDTO.activate;
        dto.deactivate = compDescDTO.deactivate;
        dto.modified   = compDescDTO.modified;

        // Satisfied References (Optimization: Loop)
        if (compConfDTO != null && compConfDTO.satisfiedReferences != null) {
            final List<XSatisfiedReferenceDTO> sats = new ArrayList<>(compConfDTO.satisfiedReferences.length);
            for (final SatisfiedReferenceDTO ref : compConfDTO.satisfiedReferences) {
                sats.add(toXS(ref));
            }
            dto.satisfiedReferences = sats;
        } else {
            dto.satisfiedReferences = new ArrayList<>();
        }

        // Unsatisfied References (Optimization: Loop)
        if (compConfDTO != null && compConfDTO.unsatisfiedReferences != null) {
            final List<XUnsatisfiedReferenceDTO> unsats = new ArrayList<>(compConfDTO.unsatisfiedReferences.length);
            for (final UnsatisfiedReferenceDTO ref : compConfDTO.unsatisfiedReferences) {
                unsats.add(toXUS(ref));
            }
            dto.unsatisfiedReferences = unsats;
        } else {
            dto.unsatisfiedReferences = new ArrayList<>();
        }

        dto.satisfyingConditionTargets = getConditionTargets(dto);

        return dto;
    }

    private List<String> getConditionTargets(final XComponentDTO component) {
        final Set<String> targets = new LinkedHashSet<>();

        // 1. Check for Satisfying Condition property safely
        if (component.properties != null) {
            final String target = component.properties.get(SATISFYING_CONDITION_TARGET);
            if (target != null && !target.trim().isEmpty()) {
                targets.add(cleanTarget(target));
            }
        }

        // 2. Check for references in description
        if (component.references != null) {
            for (final XReferenceDTO ref : component.references) {
                if (SATISFYING_CONDITION_REF_NAME.equals(ref.name) || CONDITION_INTERFACE.equals(ref.interfaceName)) {
                    final String target = cleanTarget(ref.target);
                    if (target.isEmpty() && SATISFYING_CONDITION_REF_NAME.equals(ref.name)) {
                        targets.add("(osgi.condition.id=true)");
                    } else if (!target.isEmpty()) {
                        targets.add(target);
                    }
                }
            }
        }

        // 3. Check for satisfied references (overridden targets)
        if (component.satisfiedReferences != null) {
            for (final XSatisfiedReferenceDTO ref : component.satisfiedReferences) {
                if (SATISFYING_CONDITION_REF_NAME.equals(ref.name) || ref.objectClass.contains(CONDITION_INTERFACE)) {
                    final String target = cleanTarget(ref.target);
                    if (!target.isEmpty()) {
                        targets.add(target);
                    }
                }
            }
        }

        // 4. Check for unsatisfied references (overridden targets)
        if (component.unsatisfiedReferences != null) {
            for (final XUnsatisfiedReferenceDTO ref : component.unsatisfiedReferences) {
                if (SATISFYING_CONDITION_REF_NAME.equals(ref.name) || ref.objectClass.contains(CONDITION_INTERFACE)) {
                    final String target = cleanTarget(ref.target);
                    if (!target.isEmpty()) {
                        targets.add(target);
                    }
                }
            }
        }
        return new ArrayList<>(targets);
    }

    private String cleanTarget(final String target) {
        if (target == null) {
            return "";
        }
        String clean = target.trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        return clean;
    }

    private static String mapToState(final int state) {
        switch (state) {
            case ACTIVE:
                return "ACTIVE";
            case SATISFIED:
                return "SATISFIED";
            case UNSATISFIED_REFERENCE:
                return "UNSATISFIED_REFERENCE";
            case UNSATISFIED_CONFIGURATION:
                return "UNSATISFIED_CONFIGURATION";
            case 16: // ComponentConfigurationDTO.FAILED_ACTIVATION OSGi R7
                return "FAILED_ACTIVATION";
            default:
                return "<NO-MATCH>";
        }
    }

    // --- Helpers without Streams ---

    private List<String> listFromArray(final String[] arr) {
        if (arr == null) {
            return new ArrayList<>();
        }
        // Use Arrays.asList but wrap in ArrayList to ensure mutable/serializable if needed
        return new ArrayList<>(Arrays.asList(arr));
    }

    private String getR7FieldString(final Object target, final MethodHandle getter) {
        if (target == null || getter == null) {
            return "";
        }
        try {
            return (String) getter.invoke(target);
        } catch (final Throwable e) {
            return "";
        }
    }

    private Integer getR7FieldInt(final Object target, final MethodHandle getter) {
        if (target == null || getter == null) {
            return null;
        }
        try {
            return (Integer) getter.invoke(target);
        } catch (final Throwable e) {
            return null;
        }
    }

    private XReferenceDTO toRef(final ReferenceDTO reference) {
        final XReferenceDTO ref = new XReferenceDTO();
        ref.name          = reference.name;
        ref.interfaceName = reference.interfaceName;
        ref.cardinality   = reference.cardinality;
        ref.policy        = reference.policy;
        ref.policyOption  = reference.policyOption;
        ref.target        = reference.target;
        ref.bind          = reference.bind;
        ref.unbind        = reference.unbind;
        ref.updated       = reference.updated;
        ref.field         = reference.field;
        ref.fieldOption   = reference.fieldOption;
        ref.scope         = reference.scope;

        // Fast R7 access
        ref.parameter      = getR7FieldInt(reference, PARAMETER_GETTER);
        ref.collectionType = getR7FieldString(reference, COLLECTION_TYPE_GETTER);
        return ref;
    }

    private XSatisfiedReferenceDTO toXS(final SatisfiedReferenceDTO dto) {
        final XSatisfiedReferenceDTO xsr = new XSatisfiedReferenceDTO();
        xsr.name              = dto.name;
        xsr.target            = dto.target;
        xsr.objectClass       = prepareObjectClass(dto.boundServices);
        xsr.serviceReferences = dto.boundServices;
        return xsr;
    }

    private XUnsatisfiedReferenceDTO toXUS(final UnsatisfiedReferenceDTO dto) {
        final XUnsatisfiedReferenceDTO uxsr = new XUnsatisfiedReferenceDTO();
        uxsr.name        = dto.name;
        uxsr.target      = dto.target;
        uxsr.objectClass = prepareObjectClass(dto.targetServices);
        return uxsr;
    }

    private String prepareObjectClass(final ServiceReferenceDTO[] services) {
        if (services == null || services.length == 0) {
            return "";
        }

        // Optimization: Use ArrayList + StringBuilder instead of HashSet + String.join
        // This avoids calculating hash codes for object classes (which are just strings)
        final List<String> allClasses = new ArrayList<>();
        for (final ServiceReferenceDTO dto : services) {
            if (dto.properties != null) {
                final Object objClass = dto.properties.get(OBJECTCLASS);
                if (objClass instanceof String[]) {
                    for (final String s : (String[]) objClass) {
                        // Linear scan for small lists (1-3 items) is faster than Hash
                        if (!allClasses.contains(s)) {
                            allClasses.add(s);
                        }
                    }
                }
            }
        }

        // Fast String Join
        if (allClasses.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allClasses.size(); i++) {
            sb.append(allClasses.get(i));
            if (i < allClasses.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    // Optimization: Handle primitive arrays robustly
    private String arrayToString(final Object value) {
        if (value == null) {
            return "null";
        }
        if (value.getClass().isArray()) {
            if (value instanceof Object[]) {
                return Arrays.deepToString((Object[]) value);
            } else if (value instanceof int[]) {
                return Arrays.toString((int[]) value);
            } else if (value instanceof long[]) {
                return Arrays.toString((long[]) value);
            } else if (value instanceof boolean[]) {
                return Arrays.toString((boolean[]) value);
            } else if (value instanceof double[]) {
                return Arrays.toString((double[]) value);
            } else if (value instanceof float[]) {
                return Arrays.toString((float[]) value);
            } else if (value instanceof short[]) {
                return Arrays.toString((short[]) value);
            } else if (value instanceof byte[]) {
                return Arrays.toString((byte[]) value);
            } else if (value instanceof char[]) {
                return Arrays.toString((char[]) value);
            }
        }
        return value.toString();
    }

}
