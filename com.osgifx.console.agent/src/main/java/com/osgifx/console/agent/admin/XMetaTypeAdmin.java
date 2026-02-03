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
package com.osgifx.console.agent.admin;

import static com.osgifx.console.agent.helper.AgentHelper.serviceUnavailable;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.CM;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.METATYPE;
import static java.util.stream.Collectors.toList;
import static org.osgi.service.metatype.ObjectClassDefinition.ALL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XAttributeDefDTO;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XObjectClassDefDTO;

import jakarta.inject.Inject;

public final class XMetaTypeAdmin implements ConfigurationListener {

    private final BundleContext context;
    private final FluentLogger  logger = LoggerFactory.getFluentLogger(getClass());

    private volatile ConfigurationAdmin                            configAdmin;
    private volatile MetaTypeService                               metatypeService;
    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configAdminTracker;
    private ServiceTracker<MetaTypeService, MetaTypeService>       metatypeTracker;

    private final ExecutorService                 executor;
    private final Map<String, XObjectClassDefDTO> ocdCache     = new ConcurrentHashMap<>();
    private final Map<String, XConfigurationDTO>  configCache  = new ConcurrentHashMap<>();
    private final Map<Long, List<String>>         bundleToPids = new ConcurrentHashMap<>();

    private BundleTracker<AtomicReference<Future<?>>>  bundleTracker;
    private ServiceRegistration<ConfigurationListener> configListenerReg;

    @Inject
    public XMetaTypeAdmin(final BundleContext context, final ExecutorService executor) {
        this.context  = context;
        this.executor = executor;
    }

    public void init() {
        configAdminTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(context,
                                                                                        ConfigurationAdmin.class,
                                                                                        null) {
            @Override
            public ConfigurationAdmin addingService(final ServiceReference<ConfigurationAdmin> reference) {
                final ConfigurationAdmin service = super.addingService(reference);
                configAdmin = service;
                registerConfigListener();
                processExistingConfigs();
                return service;
            }

            @Override
            public void removedService(final ServiceReference<ConfigurationAdmin> reference,
                                       final ConfigurationAdmin service) {
                unregisterConfigListener();
                configAdmin = null;
                configCache.clear();
                super.removedService(reference, service);
            }
        };
        configAdminTracker.open();

        metatypeTracker = new ServiceTracker<MetaTypeService, MetaTypeService>(context, MetaTypeService.class, null) {
            @Override
            public MetaTypeService addingService(final ServiceReference<MetaTypeService> reference) {
                final MetaTypeService service = super.addingService(reference);
                metatypeService = service;
                openBundleTracker();
                return service;
            }

            @Override
            public void removedService(final ServiceReference<MetaTypeService> reference,
                                       final MetaTypeService service) {
                closeBundleTracker();
                metatypeService = null;
                super.removedService(reference, service);
            }
        };
        metatypeTracker.open();
    }

    private void openBundleTracker() {
        if (bundleTracker == null) {
            bundleTracker = new BundleTracker<AtomicReference<Future<?>>>(context,
                                                                          Bundle.ACTIVE | Bundle.RESOLVED
                                                                                  | Bundle.STARTING | Bundle.STOPPING,
                                                                          null) {
                @Override
                public AtomicReference<Future<?>> addingBundle(final Bundle bundle, final BundleEvent event) {
                    final AtomicReference<Future<?>> futureRef = new AtomicReference<>();
                    final Future<?>                  future    = executor.submit(() -> processBundle(bundle));
                    futureRef.set(future);
                    return futureRef;
                }

                @Override
                public void modifiedBundle(final Bundle bundle,
                                           final BundleEvent event,
                                           final AtomicReference<Future<?>> futureRef) {
                    final Future<?> future = futureRef.get();
                    if (future != null && !future.isDone()) {
                        future.cancel(true);
                    }
                    removePids(bundleToPids.remove(bundle.getBundleId()));
                    final Future<?> newFuture = executor.submit(() -> processBundle(bundle));
                    futureRef.set(newFuture);
                }

                @Override
                public void removedBundle(final Bundle bundle,
                                          final BundleEvent event,
                                          final AtomicReference<Future<?>> futureRef) {
                    final Future<?> future = futureRef.get();
                    if (future != null && !future.isDone()) {
                        future.cancel(true);
                    }
                    removePids(bundleToPids.remove(bundle.getBundleId()));
                }
            };
            bundleTracker.open();
        }
    }

