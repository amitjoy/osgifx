/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
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
package com.osgifx.console.agent.admin;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XBundleInfoDTO;
import com.osgifx.console.agent.dto.XServiceDTO;

import jakarta.inject.Inject;

public final class XServiceAdmin {

    private final BundleContext context;
    private final FluentLogger  logger = LoggerFactory.getFluentLogger(getClass());

    @Inject
    public XServiceAdmin(final BundleContext context) {
        this.context = context;
    }

    public List<XServiceDTO> get() {
        requireNonNull(context);
        try {
            final FrameworkDTO dto = context.getBundle(SYSTEM_BUNDLE_ID).adapt(FrameworkDTO.class);
            return dto.services.stream().map(s -> toDTO(s, context)).collect(toList());
        } catch (final Exception e) {
            logger.atError().msg("Error occurred while retrieving services").throwable(e).log();
            return Collections.emptyList();
        }
    }

    private XServiceDTO toDTO(final ServiceReferenceDTO refDTO, final BundleContext context) {
        final XServiceDTO dto = new XServiceDTO();

        final XBundleInfoDTO bundleInfo = new XBundleInfoDTO();
        bundleInfo.id           = refDTO.bundle;
        bundleInfo.symbolicName = bsn(refDTO.bundle, context);

        dto.id                = refDTO.id;
        dto.bundleId          = bundleInfo.id;
        dto.registeringBundle = bundleInfo.symbolicName;
        // @formatter:off
        dto.properties        = refDTO.properties.entrySet()
                                                 .stream()
                                                 .collect(
                                                         toMap(Map.Entry::getKey, e -> arrayToString(e.getValue())));
        dto.usingBundles      = getUsingBundles(refDTO.usingBundles, context);
        dto.types             = getObjectClass(refDTO.properties);
        // @formatter:on

        return dto;
    }

    private List<String> getObjectClass(final Map<String, Object> properties) {
        final Object objectClass = properties.get(OBJECTCLASS);
        return Arrays.asList((String[]) objectClass);
    }

    private String arrayToString(final Object value) {
        if (value instanceof String[]) {
            return Arrays.asList((String[]) value).toString();
        }
        return value.toString();
    }

    private List<XBundleInfoDTO> getUsingBundles(final long[] usingBundles, final BundleContext context) {
        final List<XBundleInfoDTO> bundles = new ArrayList<>();
        for (final long id : usingBundles) {
            final String bsn = bsn(id, context);

            final XBundleInfoDTO dto = new XBundleInfoDTO();
            dto.id           = id;
            dto.symbolicName = bsn;

            bundles.add(dto);
        }
        return bundles;
    }

    private String bsn(final long id, final BundleContext context) {
        // @formatter:off
        return Stream.of(context.getBundles())
                     .filter(b -> b.getBundleId() == id)
                     .map(Bundle::getSymbolicName)
                     .findAny()
                     .orElse(null);
        // @formatter:on
    }

}
