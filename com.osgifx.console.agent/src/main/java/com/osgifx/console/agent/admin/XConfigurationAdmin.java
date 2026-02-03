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

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.agent.helper.AgentHelper.createResult;
import static com.osgifx.console.agent.helper.AgentHelper.serviceUnavailable;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.CM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XComponentReferenceFilterDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.dto.XSatisfiedReferenceDTO;
import com.osgifx.console.agent.dto.XUnsatisfiedReferenceDTO;
import com.osgifx.console.agent.helper.AgentHelper;
import com.osgifx.console.agent.helper.Reflect;

import jakarta.inject.Inject;

public final class XConfigurationAdmin implements ConfigurationListener {

    private final BundleContext                                    context;
    private final XComponentAdmin                                  componentAdmin;
    private final XMetaTypeAdmin                                   xMetaTypeAdmin;
    private final FluentLogger                                     logger         = LoggerFactory
            .getFluentLogger(getClass());
    private final Map<String, XConfigurationDTO>                   configurations = new ConcurrentHashMap<>();
    private ServiceRegistration<ConfigurationListener>             registration;
    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configAdminTracker;
    private volatile ConfigurationAdmin                            configAdmin;
    private ExecutorService                                        executor;

    @Inject
    public XConfigurationAdmin(final BundleContext context,
                               final XComponentAdmin componentAdmin,
                               final XMetaTypeAdmin xMetaTypeAdmin,
                               final ExecutorService executor) {
        this.context        = context;
        this.componentAdmin = componentAdmin;
        this.xMetaTypeAdmin = xMetaTypeAdmin;
        this.executor       = executor;
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
                processExistingConfigs(service);
                return service;
            }

