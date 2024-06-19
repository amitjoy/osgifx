/*******************************************************************************
 * Copyright 2021-2024 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package com.osgifx.console.agent.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

/**
 * A data transfer object (DTO) representing an OSGi service.
 * 
 * It encapsulates information about the service ID, types, bundle ID,
 * properties, bundles using the service, and the registering bundle.
 */
public class XServiceDTO extends DTO {

    /** The ID of the OSGi service */
    public long id;

    /** The types of the OSGi service */
    public List<String> types;

    /** The ID of the bundle registering the service */
    public long bundleId;

    /** The properties associated with the service */
    public Map<String, String> properties;

    /** The list of bundles using this service */
    public List<XBundleInfoDTO> usingBundles;

    /** The symbolic name of the bundle that registered the service */
    public String registeringBundle;

}
