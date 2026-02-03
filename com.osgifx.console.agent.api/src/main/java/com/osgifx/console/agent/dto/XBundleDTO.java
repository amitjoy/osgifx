/*******************************************************************************
 * Copyright 2021-2026 Amit Kumar Mondal
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
package com.osgifx.console.agent.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;

/**
 * Data Transfer Object (DTO) representing information about an OSGi bundle.
 */
public class XBundleDTO extends DTO {

    /** Unique identifier of the bundle. */
    public long id;

    /** Current state of the bundle. */
    public String state;

    /** Location of the bundle. */
    public String location;

    /** Category of the bundle. */
    public String category;

    /** Number of revisions of the bundle. */
    public int revisions;

    /** Indicates if the bundle is a fragment. */
    public boolean isFragment;

    /** Timestamp of the last modification of the bundle. */
    public long lastModified;

    /** Size of the bundle's data folder. */
    public long dataFolderSize;

    /** Documentation associated with the bundle. */
    public String documentation;

    /** Vendor of the bundle. */
    public String vendor;

    /** Version of the bundle. */
    public String version;

    /** Description of the bundle. */
    public String description;

    /** Start level of the bundle. */
    public int startLevel;

    /** Framework start level of the bundle. */
    public int frameworkStartLevel;

    /** Symbolic name of the bundle. */
    public String symbolicName;

    /** Duration taken to start the bundle in milliseconds. */
    public long startDurationInMillis;

    /** Detailed information about the bundle's revision. */
    public BundleRevisionDTO bundleRevision;

    /** Packages exported by the bundle. */
    public List<XPackageDTO> exportedPackages;

    /** Packages imported by the bundle. */
    public List<XPackageDTO> importedPackages;

    /** Bundles wired as providers by the bundle. */
    public List<XBundleInfoDTO> wiredBundlesAsProvider;

    /** Bundles wired as requirers by the bundle. */
    public List<XBundleInfoDTO> wiredBundlesAsRequirer;

    /** Services registered by the bundle. */
    public List<XServiceInfoDTO> registeredServices;

    /** Manifest headers of the bundle. */
    public Map<String, String> manifestHeaders;

    /** Services used by the bundle. */
    public List<XServiceInfoDTO> usedServices;

    /** Host bundles related to the bundle. */
    public List<XBundleInfoDTO> hostBundles;

    /** Fragments attached to the bundle. */
    public List<XBundleInfoDTO> fragmentsAttached;

    /** Indicates if the bundle is persistently started. */
    public boolean isPersistentlyStarted;

    /** Indicates if activation policy is used by the bundle. */
    public boolean isActivationPolicyUsed;

    @Override
    public String toString() {
        return "XBundleDTO [id=" + id + ", symbolicName=" + symbolicName + ", version=" + version + "]";
    }

}
