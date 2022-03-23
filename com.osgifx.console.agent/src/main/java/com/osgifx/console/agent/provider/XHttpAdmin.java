/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.agent.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

import com.osgifx.console.agent.dto.XErrorPageDTO;
import com.osgifx.console.agent.dto.XFilterDTO;
import com.osgifx.console.agent.dto.XHttpContextInfoDTO;
import com.osgifx.console.agent.dto.XListenerDTO;
import com.osgifx.console.agent.dto.XResourceDTO;
import com.osgifx.console.agent.dto.XServletDTO;

public class XHttpAdmin {

	private final HttpServiceRuntime httpServiceRuntime;

	public XHttpAdmin(final Object httpServiceRuntime) {
		this.httpServiceRuntime = (HttpServiceRuntime) httpServiceRuntime;
	}

	public XHttpContextInfoDTO runtime() {
		final RuntimeDTO runtime = httpServiceRuntime.getRuntimeDTO();
		return initHttpRuntime(runtime);
	}

	private XHttpContextInfoDTO initHttpRuntime(final RuntimeDTO runtime) {
		final XHttpContextInfoDTO httpInfo = new XHttpContextInfoDTO();

		httpInfo.servlets   = initServlets(runtime);
		httpInfo.filters    = initFilters(runtime);
		httpInfo.resources  = initResources(runtime);
		httpInfo.listeners  = initListeners(runtime);
		httpInfo.errorPages = initErrorPages(runtime);

		return httpInfo;
	}

	private List<XServletDTO> initServlets(final RuntimeDTO runtime) {
		final List<XServletDTO>   servlets           = new ArrayList<>();
		final ServletContextDTO[] servletContextDTOs = runtime.servletContextDTOs;

		for (final ServletContextDTO servletContextDTO : servletContextDTOs) {
			final List<XServletDTO> servletDTOs = initServletsByContext(servletContextDTO);
			servlets.addAll(servletDTOs);
		}
		return servlets;
	}

	private List<XServletDTO> initServletsByContext(final ServletContextDTO servletContextDTO) {
		final List<XServletDTO> servletDTO = new ArrayList<>();

		for (final ServletDTO sDTO : servletContextDTO.servletDTOs) {
			final XServletDTO dto = new XServletDTO();

			dto.contextName      = servletContextDTO.name;
			dto.contextPath      = servletContextDTO.contextPath;
			dto.contextServiceId = servletContextDTO.serviceId;
			dto.patterns         = Arrays.asList(sDTO.patterns);
			dto.name             = sDTO.name;
			dto.asyncSupported   = sDTO.asyncSupported;
			dto.serviceId        = sDTO.serviceId;
			dto.servletInfo      = sDTO.servletInfo;
			dto.type             = "Servlet";

			servletDTO.add(dto);
		}
		return servletDTO;
	}

	private List<XFilterDTO> initFilters(final RuntimeDTO runtime) {
		final List<XFilterDTO>    filters            = new ArrayList<>();
		final ServletContextDTO[] servletContextDTOs = runtime.servletContextDTOs;

		for (final ServletContextDTO servletContextDTO : servletContextDTOs) {
			final List<XFilterDTO> filterDTOs = initFiltersByContext(servletContextDTO);
			filters.addAll(filterDTOs);
		}
		return filters;
	}

	private List<XFilterDTO> initFiltersByContext(final ServletContextDTO servletContextDTO) {
		final List<XFilterDTO> filterDTO = new ArrayList<>();

		for (final FilterDTO fDTO : servletContextDTO.filterDTOs) {
			final XFilterDTO dto = new XFilterDTO();

			dto.contextName      = servletContextDTO.name;
			dto.contextPath      = servletContextDTO.contextPath;
			dto.contextServiceId = servletContextDTO.serviceId;
			dto.patterns         = Arrays.asList(fDTO.patterns);
			dto.name             = fDTO.name;
			dto.asyncSupported   = fDTO.asyncSupported;
			dto.serviceId        = fDTO.serviceId;
			dto.dispatcher       = Arrays.asList(fDTO.dispatcher);
			dto.regexs           = Arrays.asList(fDTO.regexs);
			dto.servletNames     = Arrays.asList(fDTO.servletNames);
			dto.type             = "Filter";

			filterDTO.add(dto);
		}
		return filterDTO;
	}

