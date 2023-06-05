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

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SKIPPED;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.agent.helper.AgentHelper.createResult;
import static com.osgifx.console.agent.helper.AgentHelper.serviceUnavailable;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.CM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

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

public final class XConfigurationAdmin {

    private final BundleContext      context;
    private final Object             metatype;
    private final ConfigurationAdmin configAdmin;
    private final XComponentAdmin    componentAdmin;
    private final FluentLogger       logger = LoggerFactory.getFluentLogger(getClass());

    @Inject
    public XConfigurationAdmin(final BundleContext context,
                               final Object configAdmin,
                               final Object metatype,
                               final XComponentAdmin componentAdmin) {
        this.context        = context;
        this.metatype       = metatype;
        this.configAdmin    = (ConfigurationAdmin) configAdmin;
        this.componentAdmin = componentAdmin;
    }

    public List<XConfigurationDTO> getConfigurations() {
        if (configAdmin == null) {
            logger.atInfo().msg(serviceUnavailable(CM)).log();
            return Collections.emptyList();
        }
        List<XConfigurationDTO> configsWithoutMetatype = null;
        try {
            configsWithoutMetatype = findConfigsWithoutMetatype();
        } catch (final Exception e) {
            logger.atError().msg("Error occurred while retrieving configurations").throwable(e).log();
            return Collections.emptyList();
        }
        return configsWithoutMetatype;
    }

    public XResultDTO createOrUpdateConfiguration(final String pid, final Map<String, Object> newProperties) {
        if (configAdmin == null) {
            logger.atInfo().msg(serviceUnavailable(CM)).log();
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
        if (configAdmin == null) {
            logger.atInfo().msg(serviceUnavailable(CM)).log();
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
        if (configAdmin == null) {
            logger.atInfo().msg(serviceUnavailable(CM)).log();
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

    private List<XConfigurationDTO> findConfigsWithoutMetatype() throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos    = new ArrayList<>();
        final Configuration[]         configs = configAdmin.listConfigurations(null);
        if (configs == null) {
            return dtos;
        }
        for (final Configuration config : configs) {
            final boolean hasMetatype = metatype == null ? false
                    : XMetaTypeAdmin.hasMetatype(context, metatype, config);
            if (!hasMetatype) {
                dtos.add(toConfigDTO(config));
            }
        }
        return dtos;
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
