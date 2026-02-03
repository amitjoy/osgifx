/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.CDIComponentRuntimeDTO;
import com.osgifx.console.agent.dto.HttpServiceRuntimeDTO;
import com.osgifx.console.agent.dto.JaxRsServiceRuntimeDTO;
import com.osgifx.console.agent.dto.RuntimeDTO;
import com.osgifx.console.agent.dto.ServiceComponentRuntimeDTO;
import com.osgifx.console.agent.provider.PackageWirings;

import jakarta.inject.Inject;

public final class XDtoAdmin {

    private final BundleContext    context;
    private final PackageWirings   wirings;
    private final Supplier<Object> scrRuntimeSupplier;
    private final Supplier<Object> cdiRuntimeSupplier;
    private final Supplier<Object> httpRuntimeSupplier;
    private final Supplier<Object> jaxRsRuntimeSupplier;
    private final FluentLogger     logger = LoggerFactory.getFluentLogger(getClass());

    @Inject
    public XDtoAdmin(final BundleContext context,
                     final Supplier<Object> scrRuntimeSupplier,
                     final Supplier<Object> jaxRsRuntimeSupplier,
                     final Supplier<Object> httpRuntimeSupplier,
                     final Supplier<Object> cdiRuntimeSupplier,
                     final PackageWirings wirings) {
        this.context              = context;
        this.wirings              = wirings;
        this.scrRuntimeSupplier   = scrRuntimeSupplier;
        this.cdiRuntimeSupplier   = cdiRuntimeSupplier;
        this.httpRuntimeSupplier  = httpRuntimeSupplier;
        this.jaxRsRuntimeSupplier = jaxRsRuntimeSupplier;
    }

    public RuntimeDTO runtime() {
        final RuntimeDTO dto = new RuntimeDTO();

        dto.framework = prepareFrameworkDTO();
        dto.scr       = prepareScrDTO();
        dto.jaxrs     = prepareJaxRsDTO();
        dto.http      = prepareHttpDTO();
        dto.cdi       = prepareCDIDTO();

        return dto;
    }

    private CDIComponentRuntimeDTO prepareCDIDTO() {
        final CDIComponentRuntime cdi = (CDIComponentRuntime) cdiRuntimeSupplier.get();
        if (!wirings.isCDIWired() || cdi == null) {
            logger.atWarn().msg("CDI bundle is unavailable to retrieve the CDI DTO").log();
            return null;
        }
        final CDIComponentRuntimeDTO dto = new CDIComponentRuntimeDTO();

        dto.containers = cdi.getContainerDTOs();

        // @formatter:off
        dto.containerTemplates = Stream.of(context.getBundles())
                                       .map(cdi::getContainerTemplateDTO)
                                       .filter(java.util.Objects::nonNull)
                                       .collect(toList());
        // @formatter:on

        return dto;
    }

    private HttpServiceRuntimeDTO prepareHttpDTO() {
        final HttpServiceRuntime http = (HttpServiceRuntime) httpRuntimeSupplier.get();
        if (!wirings.isHttpServiceRuntimeWired() || http == null) {
            logger.atInfo().msg("HTTP bundle is unavailable to retrieve the HTTP DTO").log();
            return null;
        }
        final HttpServiceRuntimeDTO dto = new HttpServiceRuntimeDTO();

        dto.runtime = http.getRuntimeDTO();

        return dto;
    }

    private JaxRsServiceRuntimeDTO prepareJaxRsDTO() {
        final JaxrsServiceRuntime jaxRs = (JaxrsServiceRuntime) jaxRsRuntimeSupplier.get();
        if (!wirings.isJaxRsWired() || jaxRs == null) {
            logger.atWarn().msg("JAX-RS bundle is unavailable to retrieve the JAX-RS DTO").log();
            return null;
        }
        final JaxRsServiceRuntimeDTO dto = new JaxRsServiceRuntimeDTO();

        dto.runtime = jaxRs.getRuntimeDTO();

        return dto;
    }

    private ServiceComponentRuntimeDTO prepareScrDTO() {
        final ServiceComponentRuntime scr = (ServiceComponentRuntime) scrRuntimeSupplier.get();
        if (!wirings.isScrWired() || scr == null) {
            logger.atWarn().msg("SCR runtime is unavailable to retrieve the SCR DTO").log();
            return null;
        }
        final ServiceComponentRuntimeDTO dto = new ServiceComponentRuntimeDTO();

        dto.componentDescriptionDTOs   = scr.getComponentDescriptionDTOs();
        dto.componentConfigurationDTOs = dto.componentDescriptionDTOs.stream()
                .map(scr::getComponentConfigurationDTOs).flatMap(Collection::stream).collect(toList());

        return dto;
    }

    private FrameworkDTO prepareFrameworkDTO() {
        final Bundle systemBundle = context.getBundle(SYSTEM_BUNDLE_ID);
        return systemBundle.adapt(FrameworkDTO.class);
    }

}
