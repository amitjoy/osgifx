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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.RequirementDTO;

public class ResourceBuilder {
	private final ResourceImpl                  resource     = new ResourceImpl();
	private final Map<Capability, Capability>   capabilities = new LinkedHashMap<>();
	private final Map<Requirement, Requirement> requirements = new LinkedHashMap<>();

	private boolean built = false;

	public ResourceBuilder addCapability(final CapabilityDTO capability) throws Exception {
		final var builder = CapReqBuilder.clone(capability);
		return addCapability(builder);
	}

	public ResourceBuilder addCapability(final CapReqBuilder builder) {
		if (builder == null) {
			return this;
		}

		if (built) {
			throw new IllegalStateException("Resource already built");
		}

		addCapability0(builder);

		return this;
	}

	private Capability addCapability0(final CapReqBuilder builder) {
		final var cap      = builder.setResource(resource).buildCapability();
		final var previous = capabilities.putIfAbsent(cap, cap);
		if (previous != null) {
			return previous;
		}
		return cap;
	}

	public void addCapabilities(final List<CapabilityDTO> capabilities) throws Exception {
		if (capabilities == null || capabilities.isEmpty()) {
			return;
		}
		for (final CapabilityDTO c : capabilities) {
			addCapability(c);
		}
	}

	public ResourceBuilder addRequirement(final RequirementDTO requirement) throws Exception {
		if (requirement == null) {
			return this;
		}
		final var builder = CapReqBuilder.clone(requirement);
		return addRequirement(builder);
	}

	public ResourceBuilder addRequirement(final CapReqBuilder builder) {
		if (builder == null) {
			return this;
		}
		if (built) {
			throw new IllegalStateException("Resource already built");
		}
		addRequirement0(builder);
		return this;
	}

	private Requirement addRequirement0(final CapReqBuilder builder) {
		final var req      = builder.setResource(resource).buildRequirement();
		final var previous = requirements.putIfAbsent(req, req);
		if (previous != null) {
			return previous;
		}
		return req;
	}

	public void addRequirements(final List<RequirementDTO> requirements) throws Exception {
		if (requirements == null || requirements.isEmpty()) {
			return;
		}
		for (final RequirementDTO rq : requirements) {
			addRequirement(rq);
		}
	}

	public Resource build() {
		if (built) {
			throw new IllegalStateException("Resource already built");
		}
		built = true;
		resource.setCapabilities(capabilities.values());
		resource.setRequirements(requirements.values());
		return resource;
	}

	public List<Capability> getCapabilities() {
		return new ArrayList<>(capabilities.values());
	}

	public List<Requirement> getRequirements() {
		return new ArrayList<>(requirements.values());
	}

}