/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.admin;

import static com.osgifx.console.agent.helper.AgentHelper.serviceUnavailable;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.JAX_RS_RUNTIME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.osgifx.console.agent.dto.XJaxRsComponentDTO;
import com.osgifx.console.agent.dto.XResourceMethodInfoDTO;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class XJaxRsAdmin extends AbstractSnapshotAdmin<XJaxRsComponentDTO> {

    private final BundleContext                                      context;
    private final Supplier<Object>                                   jaxRsRuntimeSupplier;
    private ServiceTracker<JaxrsServiceRuntime, JaxrsServiceRuntime> jaxRsTracker;

    @Inject
    public XJaxRsAdmin(final BundleContext context,
                       final Supplier<Object> jaxRsRuntimeSupplier,
                       final BinaryCodec codec,
                       final SnapshotDecoder decoder,
                       final ScheduledExecutorService executor) {
        super(codec, decoder, executor);
        this.context              = context;
        this.jaxRsRuntimeSupplier = jaxRsRuntimeSupplier;
    }

    public void init() {
        jaxRsTracker = new ServiceTracker<>(context, JaxrsServiceRuntime.class,
                                            new ServiceTrackerCustomizer<JaxrsServiceRuntime, JaxrsServiceRuntime>() {

                                                @Override
                                                public JaxrsServiceRuntime addingService(final ServiceReference<JaxrsServiceRuntime> reference) {
                                                    final JaxrsServiceRuntime service = context.getService(reference);
                                                    scheduleUpdate(getChangeCount(reference));
                                                    return service;
                                                }

                                                @Override
                                                public void modifiedService(final ServiceReference<JaxrsServiceRuntime> reference,
                                                                            final JaxrsServiceRuntime service) {
                                                    scheduleUpdate(getChangeCount(reference));
                                                }

                                                @Override
                                                public void removedService(final ServiceReference<JaxrsServiceRuntime> reference,
                                                                           final JaxrsServiceRuntime service) {
                                                    invalidate();
                                                    context.ungetService(reference);
                                                }
                                            });
        jaxRsTracker.open();
    }

    @Override
    public void stop() {
        super.stop();
        if (jaxRsTracker != null) {
            jaxRsTracker.close();
        }
    }

    private long getChangeCount(final ServiceReference<?> ref) {
        final Object prop = ref.getProperty("service.changecount");
        return prop instanceof Long ? (Long) prop : 0L;
    }

    @Override
    public List<XJaxRsComponentDTO> get() {
        final byte[] current = snapshot();
        if (current == null || current.length == 0) {
            return Collections.emptyList();
        }
        try {
            return decoder.decodeList(current, XJaxRsComponentDTO.class);
        } catch (final Exception e) {
            logger.atError().msg("Failed to decode JAX-RS snapshot").throwable(e).log();
            return Collections.emptyList();
        }
    }

    @Override
    protected List<XJaxRsComponentDTO> map() throws Exception {
        final JaxrsServiceRuntime runtime = (JaxrsServiceRuntime) jaxRsRuntimeSupplier.get();
        if (runtime == null) {
            logger.atWarn().msg(serviceUnavailable(JAX_RS_RUNTIME)).log();
            return Collections.emptyList();
        }
        final RuntimeDTO               runtimeDTO = runtime.getRuntimeDTO();
        final List<XJaxRsComponentDTO> dtos       = new ArrayList<>();

        // Applications
        if (runtimeDTO.defaultApplication != null) {
            addApplications(dtos, new ApplicationDTO[] { runtimeDTO.defaultApplication }, false, "Default Application");
        }
        addApplications(dtos, runtimeDTO.applicationDTOs, false, "Application");
        addFailedApplications(dtos, runtimeDTO.failedApplicationDTOs, "Application");

        // Failed Extensions
        addFailedExtensions(dtos, runtimeDTO.failedExtensionDTOs, "Extension");

        // Failed Resources
        addFailedResources(dtos, runtimeDTO.failedResourceDTOs, "Resource");

        return dtos;
    }

    private void addApplications(final List<XJaxRsComponentDTO> dtos,
                                 final ApplicationDTO[] apps,
                                 final boolean isFailed,
                                 final String type) {
        if (apps == null) {
            return;
        }
        for (final ApplicationDTO app : apps) {
            final XJaxRsComponentDTO dto = new XJaxRsComponentDTO();
            dto.type = type;
            populateBase(dto, app);
            dto.base = app.base;

            addExtensions(dtos, app.extensionDTOs, isFailed, "Extension");
            addResources(dtos, app.resourceDTOs, isFailed, "Resource");

            dtos.add(dto);
        }
    }

    private void addFailedApplications(final List<XJaxRsComponentDTO> dtos,
                                       final FailedApplicationDTO[] apps,
                                       final String type) {
        if (apps == null) {
            return;
        }
        for (final FailedApplicationDTO app : apps) {
            final XJaxRsComponentDTO dto = new XJaxRsComponentDTO();
            dto.type = type;
            populateBase(dto, app);
            dto.base = app.base;

            dto.isFailed      = true;
            dto.failureReason = app.failureReason;

            addExtensions(dtos, app.extensionDTOs, true, "Extension");
            addResources(dtos, app.resourceDTOs, true, "Resource");

            dtos.add(dto);
        }
    }

    private void addExtensions(final List<XJaxRsComponentDTO> dtos,
                               final ExtensionDTO[] extensions,
                               final boolean isFailed,
                               final String type) {
        if (extensions == null) {
            return;
        }
        for (final ExtensionDTO ext : extensions) {
            final XJaxRsComponentDTO dto = new XJaxRsComponentDTO();
            dto.type = type;
            populateBase(dto, ext);
            populateExtensionAttrs(dto, ext);

            if (ext.consumes != null) {
                dto.consumes = Arrays.asList(ext.consumes);
            }
            if (ext.produces != null) {
                dto.produces = Arrays.asList(ext.produces);
            }
            if (ext.nameBindings != null) {
                dto.nameBindings = Arrays.asList(ext.nameBindings);
            }

            if (isFailed) {
                dto.isFailed = true;
            }
            dtos.add(dto);
        }
    }

    private void addFailedExtensions(final List<XJaxRsComponentDTO> dtos,
                                     final FailedExtensionDTO[] extensions,
                                     final String type) {
        if (extensions == null) {
            return;
        }
        for (final FailedExtensionDTO ext : extensions) {
            final XJaxRsComponentDTO dto = new XJaxRsComponentDTO();
            dto.type = type;
            populateBase(dto, ext);
            populateExtensionAttrs(dto, ext);

            dto.isFailed      = true;
            dto.failureReason = ext.failureReason;

            dtos.add(dto);
        }
    }

    private void addResources(final List<XJaxRsComponentDTO> dtos,
                              final ResourceDTO[] resources,
                              final boolean isFailed,
                              final String type) {
        if (resources == null) {
            return;
        }
        for (final ResourceDTO res : resources) {
            final XJaxRsComponentDTO dto = new XJaxRsComponentDTO();
            dto.type = type;
            populateBase(dto, res);
            populateResourceAttrs(dto, res.resourceMethods);
            if (isFailed) {
                dto.isFailed = true;
            }
            dtos.add(dto);
        }
    }

    private void addFailedResources(final List<XJaxRsComponentDTO> dtos,
                                    final FailedResourceDTO[] resources,
                                    final String type) {
        if (resources == null) {
            return;
        }
        for (final FailedResourceDTO res : resources) {
            final XJaxRsComponentDTO dto = new XJaxRsComponentDTO();
            dto.type = type;
            populateBase(dto, res);

            dto.isFailed      = true;
            dto.failureReason = res.failureReason;

            dtos.add(dto);
        }
    }

    private void populateBase(final XJaxRsComponentDTO dto, final BaseDTO baseDto) {
        dto.name      = baseDto.name;
        dto.serviceId = baseDto.serviceId;
    }

    private void populateExtensionAttrs(final XJaxRsComponentDTO dto, final BaseExtensionDTO extDto) {
        if (extDto.extensionTypes != null) {
            dto.extensionTypes = Arrays.asList(extDto.extensionTypes);
        }
    }

    private void populateResourceAttrs(final XJaxRsComponentDTO dto, final ResourceMethodInfoDTO[] methods) {
        if (methods != null) {
            dto.resourceMethods = new ArrayList<>();
            for (final ResourceMethodInfoDTO method : methods) {
                final XResourceMethodInfoDTO rm = new XResourceMethodInfoDTO();
                rm.method = method.method;
                rm.path   = method.path;

                if (method.consumingMimeType != null) {
                    rm.consumingMimeType = Arrays.asList(method.consumingMimeType);
                }
                if (method.producingMimeType != null) {
                    rm.producingMimeType = Arrays.asList(method.producingMimeType);
                }
                if (method.nameBindings != null) {
                    rm.nameBindings = Arrays.asList(method.nameBindings);
                }
                dto.resourceMethods.add(rm);
            }
        }
    }

}
