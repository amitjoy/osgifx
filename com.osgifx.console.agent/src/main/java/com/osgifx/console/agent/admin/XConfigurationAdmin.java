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

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.agent.helper.AgentHelper.createResult;
import static com.osgifx.console.agent.helper.AgentHelper.serviceUnavailable;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.CM;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XComponentReferenceFilterDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XReferenceDTO;
import com.osgifx.console.agent.dto.XResultDTO;
import com.osgifx.console.agent.helper.AgentHelper;
import com.osgifx.console.agent.provider.PackageWirings;
import com.osgifx.console.agent.rpc.codec.BinaryCodec;
import com.osgifx.console.agent.rpc.codec.SnapshotDecoder;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class XConfigurationAdmin extends AbstractSnapshotAdmin<XConfigurationDTO>
        implements ConfigurationListener, BundleListener {

    private final BundleContext                                              context;
    private final XComponentAdmin                                            componentAdmin;
    private final XMetaTypeAdmin                                             xMetaTypeAdmin;
    private final PackageWirings                                             packageWirings;
    private ServiceRegistration<ConfigurationListener>                       registration;
    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>           configAdminTracker;
    private ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrTracker;

    // --- Optimized Reflection Handles (Init Once, Use Forever) ---
    private static final MethodHandle UPDATE_IF_DIFFERENT;

    static {
        final MethodHandles.Lookup lookup            = MethodHandles.publicLookup();
        MethodHandle               updateIfDifferent = null;
        try {
            updateIfDifferent = lookup.findVirtual(Configuration.class, "updateIfDifferent",
                    MethodType.methodType(boolean.class, Dictionary.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // Method not present (Older OSGi R6 environment), handle remains null
        }
        UPDATE_IF_DIFFERENT = updateIfDifferent;
    }

    @Inject
    public XConfigurationAdmin(final BundleContext context,
                               final XComponentAdmin componentAdmin,
                               final XMetaTypeAdmin xMetaTypeAdmin,
                               final PackageWirings packageWirings,
                               final BinaryCodec codec,
                               final SnapshotDecoder decoder,
                               final ScheduledExecutorService executor) {
        super(codec, decoder, executor);
        this.context        = context;
        this.componentAdmin = componentAdmin;
        this.xMetaTypeAdmin = xMetaTypeAdmin;
        this.packageWirings = packageWirings;
    }

    public void init() {
        configAdminTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(context,
                                                                                        ConfigurationAdmin.class,
                                                                                        null) {
            @Override
            public ConfigurationAdmin addingService(final ServiceReference<ConfigurationAdmin> reference) {
                final ConfigurationAdmin service = context.getService(reference);
                registerConfigListener();
                scheduleUpdate(pendingChangeCount.incrementAndGet());
                return service;
            }

            @Override
            public void removedService(final ServiceReference<ConfigurationAdmin> reference,
                                       final ConfigurationAdmin service) {
                unregisterConfigListener();
                snapshot.set(null);
                super.removedService(reference, service);
            }
        };
        configAdminTracker.open();

        if (packageWirings.isScrWired()) {
            scrTracker = new ServiceTracker<>(context, ServiceComponentRuntime.class,
                                              new ServiceTrackerCustomizer<ServiceComponentRuntime, ServiceComponentRuntime>() {
                                                  @Override
                                                  public ServiceComponentRuntime addingService(final ServiceReference<ServiceComponentRuntime> reference) {
                                                      final ServiceComponentRuntime service = context
                                                              .getService(reference);
                                                      scheduleUpdate(getChangeCount(reference));
                                                      return service;
                                                  }

                                                  @Override
                                                  public void modifiedService(final ServiceReference<ServiceComponentRuntime> reference,
                                                                              final ServiceComponentRuntime service) {
                                                      scheduleUpdate(getChangeCount(reference));
                                                  }

                                                  @Override
                                                  public void removedService(final ServiceReference<ServiceComponentRuntime> reference,
                                                                             final ServiceComponentRuntime service) {
                                                      context.ungetService(reference);
                                                  }
                                              });
            scrTracker.open();
        }

        context.addBundleListener(this);
    }

    @Override
    public void stop() {
        super.stop();
        if (configAdminTracker != null) {
            configAdminTracker.close();
        }
        if (scrTracker != null) {
            scrTracker.close();
        }
        context.removeBundleListener(this);
    }

    @Override
    protected List<XConfigurationDTO> map() throws Exception {
        final ConfigurationAdmin configAdmin = getConfigAdmin();
        if (configAdmin == null) {
            return null;
        }
        final List<XConfigurationDTO> dtos = new ArrayList<>();

        // 1. Add metatyped configurations (including metatypes without configurations)
        if (xMetaTypeAdmin != null) {
            dtos.addAll(xMetaTypeAdmin.getConfigurations());
        }

        // 2. Add configurations that don't have metatyping
        final Configuration[] configs = configAdmin.listConfigurations(null);
        if (configs != null) {
            for (final Configuration config : configs) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                final boolean hasMetatype = xMetaTypeAdmin == null ? false : xMetaTypeAdmin.isMetatype(config);
                if (!hasMetatype) {
                    dtos.add(toConfigDTO(config));
                }
            }
        }

        // 3. Add Component Reference Filters
        if (packageWirings.isScrWired() && componentAdmin != null) {
            try {
                final List<XComponentDTO> components = componentAdmin.map();
                if (components != null && !components.isEmpty()) {
                    dtos.forEach(c -> setComponentReferenceFilters(c, components));
                }
            } catch (final Exception e) {
                logger.atWarn().msg("Failed to retrieve components for reference filters").throwable(e).log();
            }
        }
        return dtos;
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
        return configAdminTracker == null ? null : configAdminTracker.getService();
    }

    @Override
    public void configurationEvent(final ConfigurationEvent event) {
        scheduleUpdate(pendingChangeCount.incrementAndGet());
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        scheduleUpdate(pendingChangeCount.incrementAndGet());
    }

    private long getChangeCount(final ServiceReference<?> ref) {
        final Object prop = ref.getProperty("service.changecount");
        return prop instanceof Long ? (Long) prop : 0L;
    }

    @Override
    public List<XConfigurationDTO> get() {
        final byte[] current = snapshot();
        if (current == null || current.length == 0) {
            return new ArrayList<>();
        }
        try {
            return decoder.decodeList(current, XConfigurationDTO.class);
        } catch (final Exception e) {
            logger.atError().msg("Failed to decode configuration snapshot").throwable(e).log();
            return new ArrayList<>();
        }
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
        try {
            final Configuration configuration = configAdmin.getConfiguration(pid, "?");
            if (configuration.getProperties() == null) {
                return createResult(SUCCESS, "Configuration with PID '" + pid + "' cannot be found");
            }
            configuration.delete();
            return createResult(SUCCESS, "Configuration with PID '" + pid + "' has been deleted");
        } catch (final Exception e) {
            return createResult(ERROR,
                    "Configuration with PID '" + pid + "' cannot be deleted due to " + e.getMessage());
        }
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
            return new HashMap<>();
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

    public void setComponentReferenceFilters(final XConfigurationDTO configuration,
                                             final List<XComponentDTO> components) {
        final List<XComponentReferenceFilterDTO> componentReferenceFilters = new ArrayList<>();
        for (final XComponentDTO component : components) {
            if (!matchPID(component, configuration)) {
                continue;
            }
            final List<XComponentReferenceFilterDTO> referenceFilters = findAllReferenceFilters(component,
                    configuration);
            componentReferenceFilters.addAll(referenceFilters);
        }
        configuration.componentReferenceFilters = componentReferenceFilters;
    }

    private boolean matchPID(final XComponentDTO component, final XConfigurationDTO configuration) {
        // Strategy 1: Component explicitly declares this PID in its configurationPid array
        final boolean hasFactoryPID = component.configurationPid.contains(configuration.factoryPid);
        final boolean hasConfigPID  = component.configurationPid.contains(configuration.pid);

        // Strategy 2: DS default - component name is used as PID when configurationPid is not set
        final boolean nameMatchesPID        = component.name.equals(configuration.pid);
        final boolean nameMatchesFactoryPID = component.name.equals(configuration.factoryPid);

        // Strategy 3: Configuration PID matches component's implementation class
        // This handles cases where the configuration is named after the implementation class
        final boolean implClassMatchesPID        = component.implementationClass != null
                && component.implementationClass.equals(configuration.pid);
        final boolean implClassMatchesFactoryPID = component.implementationClass != null
                && component.implementationClass.equals(configuration.factoryPid);

        final boolean matches = hasConfigPID || hasFactoryPID || nameMatchesPID || nameMatchesFactoryPID
                || implClassMatchesPID || implClassMatchesFactoryPID;

        return matches;
    }

    private List<XComponentReferenceFilterDTO> findAllReferenceFilters(final XComponentDTO component,
                                                                       final XConfigurationDTO configuration) {
        final List<XComponentReferenceFilterDTO> referenceFilters = new ArrayList<>();
        for (final XReferenceDTO reference : component.references) {
            referenceFilters.add(toComponentRefFilter(component.name, reference.name, configuration));
        }
        return referenceFilters;
    }

    /**
     * Converts a component reference to a DTO and extracts the target filter.
     * <p>
     * <b>Note:</b> This method mutates the {@code configuration.properties} map by removing
     * the target filter key to avoid duplication in the UI.
     */
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

    private void executeUpdate(final Configuration configuration,
                               final Map<String, Object> newProperties) throws Exception {
        final Dictionary<String, Object> properties = new Hashtable<>(newProperties);
        if (UPDATE_IF_DIFFERENT != null) {
            try {
                UPDATE_IF_DIFFERENT.invoke(configuration, properties);
            } catch (final Throwable e) {
                if (e instanceof Exception) {
                    throw (Exception) e;
                }
                throw new RuntimeException(e);
            }
        } else {
            configuration.update(properties);
        }
    }

    private XConfigurationDTO toConfigDTO(final Configuration configuration) {
        final XConfigurationDTO dto = new XConfigurationDTO();

        dto.pid         = configuration.getPid();
        dto.factoryPid  = configuration.getFactoryPid();
        dto.properties  = prepareConfiguration(configuration);
        dto.location    = configuration.getBundleLocation();
        dto.isPersisted = true;

        return dto;
    }

}
