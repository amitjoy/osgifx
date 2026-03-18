/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.admin;

import static org.osgi.framework.Constants.SERVICE_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.condition.Condition;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConditionDTO;
import com.osgifx.console.agent.dto.XConditionState;
import com.osgifx.console.agent.dto.XReferenceDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import com.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;
import com.osgifx.console.agent.helper.AgentHelper;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class XConditionAdmin extends AbstractSnapshotAdmin<XConditionDTO> {

    private static final String CONDITION_ID = "osgi.condition.id";

    private final BundleContext                                              context;
    private final XComponentAdmin                                            componentAdmin;
    private final Map<String, ServiceRegistration<?>>                        mockRegistrations = new ConcurrentHashMap<>();
    private ServiceTracker<Condition, Condition>                             conditionTracker;
    private ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrTracker;

    @Inject
    public XConditionAdmin(final BundleContext context,
                           final XComponentAdmin componentAdmin,
                           final BinaryCodec codec,
                           final SnapshotDecoder decoder,
                           final ScheduledExecutorService executor) {
        super(codec, decoder, executor);
        this.context        = context;
        this.componentAdmin = componentAdmin;
    }

    public void init() {
        conditionTracker = new ServiceTracker<Condition, Condition>(context, Condition.class, null) {
            @Override
            public Condition addingService(final ServiceReference<Condition> reference) {
                scheduleUpdate(pendingChangeCount.incrementAndGet());
                return context.getService(reference);
            }

            @Override
            public void modifiedService(final ServiceReference<Condition> reference, final Condition service) {
                scheduleUpdate(pendingChangeCount.incrementAndGet());
            }

            @Override
            public void removedService(final ServiceReference<Condition> reference, final Condition service) {
                scheduleUpdate(pendingChangeCount.incrementAndGet());
                context.ungetService(reference);
            }
        };
        conditionTracker.open();

        scrTracker = new ServiceTracker<>(context, ServiceComponentRuntime.class,
                                          new ServiceTrackerCustomizer<ServiceComponentRuntime, ServiceComponentRuntime>() {

                                              @Override
                                              public ServiceComponentRuntime addingService(final ServiceReference<ServiceComponentRuntime> reference) {
                                                  scheduleUpdate(getChangeCount(reference));
                                                  return context.getService(reference);
                                              }

                                              @Override
                                              public void modifiedService(final ServiceReference<ServiceComponentRuntime> reference,
                                                                          final ServiceComponentRuntime service) {
                                                  scheduleUpdate(getChangeCount(reference));
                                              }

                                              @Override
                                              public void removedService(final ServiceReference<ServiceComponentRuntime> reference,
                                                                         final ServiceComponentRuntime service) {
                                                  invalidate(); // Clear cache when SCR stops
                                                  context.ungetService(reference);
                                              }
                                          });
        scrTracker.open();

        // Trigger initial snapshot
        scheduleUpdate(pendingChangeCount.incrementAndGet());
    }

    private long getChangeCount(final ServiceReference<?> ref) {
        final Object prop = ref.getProperty("service.changecount");
        return prop instanceof Long ? (Long) prop : 0L;
    }

    @Override
    public void stop() {
        super.stop();
        if (conditionTracker != null) {
            conditionTracker.close();
        }
        if (scrTracker != null) {
            scrTracker.close();
        }
        mockRegistrations.values().forEach(ServiceRegistration::unregister);
        mockRegistrations.clear();
    }

    @Override
    protected List<XConditionDTO> map() throws Exception {
        componentAdmin.invalidate();
        final List<XComponentDTO>        components      = componentAdmin.get();
        final List<XConditionDTO>        dtos            = new ArrayList<>();
        final Map<String, XConditionDTO> map             = new HashMap<>();
        final List<Long>                 boundServiceIds = new ArrayList<>();

        for (final XComponentDTO component : components) {
            // 1. Process Satisfied References
            if (component.satisfiedReferences != null) {
                for (final XSatisfiedReferenceDTO ref : component.satisfiedReferences) {
                    if (isConditionReference(component, ref.name)) {
                        final String target = cleanTarget(ref.target);
                        if (isDefaultCondition(target)) {
                            continue;
                        }
                        final XConditionDTO dto = map.computeIfAbsent(target, k -> createDTO(target));
                        if (ref.serviceReferences != null && ref.serviceReferences.length > 0) {
                            final ServiceReferenceDTO         sref    = ref.serviceReferences[0];
                            final ServiceReference<Condition> realRef = getRealReference(sref.id);

                            if (realRef != null) {
                                final Long serviceId = (Long) realRef.getProperty(SERVICE_ID);
                                boundServiceIds.add(serviceId);

                                final String mockIdentifier = getMockIdentifier(realRef);
                                if (mockIdentifier != null) {
                                    dto.state = XConditionState.MOCKED;
                                } else {
                                    dto.state = XConditionState.ACTIVE;
                                }
                                dto.providerBundleId = realRef.getBundle().getBundleId();
                                dto.properties       = AgentHelper.createProperties(realRef);
                            } else {
                                // Fallback: if we can't find the real reference, we know it's at least ACTIVE
                                if (mockRegistrations.containsKey(target)) {
                                    dto.state = XConditionState.MOCKED;
                                } else {
                                    dto.state = XConditionState.ACTIVE;
                                }
                            }
                        }
                        if (!dto.satisfiedComponents.contains(component.name)) {
                            dto.satisfiedComponents.add(component.name);
                        }
                    }
                }
            }
            // 2. Process Unsatisfied References
            if (component.unsatisfiedReferences != null) {
                for (final XUnsatisfiedReferenceDTO ref : component.unsatisfiedReferences) {
                    if (isConditionReference(component, ref.name)) {
                        final String target = cleanTarget(ref.target);
                        if (isDefaultCondition(target)) {
                            continue;
                        }
                        final XConditionDTO dto = map.computeIfAbsent(target, k -> createDTO(target));
                        if (dto.state != XConditionState.ACTIVE && dto.state != XConditionState.MOCKED) {
                            dto.state = mockRegistrations.containsKey(target) ? XConditionState.MOCKED
                                    : XConditionState.MISSING;
                        }
                        if (!dto.unsatisfiedComponents.contains(component.name)) {
                            dto.unsatisfiedComponents.add(component.name);
                        }
                    }
                }
            }
        }

        // 3. Process Orphaned Conditions (Active services not bound to any component)
        final ServiceReference<Condition>[] refs = conditionTracker.getServiceReferences();
        if (refs != null) {
            for (final ServiceReference<Condition> ref : refs) {
                final Long serviceId = (Long) ref.getProperty(SERVICE_ID);
                if (boundServiceIds.contains(serviceId)) {
                    continue; // Already processed via component reference
                }
                final String mockIdentifier = getMockIdentifier(ref);
                if (mockIdentifier != null && map.containsKey(mockIdentifier)) {
                    // Update the existing DTO with provider info if not already set (e.g. from Step 2)
                    final XConditionDTO existing = map.get(mockIdentifier);
                    if (existing.providerBundleId == -1) {
                        existing.providerBundleId = ref.getBundle().getBundleId();
                        existing.properties       = AgentHelper.createProperties(ref);
                        // If it's a mock service and the identifier is already in the list, mark as MOCKED
                        existing.state = XConditionState.MOCKED;
                    }
                    continue;
                }
                final String id = (String) ref.getProperty(CONDITION_ID);
                if ("true".equals(id)) {
                    continue; // Skip default true condition
                }
                final String identifier = id != null ? "(" + CONDITION_ID + "=" + id + ")"
                        : "(" + SERVICE_ID + "=" + serviceId + ")";

                final XConditionDTO dto = map.computeIfAbsent(identifier, k -> createDTO(identifier));
                if (mockIdentifier != null) {
                    dto.state = XConditionState.MOCKED;
                } else {
                    dto.state = XConditionState.ACTIVE;
                }
                dto.providerBundleId = ref.getBundle().getBundleId();
                dto.properties       = AgentHelper.createProperties(ref);
            }
        }

        // 4. Process Orphaned Mocks (Mock registrations not matched by any service or component)
        for (final String mockIdentifier : mockRegistrations.keySet()) {
            if (!map.containsKey(mockIdentifier)) {
                final XConditionDTO dto = createDTO(mockIdentifier);
                dto.state = XConditionState.MOCKED;
                map.put(mockIdentifier, dto);
            }
        }

        dtos.addAll(map.values());
        return dtos;
    }

    private XConditionDTO createDTO(final String identifier) {
        final XConditionDTO dto = new XConditionDTO();
        dto.identifier            = identifier;
        dto.state                 = XConditionState.MISSING;
        dto.providerBundleId      = -1L;
        dto.properties            = new HashMap<>();
        dto.satisfiedComponents   = new ArrayList<>();
        dto.unsatisfiedComponents = new ArrayList<>();
        return dto;
    }

    private boolean isConditionReference(final XComponentDTO component, final String refName) {
        if (component.references == null || refName == null) {
            return false;
        }
        for (final XReferenceDTO ref : component.references) {
            if (refName.equals(ref.name)) {
                return "osgi.ds.satisfying.condition".equals(ref.name)
                        || Condition.class.getName().equals(ref.interfaceName);
            }
        }
        return false;
    }

    private ServiceReference<Condition> getRealReference(final long serviceId) {
        final ServiceReference<Condition>[] refs = conditionTracker.getServiceReferences();
        if (refs != null) {
            for (final ServiceReference<Condition> ref : refs) {
                final Object property = ref.getProperty(SERVICE_ID);
                if (property instanceof Number && ((Number) property).longValue() == serviceId) {
                    return ref;
                }
            }
        }
        return null;
    }

    private String getMockIdentifier(final ServiceReference<?> ref) {
        for (final Map.Entry<String, ServiceRegistration<?>> entry : mockRegistrations.entrySet()) {
            if (entry.getValue().getReference().equals(ref)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private boolean isDefaultCondition(final String target) {
        if (target == null) {
            return false;
        }
        // Remove spaces to prevent formatting from hiding default true filters
        final String cleanTarget = target.replaceAll("\\s", "");
        return ("(" + CONDITION_ID + "=true)").equals(cleanTarget) || "true".equals(cleanTarget);
    }

    private String cleanTarget(final String target) {
        if (target == null) {
            return "";
        }
        String clean = target.trim();
        // Array brackets [ ] might exist if the property was parsed as an array string representation
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        return clean;
    }

    public XResultDTO injectMockCondition(final String identifier, final Map<String, Object> properties) {
        if (mockRegistrations.containsKey(identifier)) {
            return AgentHelper.createResult(XResultDTO.SKIPPED, "Condition '" + identifier + "' is already mocked");
        }
        final Map<String, Object> props = new ConcurrentHashMap<>(properties);

        final ServiceRegistration<Condition> reg = context.registerService(Condition.class, Condition.INSTANCE,
                new Hashtable<>(props));
        mockRegistrations.put(identifier, reg);
        scheduleUpdate(pendingChangeCount.incrementAndGet());
        return AgentHelper.createResult(XResultDTO.SUCCESS,
                "Condition '" + identifier + "' has been successfully mocked");
    }

    public XResultDTO revokeMockCondition(final String identifier) {
        final ServiceRegistration<?> reg = mockRegistrations.remove(identifier);
        if (reg != null) {
            reg.unregister();
            scheduleUpdate(pendingChangeCount.incrementAndGet());
            return AgentHelper.createResult(XResultDTO.SUCCESS, "Mock condition '" + identifier + "' has been revoked");
        }
        return AgentHelper.createResult(XResultDTO.SKIPPED, "Condition '" + identifier + "' was not mocked");
    }

}