    private MetaTypeService getMetaTypeService() {
        final MetaTypeService service = metatypeTracker.getService();
        if (service != null) {
            return service;
        }
        return metatypeService;
    }

    private ConfigurationAdmin getConfigAdmin() {
        final ConfigurationAdmin service = configAdminTracker.getService();
        if (service != null) {
            return service;
        }
        return configAdmin;
    }

    private void closeBundleTracker() {
        if (bundleTracker != null) {
            bundleTracker.close();
            bundleTracker = null;
            ocdCache.clear();
            bundleToPids.clear();
        }
    }

    private void registerConfigListener() {
        if (configListenerReg == null) {
            final Dictionary<String, Object> props = new Hashtable<>();
            configListenerReg = context.registerService(ConfigurationListener.class, this, props);
        }
    }

    private void unregisterConfigListener() {
        if (configListenerReg != null) {
            configListenerReg.unregister();
            configListenerReg = null;
            configCache.clear();
        }
    }

    public void stop() {
        closeBundleTracker();
        unregisterConfigListener();
        if (metatypeTracker != null) {
            metatypeTracker.close();
        }
        if (configAdminTracker != null) {
            configAdminTracker.close();
        }
    }

    @Override
    public void configurationEvent(final ConfigurationEvent event) {
        final ConfigurationAdmin configAdmin = getConfigAdmin();
        if (configAdmin == null) {
            return;
        }
        final String pid = event.getPid();
        if (event.getType() == ConfigurationEvent.CM_UPDATED) {
            if (ocdCache.containsKey(pid) || isFactoryConfig(pid)) {
                updateConfigCache(pid);
            }
        } else if (event.getType() == ConfigurationEvent.CM_DELETED) {
            configCache.remove(pid);
        }
    }

    public List<XConfigurationDTO> getConfigurations() {
        final ConfigurationAdmin configAdmin = getConfigAdmin();
        if (configAdmin == null) {
            logger.atWarn().msg(serviceUnavailable(CM)).log();
            return Collections.emptyList();
        }
        final MetaTypeService metatype = getMetaTypeService();
        if (metatype == null) {
            logger.atWarn().msg(serviceUnavailable(METATYPE)).log();
            return Collections.emptyList();
        }
        final List<XConfigurationDTO> result = new ArrayList<>(configCache.values());

        // Add Metatypes that don't have associated configurations
        for (final Map.Entry<String, XObjectClassDefDTO> entry : ocdCache.entrySet()) {
            final String pid = entry.getKey();
            if (!configCache.containsKey(pid)) {
                final XObjectClassDefDTO ocd = entry.getValue();
                result.add(toConfigDTO(null, pid, ocd));
            }
        }
        return result;
    }

    private List<String> processBundle(final Bundle bundle) {
        final List<String>    pids     = new ArrayList<>();
        final MetaTypeService metatype = getMetaTypeService();
        if (metatype == null) {
            return pids;
        }
        final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
        if (metatypeInfo == null) {
            return pids;
        }

        for (final String pid : metatypeInfo.getPids()) {
            final XObjectClassDefDTO ocd = toOcdDTO(pid, metatypeInfo, ConfigurationType.SINGLETON);
            ocdCache.put(pid, ocd);
            pids.add(pid);
            // Check if config exists for this PID and update cache
            updateConfigCache(pid);
        }

        for (final String fpid : metatypeInfo.getFactoryPids()) {
            final XObjectClassDefDTO ocd = toOcdDTO(fpid, metatypeInfo, ConfigurationType.FACTORY);
            ocdCache.put(fpid, ocd);
            pids.add(fpid);
            // For factory PIDs, we need to find all configs with this factory PID
            updateFactoryConfigCache(fpid);
        }

        bundleToPids.put(bundle.getBundleId(), pids);
        return pids;
    }

