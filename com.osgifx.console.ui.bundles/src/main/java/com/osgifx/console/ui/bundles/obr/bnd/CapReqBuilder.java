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
package com.osgifx.console.ui.bundles.obr.bnd;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.osgifx.console.ui.bundles.obr.bnd.ResourceUtils.CONTENT_NAMESPACE;
import static org.osgi.framework.namespace.BundleNamespace.BUNDLE_NAMESPACE;
import static org.osgi.framework.namespace.ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE;
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;
import static org.osgi.namespace.contract.ContractNamespace.CONTRACT_NAMESPACE;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.resource.Namespace.REQUIREMENT_FILTER_DIRECTIVE;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.function.Failable;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.RequirementDTO;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import aQute.lib.converter.Converter;

public class CapReqBuilder {

    private final String              namespace;
    private Resource                  resource;
    private final Map<String, Object> attributes = Maps.newHashMap();
    private final Map<String, String> directives = Maps.newHashMap();

    public CapReqBuilder(final String namespace) {
        this.namespace = checkNotNull(namespace);
    }

    public CapReqBuilder(final Resource resource, final String namespace) {
        this(namespace);
        setResource(resource);
    }

    public static CapReqBuilder clone(final CapabilityDTO capability) {
        final var builder = new CapabilityBuilder(capability.namespace);

        builder.addAttributes(capability.attributes);
        builder.addDirectives(capability.directives);

        return builder;
    }

    public static CapReqBuilder clone(final RequirementDTO requirement) {
        final var builder = new RequirementBuilder(requirement.namespace);

        builder.addAttributes(requirement.attributes);
        builder.addDirectives(requirement.directives);

        return builder;
    }

    public String getNamespace() {
        return namespace;
    }

    public Resource getResource() {
        return resource;
    }

    public CapReqBuilder setResource(final Resource resource) {
        this.resource = resource;
        return this;
    }

    public CapReqBuilder addAttribute(final String name, Object value) throws Exception {
        if (value == null) {
            return this;
        }
        if (value.getClass().isArray()) {
            value = Converter.cnv(List.class, value);
        }
        if (name.equals(ResourceUtils.getVersionAttributeForNamespace(namespace))) {
            value = toVersions(value);
        }
        attributes.put(name, value);
        return this;
    }

    public boolean isVersion(final Object value) {
        if (value instanceof Version) {
            return true;
        }
        if (value instanceof Collection) {
            if (((Collection<?>) value).isEmpty()) {
                return true;
            }
            return isVersion(((Collection<?>) value).iterator().next());
        }
        if (value.getClass().isArray()) {
            if (Array.getLength(value) == 0) {
                return true;
            }
            return isVersion(((Object[]) value)[0]);
        }
        return false;
    }

    public CapReqBuilder addAttributes(final Map<String, ? extends Object> attributes) {
        Failable.stream(attributes.entrySet()).forEach(e -> addAttribute(e.getKey(), e.getValue()));
        return this;
    }

    public CapReqBuilder addDirective(final String name, final String value) {
        if (value == null) {
            return this;
        }
        directives.put(ResourceUtils.stripDirective(name), value);
        return this;
    }

    public CapReqBuilder addDirectives(final Map<String, String> directives) {
        Failable.stream(directives.entrySet()).forEach(e -> addDirective(e.getKey(), e.getValue()));
        return this;
    }

    public Capability buildCapability() {
        return new CapabilityImpl(namespace, resource, directives, attributes);
    }

    public Capability buildSyntheticCapability() {
        return new CapabilityImpl(namespace, resource, directives, attributes);
    }

    public Requirement buildRequirement() {
        verifyNotNull(resource, "Cannot build Requirement with null Resource. use buildSyntheticRequirement");
        return new RequirementImpl(namespace, resource, directives, attributes);
    }

    public Requirement buildSyntheticRequirement() {
        return new RequirementImpl(namespace, null, directives, attributes);
    }

