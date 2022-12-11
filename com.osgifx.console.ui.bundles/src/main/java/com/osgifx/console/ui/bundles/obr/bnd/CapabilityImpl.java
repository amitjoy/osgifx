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
package com.osgifx.console.ui.bundles.obr.bnd;

import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

public class CapabilityImpl extends CapReq implements Capability {

    CapabilityImpl(final String namespace,
                   final Resource resource,
                   final Map<String, String> directives,
                   final Map<String, Object> attributes) {
        super(MODE.Capability, namespace, resource, directives, attributes);
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        sb.append("Provide");
        super.toString(sb);
        return sb.toString();
    }
}