    private void removePids(final List<String> pids) {
        if (pids != null) {
            for (final String pid : pids) {
                ocdCache.remove(pid);
                configCache.remove(pid);
                configCache.entrySet().removeIf(e -> {
                    final XConfigurationDTO dto = e.getValue();
                    return dto.ocd != null && pid.equals(dto.ocd.id);
                });
            }
        }
    }

    private void processExistingConfigs() {
        final ConfigurationAdmin configAdmin = getConfigAdmin();
        if (configAdmin == null) {
            return;
        }
        try {
            final Configuration[] configs = configAdmin.listConfigurations(null);
            if (configs != null) {
                for (final Configuration config : configs) {
                    executor.submit(() -> {
                        final String pid        = config.getPid();
                        final String factoryPid = config.getFactoryPid();

                        if (factoryPid != null && ocdCache.containsKey(factoryPid)) {
                            configCache.put(pid, toConfigDTO(config, null, ocdCache.get(factoryPid)));
                        } else if (ocdCache.containsKey(pid)) {
                            configCache.put(pid, toConfigDTO(config, null, ocdCache.get(pid)));
                        }
                    });
                }
            }
        } catch (final Exception e) {
            logger.atError().msg("Error processing existing configurations").throwable(e).log();
        }
    }

    private void updateConfigCache(final String pid) {
        final ConfigurationAdmin configAdmin = getConfigAdmin();
        if (configAdmin == null) {
            return;
        }
        try {
            final Configuration config = configAdmin.getConfiguration(pid, "?");
            // Check if it matches a singleton PID
            if (ocdCache.containsKey(pid)) {
                configCache.put(pid, toConfigDTO(config, null, ocdCache.get(pid)));
            }
            // Check if it matches a factory PID
            final String factoryPid = config.getFactoryPid();
            if (factoryPid != null && ocdCache.containsKey(factoryPid)) {
                configCache.put(pid, toConfigDTO(config, null, ocdCache.get(factoryPid)));
            }
        } catch (final Exception e) {
            // Ignore if config doesn't exist or error
        }
    }

