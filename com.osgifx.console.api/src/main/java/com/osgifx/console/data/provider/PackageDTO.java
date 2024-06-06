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
package com.osgifx.console.data.provider;

import java.util.ArrayList;
import java.util.List;
import org.osgi.dto.DTO;
import com.osgifx.console.agent.dto.XBundleDTO;

/**
 * A Data Transfer Object (DTO) representing package information.
 * It includes details about the package name, version, and whether
 * it is duplicated when exported. Additionally, it holds lists of
 * exporters and importers associated with the package.
 */
public class PackageDTO extends DTO {

    /** The name of the package. */
    public String name;

    /** The version of the package. */
    public String version;

    /** Indicates whether the package is duplicated when exported. */
    public boolean isDuplicateExport;

    /** List of bundles exporting this package. */
    public List<XBundleDTO> exporters = new ArrayList<>();

    /** List of bundles importing this package. */
    public List<XBundleDTO> importers = new ArrayList<>();

}
