/*******************************************************************************
 * Copyright 2022 Amit Kumar Mondal
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
package com.osgifx.console.agent.provider;

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.agent.provider.AgentServer.createResult;
import static java.util.Objects.requireNonNull;

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

import com.osgifx.console.agent.dto.ConfigValue;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XResultDTO;

public class XConfigurationAdmin {

    private final BundleContext      context;
    private final Object             metatype;
    private final ConfigurationAdmin configAdmin;

    public XConfigurationAdmin(final BundleContext context, final Object configAdmin, final Object metatype) {
        this.context     = requireNonNull(context);
        this.metatype    = metatype;
        this.configAdmin = (ConfigurationAdmin) configAdmin;
    }

    public List<XConfigurationDTO> getConfigurations() {
        if (configAdmin == null) {
            return Collections.emptyList();
        }
        List<XConfigurationDTO> configsWithoutMetatype = null;
        try {
            configsWithoutMetatype = findConfigsWithoutMetatype();
        } catch (IOException | InvalidSyntaxException e) {
            return Collections.emptyList();
        }
        return configsWithoutMetatype;
    }

    public XResultDTO createOrUpdateConfiguration(final String pid, final Map<String, Object> newProperties) {
        if (configAdmin == null) {
            return createResult(XResultDTO.SKIPPED, "Required services are unavailable to process the request");
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
            configuration.update(new Hashtable<>(newProperties));
            result = createResult(SUCCESS, "Configuration with PID '" + pid + "' has been " + action);
        } catch (final Exception e) {
            result = createResult(ERROR, "Configuration with PID '" + pid + "' cannot be processed due to " + e.getMessage());
        }
        return result;
    }

    public XResultDTO deleteConfiguration(final String pid) {
        if (configAdmin == null) {
            return createResult(XResultDTO.SKIPPED, "Required services are unavailable to process the request");
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
            result = createResult(ERROR, "Configuration with PID '" + pid + "' cannot be deleted due to " + e.getMessage());
        }
        return result;
    }

    public XResultDTO createFactoryConfiguration(final String factoryPid, final Map<String, Object> newProperties) {
        if (configAdmin == null) {
            return createResult(XResultDTO.SKIPPED, "Required services are unavailable to process the request");
        }
        XResultDTO result = null;
        try {
            final Configuration configuration = configAdmin.createFactoryConfiguration(factoryPid, "?");
            configuration.update(new Hashtable<>(newProperties));
            result = createResult(SUCCESS, "Configuration with factory PID '" + factoryPid + " ' has been created");
        } catch (final Exception e) {
            result = createResult(ERROR, "Configuration with factory PID '" + factoryPid + "' cannot be created due to " + e.getMessage());
        }
        return result;
    }

    private List<XConfigurationDTO> findConfigsWithoutMetatype() throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos    = new ArrayList<>();
        final Configuration[]         configs = configAdmin.listConfigurations(null);
        if (configs == null) {
            return dtos;
        }
        for (final Configuration config : configs) {
            final boolean hasMetatype = metatype == null ? false : XMetaTypeAdmin.hasMetatype(context, metatype, config);
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
        dto.properties  = prepareConfiguration(configuration.getProperties());
        dto.location    = Optional.ofNullable(configuration).map(Configuration::getBundleLocation).orElse(null);
        dto.isPersisted = true;

        return dto;
    }

    public static Map<String, ConfigValue> prepareConfiguration(final Dictionary<String, Object> properties) {
        final Map<String, ConfigValue> props = new HashMap<>();
        for (final Entry<String, Object> entry : AgentServer.valueOf(properties).entrySet()) {
            final String      key         = entry.getKey();
            final Object      value       = entry.getValue();
            final ConfigValue configValue = ConfigValue.create(key, value, XAttributeDefType.getType(value));
            props.put(key, configValue);
        }
        return props;
    }

}
