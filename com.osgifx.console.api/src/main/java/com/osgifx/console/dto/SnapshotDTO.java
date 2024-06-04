/*******************************************************************************
 * COPYRIGHT 2021-2024 AMIT KUMAR MONDAL
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.dto;

import java.util.List;
import java.util.Set;

import org.osgi.dto.DTO;

import com.osgifx.console.agent.dto.RuntimeDTO;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;

/**
 * A Data Transfer Object (DTO) that captures a snapshot of various runtime
 * states and configurations. This DTO aggregates different types of
 * information related to the OSGi runtime and its components.
 */
public class SnapshotDTO extends DTO {

    /** A list of bundle data transfer objects. */
    public List<XBundleDTO> bundles;

    /** A list of component data transfer objects. */
    public List<XComponentDTO> components;

    /** A list of configuration data transfer objects. */
    public List<XConfigurationDTO> configurations;

    /** A list of property data transfer objects. */
    public List<XPropertyDTO> properties;

    /** A list of service data transfer objects. */
    public List<XServiceDTO> services;

    /** A list of thread data transfer objects. */
    public List<XThreadDTO> threads;

    /** A data transfer object representing the DMT nodes. */
    public XDmtNodeDTO dmtNodes;

    /** A data transfer object representing the memory information. */
    public XMemoryInfoDTO memoryInfo;

    /** A list of role data transfer objects. */
    public List<XRoleDTO> roles;

    /** A list of health check data transfer objects. */
    public List<XHealthCheckDTO> healthChecks;

    /** A set of bundle data transfer objects indicating classloader leaks. */
    public Set<XBundleDTO> classloaderLeaks;

    /** A list of HTTP component data transfer objects. */
    public List<XHttpComponentDTO> httpComponents;

    /** A list of bundle logger context data transfer objects. */
    public List<XBundleLoggerContextDTO> bundleLoggerContexts;

    /** A data transfer object representing the heap usage. */
    public XHeapUsageDTO heapUsage;

    /** A data transfer object representing the runtime information. */
    public RuntimeDTO runtime;
}
