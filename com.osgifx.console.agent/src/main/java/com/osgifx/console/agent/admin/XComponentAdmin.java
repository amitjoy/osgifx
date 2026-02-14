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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XReferenceDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import com.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;

import jakarta.inject.Inject;

public final class XComponentAdmin {

    // --- Tuning ---
    private static final long DEBOUNCE_DELAY_MS = 200;
    private static final long MAX_WAIT_MS       = 5000;

    // --- Optimized Reflection Handles (Init Once, Use Forever) ---
    private static final MethodHandle FAILURE_GETTER;
    private static final MethodHandle PARAMETER_GETTER;
    private static final MethodHandle COLLECTION_TYPE_GETTER;

    static {
        MethodHandles.Lookup lookup         = MethodHandles.publicLookup();
        MethodHandle         failure        = null;
        MethodHandle         parameter      = null;
        MethodHandle         collectionType = null;
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

    // --- State Management ---
    // Volatile allows lock-free reads. Readers always see a consistent map.
    private volatile Map<Long, List<XComponentDTO>> components = Collections.emptyMap();

    // Concurrency controls
    private final AtomicLong                          lastChangeCount    = new AtomicLong(-1);     // Last *Processed*
    private final AtomicLong                          pendingChangeCount = new AtomicLong(-1);     // Latest *Requested*
    private final AtomicLong                          lastEventTime      = new AtomicLong(0);
    private final AtomicLong                          deadline           = new AtomicLong(0);
    private final AtomicReference<ScheduledFuture<?>> scheduledTask      = new AtomicReference<>();

    // --- Infrastructure ---
    private final BundleContext                                              context;
    private final ScheduledExecutorService                                   executor;
    private ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrTracker;
    private final FluentLogger                                               logger = LoggerFactory
            .getFluentLogger(getClass());

    @Inject
    public XComponentAdmin(final BundleContext context, final ScheduledExecutorService executor) {
        this.context  = context;
        this.executor = executor;
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
                                                  components = Collections.emptyMap();
                                                  context.ungetService(reference);
                                              }
                                          });
        scrTracker.open();
    }

    public void stop() {
        if (scrTracker != null) {
            scrTracker.close();
        }
        final ScheduledFuture<?> task = scheduledTask.get();
        if (task != null) {
            task.cancel(true);
        }
    }

    // --- Adaptive Scheduler (Lock-Free) ---
    private void scheduleUpdate(final long changeCount) {
        final long now = System.currentTimeMillis();
        lastEventTime.set(now);
        pendingChangeCount.set(changeCount);

        // 1. Initialize Deadline if not set (CAS ensures we only set it on the FIRST event of a burst)
        deadline.compareAndSet(0, now + MAX_WAIT_MS);

        // 2. Ensure a Checker Task is running
        scheduledTask.updateAndGet(current -> {
            if (current != null && !current.isDone()) {
                // Task is already running/scheduled. It will see the updated 'lastEventTime'
                // and reschedule itself if necessary.
                return current;
            }
            // No task running. Schedule one.
            return executor.schedule(this::checkAndRun, DEBOUNCE_DELAY_MS, MILLISECONDS);
        });
    }

    // --- The Checker Logic ---
    private void checkAndRun() {
        final long now             = System.currentTimeMillis();
        final long lastEvent       = lastEventTime.get();
        final long currentDeadline = deadline.get();

        final long silence   = now - lastEvent;
        final long remaining = currentDeadline - now;
        final long delay     = DEBOUNCE_DELAY_MS - silence;

        // Condition to RUN: Silence achieved OR Deadline exceeded
        if (delay <= 0 || remaining <= 0) {
            performSnapshot(pendingChangeCount.get());

            // Cleanup
            deadline.set(0);

            // Race Condition Check:
            // If a new event came in *while* we were running (or just before we cleared deadline),
            // 'lastEventTime' would be > 'now' (approx).
            // We must insure we don't drop that event.
            if (lastEventTime.get() > now) {
                // Reschedule to handle the pending event
                scheduledTask.set(executor.schedule(this::checkAndRun, DEBOUNCE_DELAY_MS, MILLISECONDS));
            }
        } else {
            // Not ready yet. Reschedule for the remaining wait time.
            // We take the smaller of (Debounce Delay) or (Deadline).
            final long wait = Math.max(1, Math.min(delay, remaining));
            scheduledTask.set(executor.schedule(this::checkAndRun, wait, MILLISECONDS));
        }
    }

    // --- Optimized Snapshot Execution ---
    private void performSnapshot(final long changeCount) {
        final ServiceComponentRuntime scr = getServiceComponentRuntime();
        if (scr == null) {
            return;
        }

        try {
            final Map<Long, List<XComponentDTO>> newSnapshot = new HashMap<>();

            // Optimization: Iterate descriptions directly
            for (final ComponentDescriptionDTO desc : scr.getComponentDescriptionDTOs()) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                // Optimization: Pre-size list to avoid resizing (heuristic: typical bundle has 1-5 components)
                final List<XComponentDTO> dtos = new ArrayList<>(5);

                final Collection<ComponentConfigurationDTO> configs = scr.getComponentConfigurationDTOs(desc);
                if (configs.isEmpty()) {
                    dtos.add(toDTO(null, desc));
                } else {
                    for (final ComponentConfigurationDTO config : configs) {
                        dtos.add(toDTO(config, desc));
                    }
                }
                newSnapshot.put(desc.bundle.id, dtos);
            }

            this.components = Collections.unmodifiableMap(newSnapshot);
            lastChangeCount.set(changeCount);

        } catch (final Exception e) {
            logger.atError().msg("SCR Snapshot failed").throwable(e).log();
        } finally {
            deadline.set(0);
        }
    }

    // --- Lock-Free ---
    public List<XComponentDTO> getComponents() {
        // Fast. Non-blocking. Thread-safe.
        return components.values().stream().flatMap(List::stream).collect(Collectors.toList());
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
            logger.atWarn().msg(serviceUnavailable(SCR)).log();
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
                    try {
                        final Promise<Void> promise = enable ? scr.enableComponent(dto) : scr.disableComponent(dto);
                        promise.getValue(); // Blocks intentionally (User Action)
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

    private XComponentDTO findCachedDTO(final long componentId) {
        for (final List<XComponentDTO> list : components.values()) {
            for (final XComponentDTO dto : list) {
                if (dto.id == componentId) {
                    return dto;
                }
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
            logger.atWarn().msg(serviceUnavailable(SCR)).log();
            return createResult(SKIPPED, serviceUnavailable(SCR));
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = scr.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            if (dto.name.equals(name)) {
                try {
                    final Promise<Void> promise = enable ? scr.enableComponent(dto) : scr.disableComponent(dto);
                    promise.getValue();
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

        return dto;
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
