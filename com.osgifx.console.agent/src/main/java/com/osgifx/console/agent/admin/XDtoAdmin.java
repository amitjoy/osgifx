/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;

import com.osgifx.console.agent.dto.CDIComponentRuntimeDTO;
import com.osgifx.console.agent.dto.HttpServiceRuntimeDTO;
import com.osgifx.console.agent.dto.JaxRsServiceRuntimeDTO;
import com.osgifx.console.agent.dto.RuntimeDTO;
import com.osgifx.console.agent.dto.ServiceComponentRuntimeDTO;
import com.osgifx.console.agent.provider.PackageWirings;

import jakarta.inject.Inject;

public final class XDtoAdmin {

    private final BundleContext  context;
    private final PackageWirings wirings;
    private final Object         scrRuntime;
    private final Object         cdiRuntime;
    private final Object         httpRuntime;
    private final Object         jaxRsRuntime;

    @Inject
    public XDtoAdmin(final BundleContext context,
                     final Object scrRuntime,
                     final Object jaxRsRuntime,
                     final Object httpRuntime,
                     final Object cdiRuntime,
                     final PackageWirings wirings) {
        this.context      = context;
        this.wirings      = wirings;
        this.scrRuntime   = scrRuntime;
        this.cdiRuntime   = cdiRuntime;
        this.httpRuntime  = httpRuntime;
        this.jaxRsRuntime = jaxRsRuntime;
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
        if (!wirings.isCDIWired() || cdiRuntime == null) {
            return null;
        }
        final CDIComponentRuntime    cdi = (CDIComponentRuntime) cdiRuntime;
        final CDIComponentRuntimeDTO dto = new CDIComponentRuntimeDTO();

        dto.containers = cdi.getContainerDTOs();

        // @formatter:off
        dto.containerTemplates = Stream.of(context.getBundles())
                                       .map(cdi::getContainerTemplateDTO)
                                       .collect(toList());
        // @formatter:on

        return dto;
    }

    private HttpServiceRuntimeDTO prepareHttpDTO() {
        if (!wirings.isHttpServiceRuntimeWired() || httpRuntime == null) {
            return null;
        }
        final HttpServiceRuntime    http = (HttpServiceRuntime) httpRuntime;
        final HttpServiceRuntimeDTO dto  = new HttpServiceRuntimeDTO();

        dto.runtime = http.getRuntimeDTO();

        return dto;
    }

    private JaxRsServiceRuntimeDTO prepareJaxRsDTO() {
        if (!wirings.isJaxRsWired() || jaxRsRuntime == null) {
            return null;
        }
        final JaxrsServiceRuntime    jaxRs = (JaxrsServiceRuntime) jaxRsRuntime;
        final JaxRsServiceRuntimeDTO dto   = new JaxRsServiceRuntimeDTO();

        dto.runtime = jaxRs.getRuntimeDTO();

        return dto;
    }

    private ServiceComponentRuntimeDTO prepareScrDTO() {
        if (!wirings.isScrWired() || scrRuntime == null) {
            return null;
        }
        final ServiceComponentRuntime    scr = (ServiceComponentRuntime) scrRuntime;
        final ServiceComponentRuntimeDTO dto = new ServiceComponentRuntimeDTO();

        dto.componentDescriptionDTOs   = scr.getComponentDescriptionDTOs();
        dto.componentConfigurationDTOs = scr.getComponentDescriptionDTOs().stream()
                .map(desc -> scr.getComponentConfigurationDTOs(desc)).flatMap(Collection::stream).collect(toList());

        return dto;
    }

    private FrameworkDTO prepareFrameworkDTO() {
        final Bundle systemBundle = context.getBundle(SYSTEM_BUNDLE_ID);
        return systemBundle.adapt(FrameworkDTO.class);
    }

}
