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
 * Represents the availability of an optional OSGi compendium feature on the
 * connected remote runtime.
 *
 * <p>
 * Each instance corresponds to one entry in {@code PackageWirings.Type} and
 * reflects whether the required packages for that feature are currently wired
 * into the agent bundle at the time the snapshot was taken.
 * </p>
 *
 * @see com.osgifx.console.agent.Agent#getRuntimeCapabilities()
 * @since 12.0
 */
public class XRuntimeCapabilityDTO extends DTO {

    /**
     * The stable enum key corresponding to {@code PackageWirings.Type.name()}.
     * Suitable for programmatic comparisons (e.g. {@code "JAX_RS"}, {@code "SCR"}).
     */
    public String id;

    /**
     * The human-readable name of the capability
     * (e.g. {@code "JAX-RS"}, {@code "Config Admin"}).
     */
    public String name;

    /**
     * {@code true} if the required packages for this feature are wired into
     * the agent bundle; {@code false} if the feature is absent from the runtime.
     */
    public boolean isAvailable;

}
