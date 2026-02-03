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

package com.osgifx.console.agent.dto;

import org.osgi.dto.DTO;

/**
 * A data transfer object (DTO) representing information about an OSGi package.
 * It includes the package's name, version, and type (either export or import).
 */
public class XPackageDTO extends DTO {

    /** The name of the OSGi package */
    public String name;

    /** The version of the OSGi package */
    public String version;

    /** The type of the OSGi package (either EXPORT or IMPORT) */
    public XPackageType type;

    @Override
    public String toString() {
        return "XPackageDTO [name=" + name + ", version=" + version + ", type=" + type + "]";
    }

}
