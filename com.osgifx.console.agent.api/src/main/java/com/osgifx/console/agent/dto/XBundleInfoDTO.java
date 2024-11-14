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

package com.osgifx.console.agent.dto;

import org.osgi.dto.DTO;

/**
 * A data transfer object (DTO) representing information about an OSGi bundle.
 * It includes the bundle's ID and symbolic name.
 */
public class XBundleInfoDTO extends DTO {

    /** The unique identifier of the OSGi bundle */
    public long id;

    /** The symbolic name of the OSGi bundle */
    public String symbolicName;

}
