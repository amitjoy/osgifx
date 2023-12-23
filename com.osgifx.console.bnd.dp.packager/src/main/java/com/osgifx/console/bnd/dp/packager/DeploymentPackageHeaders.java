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
package com.osgifx.console.bnd.dp.packager;

import aQute.bnd.osgi.Constants;

public final class DeploymentPackageHeaders {

    private DeploymentPackageHeaders() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static final String DEPLOYMENT_PACKAGE_SYMBOLIC_NAME    = "DeploymentPackage-SymbolicName";
    public static final String DEPLOYMENT_PACKAGE_VERSION          = "DeploymentPackage-Version";
    public static final String DEPLOYMENT_PACKAGE_FIX_PACK         = "DeploymentPackage-FixPack";
    public static final String DEPLOYMENT_PACKAGE_NAME             = "DeploymentPackage-Name";
    public static final String DEPLOYMENT_PACKAGE_COPYRIGHT        = "DeploymentPackage-Copyright";
    public static final String DEPLOYMENT_PACKAGE_CONTACT_ADDRESS  = "DeploymentPackage-ContactAddress";
    public static final String DEPLOYMENT_PACKAGE_DESCRIPTION      = "DeploymentPackage-Description";
    public static final String DEPLOYMENT_PACKAGE_DOC_URL          = "DeploymentPackage-DocURL";
    public static final String DEPLOYMENT_PACKAGE_ICON             = "DeploymentPackage-Icon";
    public static final String DEPLOYMENT_PACKAGE_VENDOR           = "DeploymentPackage-Vendor";
    public static final String DEPLOYMENT_PACKAGE_LICENSE          = "DeploymentPackage-License";
    public static final String DEPLOYMENT_PACKAGE_REQUIRED_STORAGE = "DeploymentPackage-RequiredStorage";
    public static final String DEPLOYMENT_PACKAGE_CUSTOMIZER       = "DeploymentPackage-Customizer";

    public static final String DEPLOYMENT_PACKAGE_RESOURCE_SHA_DIGEST           = "SHA-1-Digest";
    public static final String DEPLOYMENT_PACKAGE_RESOURCE_PROCESSOR_PID        = "Resource-Processor";
    public static final String DEPLOYMENT_PACKAGE_RESOURCE_BUNDLE_SYMBOLIC_NAME = Constants.BUNDLE_SYMBOLICNAME;
    public static final String DEPLOYMENT_PACKAGE_RESOURCE_BUNDLE_VERSION       = Constants.BUNDLE_VERSION;

}
