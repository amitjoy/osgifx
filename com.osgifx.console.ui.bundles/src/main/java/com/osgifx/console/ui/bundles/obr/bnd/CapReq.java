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

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class CapReq {

	enum MODE {
		Capability, Requirement
	}

	private final MODE                mode;
	private final String              namespace;
	private final Resource            resource;
	private final Map<String, String> directives;
	private final Map<String, Object> attributes;
	private transient int             hashCode = 0;

	CapReq(final MODE mode, final String namespace, final Resource resource, final Map<String, String> directives,
	        final Map<String, Object> attributes) {
		this.mode       = requireNonNull(mode);
		this.namespace  = requireNonNull(namespace);
		this.resource   = resource;
		this.directives = unmodifiableMap(new HashMap<>(directives));
		this.attributes = unmodifiableMap(new HashMap<>(attributes));
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String, String> getDirectives() {
		return directives;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Resource getResource() {
		return resource;
	}

	@Override
	public int hashCode() {
		if (hashCode != 0) {
			return hashCode;
		}
		return hashCode = Objects.hash(attributes, directives, mode, namespace, resource);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof CapReq) {
			return equalsNative((CapReq) obj);
		}
		if (mode == MODE.Capability && obj instanceof Capability) {
			return equalsCap((Capability) obj);
		}
		if (mode == MODE.Requirement && obj instanceof Requirement) {
			return equalsReq((Requirement) obj);
		}
		return false;
	}

	private boolean equalsCap(final Capability other) {
		if (!Objects.equals(namespace, other.getNamespace()) || !Objects.equals(attributes, other.getAttributes())
		        || !Objects.equals(directives, other.getDirectives())) {
			return false;
		}
		return Objects.equals(resource, other.getResource());
	}

	private boolean equalsNative(final CapReq other) {
		if (mode != other.mode || !Objects.equals(namespace, other.getNamespace()) || !Objects.equals(attributes, other.getAttributes())
		        || !Objects.equals(directives, other.getDirectives())) {
			return false;
		}
		return Objects.equals(resource, other.getResource());
	}

	private boolean equalsReq(final Requirement other) {
		if (!Objects.equals(namespace, other.getNamespace()) || !Objects.equals(attributes, other.getAttributes())
		        || !Objects.equals(directives, other.getDirectives())) {
			return false;
		}
		return Objects.equals(resource, other.getResource());
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		if (mode == MODE.Capability) {
			final var value = attributes.get(namespace);
			builder.append(namespace).append('=').append(value);
		} else {
			final var filter = directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			builder.append(filter);
			if (Namespace.RESOLUTION_OPTIONAL.equals(directives.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
				builder.append("%OPT");
			}
		}
		return builder.toString();
	}

	protected void toString(final StringBuilder sb) {
		sb.append("[").append(namespace).append("]");
		sb.append(attributes);
		sb.append(directives);
	}

}