    public CapReqBuilder filter(final CharSequence f) {
        return addDirective("filter", f.toString());
    }

    public CapReqBuilder from(final Capability c) {
        addAttributes(c.getAttributes());
        addDirectives(c.getDirectives());
        return this;
    }

    public CapReqBuilder from(final Requirement r) {
        addAttributes(r.getAttributes());
        addDirectives(r.getDirectives());
        return this;
    }

    public static Capability copy(final Capability c, final Resource r) {
        final var from = new CapReqBuilder(c.getNamespace()).from(c);
        if (r == null) {
            return from.buildSyntheticCapability();
        }
        return from.setResource(r).buildCapability();
    }

    public static Requirement copy(final Requirement c, final Resource r) {
        final var from = new CapReqBuilder(c.getNamespace()).from(c);
        if (r == null) {
            return from.buildSyntheticRequirement();
        }
        return from.setResource(r).buildRequirement();
    }

    /**
     * If value must contain one of the characters reverse solidus ('\' \u005C),
     * asterisk ('*' \u002A), parentheses open ('(' \u0028) or parentheses close
     * (')' \u0029), then these characters should be preceded with the reverse
     * solidus ('\' \u005C) character. Spaces are significant in value. Space
     * characters are defined by Character.isWhiteSpace().
     */

    private static final Pattern ESCAPE_FILTER_VALUE_P = Pattern.compile("[\\\\*()]");

    public static String escapeFilterValue(final String value) {
        return ESCAPE_FILTER_VALUE_P.matcher(value).replaceAll("\\\\$0");
    }

    public void and(final String... s) {
        final var previous = directives == null ? null : directives.get(REQUIREMENT_FILTER_DIRECTIVE);
        final var filter   = new StringBuilder();

        if (previous != null) {
            filter.append("(&").append(previous);
        }
        for (final String subexpr : s) {
            filter.append(subexpr);
        }
        if (previous != null) {
            filter.append(")");
        }
        addDirective(REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
    }

    public boolean isPackage() {
        return PACKAGE_NAMESPACE.equals(getNamespace());
    }

    public boolean isHost() {
        return HOST_NAMESPACE.equals(getNamespace());
    }

    public boolean isBundle() {
        return BUNDLE_NAMESPACE.equals(getNamespace());
    }

    public boolean isService() {
        return SERVICE_NAMESPACE.equals(getNamespace());
    }

    public boolean isContract() {
        return CONTRACT_NAMESPACE.equals(getNamespace());
    }

    public boolean isIdentity() {
        return IDENTITY_NAMESPACE.equals(getNamespace());
    }

    public boolean isContent() {
        return CONTENT_NAMESPACE.equals(getNamespace());
    }

    public boolean isEE() {
        return EXECUTION_ENVIRONMENT_NAMESPACE.equals(getNamespace());
    }

    public boolean isExtender() {
        return EXTENDER_NAMESPACE.equals(getNamespace());
    }

    private Object toVersions(final Object value) {
        if (value instanceof Version) {
            return value;
        }
        if (value instanceof Version) {
            return new Version(value.toString());
        }
        if (value instanceof final String strValue) {
            try {
                return new Version(strValue);
            } catch (final Exception e) {
                return value;
            }
        }
        if (value instanceof final Number num) {
            try {
                return new Version(num.intValue(), 0, 0);
            } catch (final Exception e) {
                return value;
            }
        }
        if (value instanceof final Collection<?> coll) {
            if (coll.isEmpty() || coll.iterator().next() instanceof Version) {
                return value;
            }
            final List<Version> osgis = Lists.newArrayList();
            for (final Object m : (Collection<?>) value) {
                osgis.add((Version) toVersions(m));
            }
            return osgis;
        }
        throw new IllegalArgumentException("cannot convert " + value
                + " to an org.osgi.framework Version(s) object as requested");
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        sb.append('[').append(namespace).append('[');
        sb.append(attributes);
        sb.append(directives);
        return sb.toString();
    }
}
