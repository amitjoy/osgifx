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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cdi.runtime.dto.ActivationDTO;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ComponentInstanceDTO;
import org.osgi.service.cdi.runtime.dto.ConfigurationDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.ExtensionDTO;
import org.osgi.service.cdi.runtime.dto.ReferenceDTO;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.osgifx.console.agent.dto.XCdiActivationDTO;
import com.osgifx.console.agent.dto.XCdiComponentDTO;
import com.osgifx.console.agent.dto.XCdiComponentInstanceDTO;
import com.osgifx.console.agent.dto.XCdiConfigurationDTO;
import com.osgifx.console.agent.dto.XCdiContainerDTO;
import com.osgifx.console.agent.dto.XCdiExtensionDTO;
import com.osgifx.console.agent.dto.XCdiReferenceDTO;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class XCdiAdmin extends AbstractSnapshotAdmin<XCdiContainerDTO> {

    private final BundleContext                                      context;
    private final Supplier<Object>                                   cdiRuntimeSupplier;
    private ServiceTracker<CDIComponentRuntime, CDIComponentRuntime> cdiTracker;

    @Inject
    public XCdiAdmin(final BundleContext context,
                     final Supplier<Object> cdiRuntimeSupplier,
                     final BinaryCodec codec,
                     final SnapshotDecoder decoder,
                     final ScheduledExecutorService executor) {
        super(codec, decoder, executor);
        this.context            = context;
        this.cdiRuntimeSupplier = cdiRuntimeSupplier;
    }

    public void init() {
        cdiTracker = new ServiceTracker<>(context, CDIComponentRuntime.class,
                                          new ServiceTrackerCustomizer<CDIComponentRuntime, CDIComponentRuntime>() {

                                              @Override
                                              public CDIComponentRuntime addingService(final ServiceReference<CDIComponentRuntime> reference) {
                                                  final CDIComponentRuntime service = context.getService(reference);
                                                  scheduleUpdate(getChangeCount(reference));
                                                  return service;
                                              }

                                              @Override
                                              public void modifiedService(final ServiceReference<CDIComponentRuntime> reference,
                                                                          final CDIComponentRuntime service) {
                                                  scheduleUpdate(getChangeCount(reference));
                                              }

                                              @Override
                                              public void removedService(final ServiceReference<CDIComponentRuntime> reference,
                                                                         final CDIComponentRuntime service) {
                                                  invalidate();
                                                  context.ungetService(reference);
                                              }
                                          });
        cdiTracker.open();
    }

    @Override
    public void stop() {
        super.stop();
        if (cdiTracker != null) {
            cdiTracker.close();
        }
    }

    private long getChangeCount(final ServiceReference<?> ref) {
        final Object prop = ref.getProperty("service.changecount");
        return prop instanceof Long ? (Long) prop : 0L;
    }

    @Override
    public List<XCdiContainerDTO> get() {
        final byte[] current = snapshot();
        if (current == null || current.length == 0) {
            return Collections.emptyList();
        }
        try {
            return decoder.decodeList(current, XCdiContainerDTO.class);
        } catch (final Exception e) {
            logger.atError().msg("Failed to decode CDI snapshot").throwable(e).log();
            return Collections.emptyList();
        }
    }

    @Override
    protected List<XCdiContainerDTO> map() throws Exception {
        final CDIComponentRuntime runtime = (CDIComponentRuntime) cdiRuntimeSupplier.get();
        if (runtime == null) {
            logger.atWarn().msg("OSGi CDI Runtime service is unavailable").log();
            return Collections.emptyList();
        }
        return runtime.getContainerDTOs().stream().map(this::toXCdiContainerDTO).collect(toList());
    }

    private XCdiContainerDTO toXCdiContainerDTO(final ContainerDTO dto) {
        final XCdiContainerDTO xdto = new XCdiContainerDTO();
        xdto.id          = dto.template.id;
        xdto.bundleId    = dto.bundle.id;
        xdto.changeCount = dto.changeCount;
        xdto.errors      = dto.errors;
        xdto.extensions  = dto.extensions.stream().map(this::toXCdiExtensionDTO).collect(toList());
        xdto.components  = dto.components.stream().map(this::toXCdiComponentDTO).collect(toList());
        return xdto;
    }

    private XCdiExtensionDTO toXCdiExtensionDTO(final ExtensionDTO dto) {
        final XCdiExtensionDTO xdto = new XCdiExtensionDTO();
        if (dto.template != null) {
            xdto.serviceFilter = dto.template.serviceFilter;
        }
        if (dto.service != null) {
            xdto.serviceId = dto.service.id;
        }
        return xdto;
    }

    private XCdiComponentDTO toXCdiComponentDTO(final ComponentDTO dto) {
        final XCdiComponentDTO xdto = new XCdiComponentDTO();
        if (dto.template != null) {
            xdto.name       = dto.template.name;
            xdto.type       = dto.template.type.name();
            xdto.beans      = dto.template.beans;
            xdto.properties = convertMap(dto.template.properties);
        }
        xdto.enabled   = dto.enabled;
        xdto.instances = dto.instances.stream().map(this::toXCdiComponentInstanceDTO).collect(toList());
        return xdto;
    }

    private XCdiComponentInstanceDTO toXCdiComponentInstanceDTO(final ComponentInstanceDTO dto) {
        final XCdiComponentInstanceDTO xdto = new XCdiComponentInstanceDTO();
        xdto.properties     = convertMap(dto.properties);
        xdto.configurations = dto.configurations.stream().map(this::toXCdiConfigurationDTO).collect(toList());
        xdto.references     = dto.references.stream().map(this::toXCdiReferenceDTO).collect(toList());
        xdto.activations    = dto.activations.stream().map(this::toXCdiActivationDTO).collect(toList());
        return xdto;
    }

    private XCdiConfigurationDTO toXCdiConfigurationDTO(final ConfigurationDTO dto) {
        final XCdiConfigurationDTO xdto = new XCdiConfigurationDTO();
        if (dto.template != null) {
            xdto.pid                = dto.template.pid;
            xdto.policy             = dto.template.policy.name();
            xdto.maximumCardinality = dto.template.maximumCardinality.name();
        }
        xdto.properties = convertMap(dto.properties);
        return xdto;
    }

    private XCdiReferenceDTO toXCdiReferenceDTO(final ReferenceDTO dto) {
        final XCdiReferenceDTO xdto = new XCdiReferenceDTO();
        if (dto.template != null) {
            xdto.name               = dto.template.name;
            xdto.serviceType        = dto.template.serviceType;
            xdto.targetFilter       = dto.template.targetFilter;
            xdto.minimumCardinality = dto.template.minimumCardinality;
            xdto.maximumCardinality = dto.template.maximumCardinality.name();
            xdto.policy             = dto.template.policy.name();
            xdto.policyOption       = dto.template.policyOption.name();
        }
        xdto.matches = dto.matches.stream().map(s -> s.id).collect(toList());
        return xdto;
    }

    private XCdiActivationDTO toXCdiActivationDTO(final ActivationDTO dto) {
        final XCdiActivationDTO xdto = new XCdiActivationDTO();
        if (dto.template != null) {
            xdto.scope          = dto.template.scope.name();
            xdto.serviceClasses = dto.template.serviceClasses;
            xdto.properties     = convertMap(dto.template.properties);
        }
        if (dto.service != null) {
            xdto.serviceId = dto.service.id;
        }
        xdto.errors = dto.errors;
        return xdto;
    }

    private Map<String, String> convertMap(final Map<String, Object> map) {
        if (map == null) {
            return Collections.emptyMap();
        }
        return map.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
    }

}
