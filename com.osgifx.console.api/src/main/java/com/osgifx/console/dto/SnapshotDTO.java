/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
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

public class SnapshotDTO extends DTO {

    public List<XBundleDTO>              bundles;
    public List<XComponentDTO>           components;
    public List<XConfigurationDTO>       configurations;
    public List<XPropertyDTO>            properties;
    public List<XServiceDTO>             services;
    public List<XThreadDTO>              threads;
    public XDmtNodeDTO                   dmtNodes;
    public XMemoryInfoDTO                memoryInfo;
    public List<XRoleDTO>                roles;
    public List<XHealthCheckDTO>         healthChecks;
    public Set<XBundleDTO>               classloaderLeaks;
    public List<XHttpComponentDTO>       httpComponents;
    public List<XBundleLoggerContextDTO> bundleLoggerContexts;
    public XHeapUsageDTO                 heapUsage;
    public RuntimeDTO                    runtime;

}