            @Override
            public void removedService(final ServiceReference<ConfigurationAdmin> reference,
                                       final ConfigurationAdmin service) {
                unregisterConfigListener();
                configurations.clear();
                configAdmin = null;
                super.removedService(reference, service);
            }
        };
        configAdminTracker.open();
    }

    public void stop() {
        if (configAdminTracker != null) {
            configAdminTracker.close();
        }
    }

    private void registerConfigListener() {
        if (registration == null) {
            registration = context.registerService(ConfigurationListener.class, this, null);
        }
    }

    private void unregisterConfigListener() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    private ConfigurationAdmin getConfigAdmin() {
        final ConfigurationAdmin service = configAdminTracker.getService();
        if (service != null) {
            return service;
        }
        return configAdmin;
    }

    private void processExistingConfigs(final ConfigurationAdmin configAdmin) {
        try {
            final Configuration[] configs = configAdmin.listConfigurations(null);
            if (configs != null) {
                for (final Configuration config : configs) {
                    executor.submit(() -> processConfiguration(config));
                }
            }
        } catch (final Exception e) {
            logger.atError().msg("Error processing existing configurations").throwable(e).log();
        }
    }

    private void processConfiguration(final Configuration config) {
        final String pid = config.getPid();
        try {
            final boolean hasMetatype = xMetaTypeAdmin == null ? false : xMetaTypeAdmin.isMetatype(config);
            if (!hasMetatype) {
                configurations.put(pid, toConfigDTO(config));
            } else {
                configurations.remove(pid);
            }
        } catch (final Exception e) {
            logger.atError().msg("Error processing configuration for PID: " + pid).throwable(e).log();
        }
    }

    @Override
    public void configurationEvent(final ConfigurationEvent event) {
        final ConfigurationAdmin configAdmin = getConfigAdmin();
        if (configAdmin == null) {
            return;
        }
        final String pid = event.getPid();
        if (event.getType() == ConfigurationEvent.CM_DELETED) {
            configurations.remove(pid);
        } else if (event.getType() == ConfigurationEvent.CM_UPDATED) {
            executor.submit(() -> {
                try {
                    final Configuration config = configAdmin.getConfiguration(pid, "?");
                    processConfiguration(config);
                } catch (final Exception e) {
                    logger.atError().msg("Error processing configuration event for PID: " + pid).throwable(e).log();
                }
            });
        }
    }

    public List<XConfigurationDTO> getConfigurations() {
        if (getConfigAdmin() == null) {
            logger.atWarn().msg(serviceUnavailable(CM)).log();
            return Collections.emptyList();
        }
        return new ArrayList<>(configurations.values());
    }

    public XResultDTO createOrUpdateConfiguration(final String pid, final Map<String, Object> newProperties) {
        final ConfigurationAdmin configAdmin = getConfigAdmin();
        if (configAdmin == null) {
            logger.atWarn().msg(serviceUnavailable(CM)).log();
            return createResult(SKIPPED, serviceUnavailable(CM));
        }
        XResultDTO result = null;
        try {
            String              action        = null;
            final Configuration configuration = configAdmin.getConfiguration(pid, "?");
            if (configuration.getProperties() == null) { // new configuration
                action = "created";
            } else {
                action = "updated";
            }
            executeUpdate(configuration, newProperties);
            result = createResult(SUCCESS, "Configuration with PID '" + pid + "' has been " + action);
        } catch (final Exception e) {
            result = createResult(ERROR,
                    "Configuration with PID '" + pid + "' cannot be processed due to " + e.getMessage());
        }
        return result;
    }

    public XResultDTO deleteConfiguration(final String pid) {
        final ConfigurationAdmin configAdmin = getConfigAdmin();
        if (configAdmin == null) {
            logger.atWarn().msg(serviceUnavailable(CM)).log();
            return createResult(SKIPPED, serviceUnavailable(CM));
        }
        XResultDTO result = null;
        try {
            final Configuration[] configs = configAdmin.listConfigurations(null);
            if (configs == null) {
                return createResult(SUCCESS, "Configuration with PID '" + pid + "' cannot be found");
            }
            for (final Configuration configuration : configs) {
                if (configuration.getPid().equals(pid)) {
                    configuration.delete();
                    result = createResult(SUCCESS, "Configuration with PID '" + pid + "' has been deleted");
                }
            }
            if (result == null) {
                result = createResult(SUCCESS, "Configuration with PID '" + pid + "' cannot be found");
            }
        } catch (final Exception e) {
            result = createResult(ERROR,
                    "Configuration with PID '" + pid + "' cannot be deleted due to " + e.getMessage());
        }
        return result;
    }

    public XResultDTO createFactoryConfiguration(final String factoryPid, final Map<String, Object> newProperties) {
        final ConfigurationAdmin configAdmin = getConfigAdmin();
        if (configAdmin == null) {
            logger.atWarn().msg(serviceUnavailable(CM)).log();
            return createResult(SKIPPED, serviceUnavailable(CM));
        }
        XResultDTO result = null;
        try {
            final Configuration configuration = configAdmin.createFactoryConfiguration(factoryPid, "?");
            configuration.update(new Hashtable<>(newProperties));
            result = createResult(SUCCESS, "Configuration with factory PID '" + factoryPid + " ' has been created");
        } catch (final Exception e) {
            result = createResult(ERROR,
                    "Configuration with factory PID '" + factoryPid + "' cannot be created due to " + e.getMessage());
        }
        return result;
    }

    public static Map<String, ConfigValue> prepareConfiguration(final Configuration config) {
        if (config == null) {
            return Collections.emptyMap();
        }
        final Map<String, ConfigValue> props = new HashMap<>();
        for (final Entry<String, Object> entry : AgentHelper.valueOf(config.getProperties()).entrySet()) {
            final String      key         = entry.getKey();
            final Object      value       = entry.getValue();
            final ConfigValue configValue = ConfigValue.create(key, value, XAttributeDefType.getType(value));
            props.put(key, configValue);
        }
        return props;
    }

    public void setComponentReferenceFilters(final XConfigurationDTO configuration) {
        final List<XComponentReferenceFilterDTO> componentReferenceFilters = new ArrayList<>();
        final List<XComponentDTO>                components                = componentAdmin.getComponents();
        for (final XComponentDTO component : components) {
            if (!matchPID(component, configuration)) {
                continue;
            }
            final List<XComponentReferenceFilterDTO> satisfiedReferenceFilters   = findSatisfiedReferenceFilters(
                    component, configuration);
            final List<XComponentReferenceFilterDTO> unsatisfiedReferenceFilters = findUnsatisfiedReferenceFilters(
                    component, configuration);
            componentReferenceFilters.addAll(satisfiedReferenceFilters);
            componentReferenceFilters.addAll(unsatisfiedReferenceFilters);
        }
        configuration.componentReferenceFilters = componentReferenceFilters;
    }

    private boolean matchPID(final XComponentDTO component, final XConfigurationDTO configuration) {
        final boolean hasFactoryPID = component.configurationPid.contains(configuration.factoryPid);
        final boolean hasConfigPID  = component.configurationPid.contains(configuration.pid);

        return hasConfigPID || hasFactoryPID;
    }

    private List<XComponentReferenceFilterDTO> findUnsatisfiedReferenceFilters(final XComponentDTO component,
                                                                               final XConfigurationDTO configuration) {
        final List<XComponentReferenceFilterDTO> satisfiedReferenceFilters = new ArrayList<>();
        for (final XSatisfiedReferenceDTO satisfiedReference : component.satisfiedReferences) {
            satisfiedReferenceFilters.add(toComponentRefFilter(component.name, satisfiedReference.name, configuration));
        }
        return satisfiedReferenceFilters;
    }

    private List<XComponentReferenceFilterDTO> findSatisfiedReferenceFilters(final XComponentDTO component,
                                                                             final XConfigurationDTO configuration) {
        final List<XComponentReferenceFilterDTO> unsatisfiedReferenceFilters = new ArrayList<>();
        for (final XUnsatisfiedReferenceDTO unsatisfiedReference : component.unsatisfiedReferences) {
            unsatisfiedReferenceFilters
                    .add(toComponentRefFilter(component.name, unsatisfiedReference.name, configuration));
        }
        return unsatisfiedReferenceFilters;
    }

    private XComponentReferenceFilterDTO toComponentRefFilter(final String componentName,
                                                              final String refName,
                                                              final XConfigurationDTO configuration) {
        final XComponentReferenceFilterDTO dto = new XComponentReferenceFilterDTO();

        dto.componentName = componentName;
        dto.targetKey     = refName + ".target";

        // @formatter:off
        dto.targetFilter  = Optional.ofNullable(configuration.properties.remove(dto.targetKey))
                                    .map(k -> k.value)
                                    .map(String.class::cast)
                                    .orElse(null);
        // @formatter:on

        return dto;
    }

    private void executeUpdate(final Configuration configuration, final Map<String, Object> newProperties) {
        final Dictionary<String, Object> properties = new Hashtable<>(newProperties);
        try {
            Reflect.on(configuration).call("updateIfDifferent", properties).get();
        } catch (final Exception e) {
            Reflect.on(configuration).call("update", properties).get();
        }
    }

    private XConfigurationDTO toConfigDTO(final Configuration configuration) {
        final XConfigurationDTO dto = new XConfigurationDTO();

        dto.pid         = Optional.ofNullable(configuration).map(Configuration::getPid).orElse(null);
        dto.factoryPid  = Optional.ofNullable(configuration).map(Configuration::getFactoryPid).orElse(null);
        dto.properties  = prepareConfiguration(configuration);
        dto.location    = Optional.ofNullable(configuration).map(Configuration::getBundleLocation).orElse(null);
        dto.isPersisted = true;

        return dto;
    }

}