	private List<XResourceDTO> initResources(final RuntimeDTO runtime) {
		final List<XResourceDTO>  resources          = new ArrayList<>();
		final ServletContextDTO[] servletContextDTOs = runtime.servletContextDTOs;

		for (final ServletContextDTO servletContextDTO : servletContextDTOs) {
			final List<XResourceDTO> resourceDTOs = initResourcesByContext(servletContextDTO);
			resources.addAll(resourceDTOs);
		}
		return resources;
	}

	private List<XResourceDTO> initResourcesByContext(final ServletContextDTO servletContextDTO) {
		final List<XResourceDTO> resourceDTO = new ArrayList<>();

		for (final ResourceDTO rDTO : servletContextDTO.resourceDTOs) {
			final XResourceDTO dto = new XResourceDTO();

			dto.contextName      = servletContextDTO.name;
			dto.contextPath      = servletContextDTO.contextPath;
			dto.contextServiceId = servletContextDTO.serviceId;
			dto.patterns         = Arrays.asList(rDTO.patterns);
			dto.prefix           = rDTO.prefix;
			dto.serviceId        = rDTO.serviceId;
			dto.type             = "Resource";

			resourceDTO.add(dto);
		}
		return resourceDTO;
	}

	private List<XListenerDTO> initListeners(final RuntimeDTO runtime) {
		final List<XListenerDTO>  listeners          = new ArrayList<>();
		final ServletContextDTO[] servletContextDTOs = runtime.servletContextDTOs;

		for (final ServletContextDTO servletContextDTO : servletContextDTOs) {
			final List<XListenerDTO> listenerDTOs = initListenersByContext(servletContextDTO);
			listeners.addAll(listenerDTOs);
		}
		return listeners;
	}

	private List<XListenerDTO> initListenersByContext(final ServletContextDTO servletContextDTO) {
		final List<XListenerDTO> listenerDTO = new ArrayList<>();

		for (final ListenerDTO lDTO : servletContextDTO.listenerDTOs) {
			final XListenerDTO dto = new XListenerDTO();

			dto.contextName      = servletContextDTO.name;
			dto.contextPath      = servletContextDTO.contextPath;
			dto.contextServiceId = servletContextDTO.serviceId;
			dto.types            = Arrays.asList(lDTO.types);
			dto.serviceId        = lDTO.serviceId;
			dto.type             = "Listener";

			listenerDTO.add(dto);
		}
		return listenerDTO;
	}

	private List<XErrorPageDTO> initErrorPages(final RuntimeDTO runtime) {
		final List<XErrorPageDTO> errorPages         = new ArrayList<>();
		final ServletContextDTO[] servletContextDTOs = runtime.servletContextDTOs;

		for (final ServletContextDTO servletContextDTO : servletContextDTOs) {
			final List<XErrorPageDTO> errorPageDTOs = initErrorPagesByContext(servletContextDTO);
			errorPages.addAll(errorPageDTOs);
		}
		return errorPages;
	}

	private List<XErrorPageDTO> initErrorPagesByContext(final ServletContextDTO servletContextDTO) {
		final List<XErrorPageDTO> errorPageDTO = new ArrayList<>();

		for (final ErrorPageDTO eDTO : servletContextDTO.errorPageDTOs) {
			final XErrorPageDTO dto = new XErrorPageDTO();

			dto.contextName      = servletContextDTO.name;
			dto.contextPath      = servletContextDTO.contextPath;
			dto.contextServiceId = servletContextDTO.serviceId;
			dto.name             = eDTO.name;
			dto.asyncSupported   = eDTO.asyncSupported;
			dto.serviceId        = eDTO.serviceId;
			dto.servletInfo      = eDTO.servletInfo;
			dto.exceptions       = Arrays.asList(eDTO.exceptions);
			dto.errorCodes       = Arrays.stream(eDTO.errorCodes).boxed().collect(Collectors.toList());
			dto.type             = "Error Page";

			errorPageDTO.add(dto);
		}
		return errorPageDTO;
	}

}