    private void updateFactoryConfigCache(final String factoryPid) {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return;
        }
        try {
            final Configuration[] configs = configAdmin.listConfigurations("(service.factoryPid=" + factoryPid + ")");
            if (configs != null) {
                for (final Configuration config : configs) {
                    configCache.put(config.getPid(), toConfigDTO(config, null, ocdCache.get(factoryPid)));
                }
            }
        } catch (final Exception e) {
            logger.atError().msg("Error updating factory config cache").throwable(e).log();
        }
    }

    private boolean isFactoryConfig(final String pid) {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return false;
        }
        try {
            final Configuration config = configAdmin.getConfiguration(pid, "?");
            return config.getFactoryPid() != null && ocdCache.containsKey(config.getFactoryPid());
        } catch (Exception e) {
            return false;
        }
    }

    private XConfigurationDTO toConfigDTO(final Configuration configuration,
                                          final String metatypePID,
                                          final XObjectClassDefDTO ocd) {
        final XConfigurationDTO dto = new XConfigurationDTO();

        dto.ocd         = ocd;
        dto.isPersisted = configuration != null;
        dto.pid         = Optional.ofNullable(configuration).map(Configuration::getPid).orElse(metatypePID);
        dto.factoryPid  = Optional.ofNullable(configuration).map(Configuration::getFactoryPid)
                .orElse((ocd != null && ocd.pid == null) ? ocd.factoryPid : null);
        // If ocd is Factory, ocd.pid is null, ocd.factoryPid is set.

        dto.properties = XConfigurationAdmin.prepareConfiguration(configuration);
        dto.location   = Optional.ofNullable(configuration).map(Configuration::getBundleLocation).orElse(null);
        if (dto.factoryPid != null && configuration == null) {
            dto.isFactory = true;
        }

        return dto;
    }

    private XObjectClassDefDTO toOcdDTO(final String ocdId,
                                        final MetaTypeInformation metatypeInfo,
                                        final ConfigurationType type) {
        final ObjectClassDefinition ocd = metatypeInfo.getObjectClassDefinition(ocdId, null);
        final XObjectClassDefDTO    dto = new XObjectClassDefDTO();

        dto.id                 = ocd.getID();
        dto.pid                = type == ConfigurationType.SINGLETON ? ocdId : null;
        dto.factoryPid         = type == ConfigurationType.FACTORY ? ocdId : null;
        dto.name               = ocd.getName();
        dto.description        = ocd.getDescription();
        dto.descriptorLocation = metatypeInfo.getBundle().getSymbolicName();
        dto.attributeDefs      = Stream.of(ocd.getAttributeDefinitions(ALL)).map(this::toAdDTO).collect(toList());

        return dto;
    }

    private XAttributeDefDTO toAdDTO(final AttributeDefinition ad) {
        final XAttributeDefDTO dto = new XAttributeDefDTO();

        dto.id           = ad.getID();
        dto.name         = ad.getName();
        dto.description  = ad.getDescription();
        dto.cardinality  = ad.getCardinality();
        dto.type         = defType(ad.getType(), ad.getCardinality()).ordinal();
        dto.optionValues = Optional.ofNullable(ad.getOptionLabels()).map(Arrays::asList).orElse(null);
        dto.defaultValue = Optional.ofNullable(ad.getDefaultValue()).map(Arrays::asList).orElse(null);

        return dto;
    }

    public boolean isMetatype(final Configuration config) {
        final String pid = config.getPid();
        if (ocdCache.containsKey(pid)) {
            return true;
        }
        final String factoryPid = config.getFactoryPid();
        return factoryPid != null && ocdCache.containsKey(factoryPid);
    }

    private static XAttributeDefType defType(final int defType, final int cardinality) {
        switch (defType) {
            case AttributeDefinition.STRING:
                if (cardinality > 0) {
                    return XAttributeDefType.STRING_ARRAY;
                }
                if (cardinality < 0) {
                    return XAttributeDefType.STRING_LIST;
                }
                return XAttributeDefType.STRING;
            case AttributeDefinition.LONG:
                if (cardinality > 0) {
                    return XAttributeDefType.LONG_ARRAY;
                }
                if (cardinality < 0) {
                    return XAttributeDefType.LONG_LIST;
                }
                return XAttributeDefType.LONG;
            case AttributeDefinition.INTEGER:
                if (cardinality > 0) {
                    return XAttributeDefType.INTEGER_ARRAY;
                }
                if (cardinality < 0) {
                    return XAttributeDefType.INTEGER_LIST;
                }
                return XAttributeDefType.INTEGER;
            case AttributeDefinition.CHARACTER:
                if (cardinality > 0) {
                    return XAttributeDefType.CHAR_ARRAY;
                }
                if (cardinality < 0) {
                    return XAttributeDefType.CHAR_LIST;
                }
                return XAttributeDefType.CHAR;
            case AttributeDefinition.DOUBLE:
                if (cardinality > 0) {
                    return XAttributeDefType.DOUBLE_ARRAY;
                }
                if (cardinality < 0) {
                    return XAttributeDefType.DOUBLE_LIST;
                }
                return XAttributeDefType.DOUBLE;
            case AttributeDefinition.FLOAT:
                if (cardinality > 0) {
                    return XAttributeDefType.FLOAT_ARRAY;
                }
                if (cardinality < 0) {
                    return XAttributeDefType.FLOAT_LIST;
                }
                return XAttributeDefType.FLOAT;
            case AttributeDefinition.BOOLEAN:
                if (cardinality > 0) {
                    return XAttributeDefType.BOOLEAN_ARRAY;
                }
                if (cardinality < 0) {
                    return XAttributeDefType.BOOLEAN_LIST;
                }
                return XAttributeDefType.BOOLEAN;
            case AttributeDefinition.PASSWORD:
                return XAttributeDefType.PASSWORD;
            default:
                return XAttributeDefType.STRING;
        }
    }

    private enum ConfigurationType {
        SINGLETON,
        FACTORY
    }

}
