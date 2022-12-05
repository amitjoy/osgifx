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

import static aQute.lib.collections.Logic.retain;
import static java.util.Collections.unmodifiableList;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import com.google.common.collect.Lists;

class ResourceImpl implements Resource, Comparable<Resource> {

    private volatile List<Capability>               allCapabilities;
    private volatile Map<String, List<Capability>>  capabilityMap;
    private volatile List<Requirement>              allRequirements;
    private volatile Map<String, List<Requirement>> requirementMap;
    private volatile transient Map<URI, String>     locations;

    void setCapabilities(final Collection<Capability> capabilities) {
        final Map<String, List<Capability>> prepare = new HashMap<>();
        for (final Capability capability : capabilities) {
            var list = prepare.get(capability.getNamespace());
            if (list == null) {
                list = new LinkedList<>();
                prepare.put(capability.getNamespace(), list);
            }
            list.add(capability);
        }
        for (final Map.Entry<String, List<Capability>> entry : prepare.entrySet()) {
            entry.setValue(unmodifiableList(Lists.newArrayList(entry.getValue())));
        }
        allCapabilities = unmodifiableList(Lists.newArrayList(capabilities));
        capabilityMap   = prepare;
        locations       = null;                                              // clear so equals/hashCode can recompute
    }

    @Override
    public List<Capability> getCapabilities(final String namespace) {
        final var caps = namespace != null ? capabilityMap != null ? capabilityMap.get(namespace) : null
                : allCapabilities;
        return caps != null ? caps : Collections.emptyList();
    }

    void setRequirements(final Collection<Requirement> requirements) {
        final Map<String, List<Requirement>> prepare = new HashMap<>();
        for (final Requirement requirement : requirements) {
            var list = prepare.get(requirement.getNamespace());
            if (list == null) {
                list = new LinkedList<>();
                prepare.put(requirement.getNamespace(), list);
            }
            list.add(requirement);
        }
        for (final Map.Entry<String, List<Requirement>> entry : prepare.entrySet()) {
            entry.setValue(unmodifiableList(Lists.newArrayList(entry.getValue())));
        }
        allRequirements = unmodifiableList(Lists.newArrayList(requirements));
        requirementMap  = prepare;
    }

    @Override
    public List<Requirement> getRequirements(final String namespace) {
        final var reqs = namespace != null ? requirementMap != null ? requirementMap.get(namespace) : null
                : allRequirements;
        return reqs != null ? reqs : Collections.emptyList();
    }

    @Override
    public String toString() {
        final var builder    = new StringBuilder();
        final var identities = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (identities.size() == 1) {
            final var idCap = identities.get(0);
            final var id    = idCap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
            builder.append(id);
            final var version = idCap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
            if (version != null) {
                builder.append(" version=").append(version);
            }
        } else {
            // Generic toString
            builder.append("ResourceImpl [caps=");
            builder.append(allCapabilities);
            builder.append(", reqs=");
            builder.append(allRequirements);
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public int compareTo(final Resource o) {
        return ResourceUtils.compareTo(this, o);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Resource)) {
            return false;
        }
        final var        thisLocations = getLocations();
        Map<URI, String> otherLocations;
        if (other instanceof final ResourceImpl otherRes) {
            otherLocations = otherRes.getLocations();
        } else {
            otherLocations = ResourceUtils.getLocations((Resource) other);
        }
        final Collection<URI> overlap = retain(thisLocations.keySet(), otherLocations.keySet());
        for (final URI uri : overlap) {
            final var thisSha  = thisLocations.get(uri);
            final var otherSha = otherLocations.get(uri);
            if (thisSha == otherSha || thisSha != null && otherSha != null && thisSha.equals(otherSha)) {
                return true;
            }
        }
        return false;
    }

    private Map<URI, String> getLocations() {
        final var map = locations;
        if (map != null) {
            return map;
        }
        return locations = ResourceUtils.getLocations(this);
    }

    @Override
    public int hashCode() {
        return getLocations().hashCode();
    }

}
