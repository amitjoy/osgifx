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
package com.osgifx.console.ui.bundles.obr.bnd;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.osgi.resource.Resource;

import com.google.common.collect.Sets;

import aQute.lib.tag.Tag;

/**
 * Can turn an OSGi repository into an
 * {@code http://www.osgi.org/xmlns/repository/v1.0.0} XML file. See the
 * Repository spec in OSGi.
 */
public class XMLResourceGenerator {

    private final Tag           repository = new Tag("repository");
    private final Set<Resource> visited    = Sets.newHashSet();
    private int                 indent     = 0;
    private boolean             compress   = false;

    public XMLResourceGenerator() {
        repository.addAttribute("xmlns", "http://www.osgi.org/xmlns/repository/v1.0.0");
    }

    public void save(OutputStream out) throws IOException {
        try {
            if (compress) {
                out = new GZIPOutputStream(out);
            }
            try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                    var pw = new PrintWriter(writer)) {
                pw.printf("<?xml version='1.0' encoding='UTF-8'?>%n");
                repository.print(indent, pw);
            }
        } finally {
            out.close();
        }
    }

    /**
     * Note that calling {@link #name(String)} sets increment to
     * {@link System#currentTimeMillis()}. In order to retain backward compatibility
     * that is not change. Therefore, in order to specify a value
     * {@link #increment(long)} should be called after.
     *
     * @param name
     * @return this
     */
    public XMLResourceGenerator name(final String name) {
        repository.addAttribute("name", name);
        repository.addAttribute("increment", System.currentTimeMillis());
        return this;
    }

    /**
     * Note that calling {@link #name(String)} sets increment to
     * {@link System#currentTimeMillis()}. In order to retain backward compatibility
     * that is not change. Therefore, in order to specify a value
     * {@link #increment(long)} should be called after.
     *
     * @param increment
     * @return this
     */
    public XMLResourceGenerator increment(final long increment) {
        repository.addAttribute("increment", increment);
        return this;
    }

    public XMLResourceGenerator referral(final URI reference, final int depth) {
        final var referall = new Tag(repository, "referral");
        referall.addAttribute("url", reference);
        if (depth > 0) {
            referall.addAttribute("depth", depth);
        }
        return this;
    }

    public XMLResourceGenerator resources(final Collection<? extends Resource> resources) {
        resources.forEach(this::resource);
        return this;
    }

    public XMLResourceGenerator resource(final Resource resource) {
        if (!visited.contains(resource)) {
            visited.add(resource);

            final var r    = new Tag(repository, "resource");
            final var caps = resource.getCapabilities(null);
            caps.forEach(cap -> {
                final var cr = new Tag(r, "capability");
                cr.addAttribute("namespace", cap.getNamespace());
                directives(cr, cap.getDirectives());
                attributes(cr, cap.getAttributes());
            });

            final var reqs = resource.getRequirements(null);
            reqs.forEach(req -> {
                final var cr = new Tag(r, "requirement");
                cr.addAttribute("namespace", req.getNamespace());
                directives(cr, req.getDirectives());
                attributes(cr, req.getAttributes());
            });
        }
        return this;
    }

    private void directives(final Tag cr, final Map<String, String> directives) {
        directives.forEach((k, v) -> {
            final var d = new Tag(cr, "directive");
            d.addAttribute("name", k);
            d.addAttribute("value", v);
        });
    }

    private void attributes(final Tag cr, final Map<String, Object> attributes) {
        attributes.forEach((k, v) -> {
            if (v == null) {
                return;
            }
            final var ta = TypedAttribute.getTypedAttribute(v);
            if (ta == null) {
                return;
            }
            final var d = new Tag(cr, "attribute");
            d.addAttribute("name", k);
            d.addAttribute("value", ta.value);
            if (ta.type != null) {
                d.addAttribute("type", ta.type);
            }
        });
    }

    public XMLResourceGenerator indent(final int n) {
        indent = n;
        return this;
    }

    public XMLResourceGenerator compress() {
        compress = true;
        return this;
    }
}
