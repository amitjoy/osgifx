/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
import static java.util.stream.Collectors.toMap;
import static org.osgi.framework.Constants.OBJECTCLASS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.util.tracker.ServiceTracker;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XBundleInfoDTO;
import com.osgifx.console.agent.dto.XServiceDTO;

import jakarta.inject.Inject;

public final class XServiceAdmin {

    private final Map<Long, XServiceDTO>      services = new ConcurrentHashMap<>();
    private final BundleContext               context;
    private final FluentLogger                logger   = LoggerFactory.getFluentLogger(getClass());
    private ServiceTracker<Object, Future<?>> serviceTracker;
    private ExecutorService                   executor;

    @Inject
    public XServiceAdmin(final BundleContext context, final ExecutorService executor) {
        this.context  = context;
        this.executor = executor;
    }

    public void init() {
        try {

            serviceTracker = new ServiceTracker<Object, Future<?>>(context, context.createFilter("(objectClass=*)"),
                                                                   null) {
                @Override
                public Future<?> addingService(final ServiceReference<Object> reference) {
                    return executor.submit(() -> {
                        final XServiceDTO dto = toDTO(reference);
                        services.put(dto.id, dto);
                    });
                }

                @Override
                public void modifiedService(final ServiceReference<Object> reference, final Future<?> future) {
                    executor.submit(() -> {
                        final XServiceDTO dto = toDTO(reference);
                        services.put(dto.id, dto);
                    });
                }

                @Override
                public void removedService(final ServiceReference<Object> reference, final Future<?> future) {
                    if (future != null && !future.isDone()) {
                        future.cancel(true);
                    }
                    final long id = (Long) reference.getProperty(org.osgi.framework.Constants.SERVICE_ID);
                    services.remove(id);
                }
            };
            serviceTracker.open();
        } catch (final InvalidSyntaxException e) {
            logger.atError().msg("Error occurred while initializing service tracker").throwable(e).log();
        }
    }

    public void stop() {
        if (serviceTracker != null) {
            serviceTracker.close();
        }
    }

    public List<XServiceDTO> get() {
        requireNonNull(context);
        return new ArrayList<>(services.values());
    }

    private XServiceDTO toDTO(final ServiceReference<?> ref) {
        final ServiceReferenceDTO refDTO = toServiceReferenceDTO(ref);
        return toDTO(refDTO, context);
    }

    private ServiceReferenceDTO toServiceReferenceDTO(final ServiceReference<?> ref) {
        final ServiceReferenceDTO dto = new ServiceReferenceDTO();
        dto.id     = (Long) ref.getProperty(org.osgi.framework.Constants.SERVICE_ID);
        dto.bundle = ref.getBundle().getBundleId();

        dto.properties = new HashMap<>();
        for (final String key : ref.getPropertyKeys()) {
            dto.properties.put(key, ref.getProperty(key));
        }

        final Bundle[] usingBundles = ref.getUsingBundles();
        if (usingBundles != null) {
            dto.usingBundles = Stream.of(usingBundles).mapToLong(Bundle::getBundleId).toArray();
        } else {
            dto.usingBundles = new long[0];
        }
        return dto;
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
        final Bundle bundle = context.getBundle(id);
        return bundle == null ? null : bundle.getSymbolicName();
    }

}
