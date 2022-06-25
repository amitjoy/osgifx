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
package com.osgifx.console.agent.admin;

import static com.osgifx.console.agent.dto.XResultDTO.ERROR;
import static com.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static com.osgifx.console.agent.provider.AgentServer.createResult;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.osgi.service.metatype.ObjectClassDefinition.ALL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.osgifx.console.agent.dto.XAttributeDefDTO;
import com.osgifx.console.agent.dto.XAttributeDefType;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XObjectClassDefDTO;
import com.osgifx.console.agent.dto.XResultDTO;

public class XMetaTypeAdmin {

    private final BundleContext      context;
    private final MetaTypeService    metatype;
    private final ConfigurationAdmin configAdmin;

    public XMetaTypeAdmin(final BundleContext context, final Object configAdmin, final Object metatype) {
        this.context     = requireNonNull(context);
        this.configAdmin = (ConfigurationAdmin) configAdmin;
        this.metatype    = (MetaTypeService) metatype;
    }

    public List<XConfigurationDTO> getConfigurations() {
        if (configAdmin == null || metatype == null) {
            return Collections.emptyList();
        }
        List<XConfigurationDTO> configsWithMetatype    = null;
        List<XConfigurationDTO> metatypeWithoutConfigs = null;
        try {
            configsWithMetatype    = findConfigsWithMetatype();
            metatypeWithoutConfigs = findMetatypeWithoutConfigs();
        } catch (final Exception e) {
            return Collections.emptyList();
        }
        return joinLists(configsWithMetatype, metatypeWithoutConfigs);
    }

    public XResultDTO createOrUpdateConfiguration(final String pid, final Map<String, Object> newProperties) {
        if (configAdmin == null || metatype == null) {
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
        if (configAdmin == null || metatype == null) {
            return createResult(XResultDTO.SKIPPED, "Required services are unavailable to process the request");
        }
        XResultDTO result = null;
        try {
            for (final Configuration configuration : configAdmin.listConfigurations(null)) {
                if (configuration.getPid().equals(pid)) {
                    configuration.delete();
                    result = createResult(SUCCESS, "Configuration with PID '" + pid + "' has been deleted");
                }
            }
        } catch (final Exception e) {
            result = createResult(ERROR, "Configuration with PID '" + pid + "' cannot be deleted due to " + e.getMessage());
        }
        return result;
    }

    public XResultDTO createFactoryConfiguration(final String factoryPid, final Map<String, Object> newProperties) {
        if (configAdmin == null || metatype == null) {
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

    private List<XConfigurationDTO> findConfigsWithMetatype() throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Configuration config : configAdmin.listConfigurations(null)) {
            final boolean hasMetatype = hasMetatype(context, metatype, config);
            if (hasMetatype) {
                dtos.add(toConfigDTO(config, null, toOCD(config)));
            }
        }
        return dtos;
    }

    private List<XConfigurationDTO> findMetatypeWithoutConfigs() throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Bundle bundle : context.getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            if (metatypeInfo == null) {
                continue;
            }
            for (final String pid : metatypeInfo.getPids()) {
                final boolean hasAssociatedConfiguration = checkConfigurationExistence(pid);
                if (!hasAssociatedConfiguration) {
                    final XObjectClassDefDTO ocd = toOcdDTO(pid, metatypeInfo, ConfigurationType.SINGLETON);
                    dtos.add(toConfigDTO(null, pid, ocd));
                }
            }
            for (final String fpid : metatypeInfo.getFactoryPids()) {
                final boolean hasAssociatedConfiguration = checkFactoryConfigurationExistence(fpid);
                if (!hasAssociatedConfiguration) {
                    final XObjectClassDefDTO ocd       = toOcdDTO(fpid, metatypeInfo, ConfigurationType.FACTORY);
                    final XConfigurationDTO  configDTO = toConfigDTO(null, fpid, ocd);
                    configDTO.isFactory = true;
                    dtos.add(configDTO);
                }
            }
        }
        return dtos;
    }

    private boolean checkConfigurationExistence(final String pid) throws IOException, InvalidSyntaxException {
        return configAdmin.listConfigurations("(service.pid=" + pid + ")") != null;
    }

    private boolean checkFactoryConfigurationExistence(final String factoryPid) throws IOException, InvalidSyntaxException {
        return configAdmin.listConfigurations("(service.factoryPid=" + factoryPid + ")") != null;
    }

    private XConfigurationDTO toConfigDTO(final Configuration configuration, final String metatypePID, final XObjectClassDefDTO ocd) {
        final XConfigurationDTO dto = new XConfigurationDTO();

        dto.ocd         = ocd;
        dto.isPersisted = configuration != null;
        dto.pid         = Optional.ofNullable(configuration).map(Configuration::getPid).orElse(metatypePID);
        dto.factoryPid  = Optional.ofNullable(configuration).map(Configuration::getFactoryPid).orElse(null);
        dto.properties  = XConfigurationAdmin.prepareConfiguration(configuration);
        dto.location    = Optional.ofNullable(configuration).map(Configuration::getBundleLocation).orElse(null);

        return dto;
    }

    private XObjectClassDefDTO toOCD(final Configuration config) {
        for (final Bundle bundle : context.getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            if (metatypeInfo == null) {
                continue;
            }
            final String configPID        = config.getPid();
            final String configFactoryPID = config.getFactoryPid();

            for (final String pid : metatypeInfo.getPids()) {
                if (pid.equals(configPID)) {
                    return toOcdDTO(configPID, metatypeInfo, ConfigurationType.SINGLETON);
                }
            }
            for (final String fPid : metatypeInfo.getFactoryPids()) {
                if (fPid.equals(config.getFactoryPid())) {
                    return toOcdDTO(configFactoryPID, metatypeInfo, ConfigurationType.FACTORY);
                }
            }
        }
        return null;
    }

    private XObjectClassDefDTO toOcdDTO(final String ocdId, final MetaTypeInformation metatypeInfo, final ConfigurationType type) {
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
        dto.type         = defType(ad.getType(), ad.getCardinality()).ordinal();
        dto.optionValues = Optional.ofNullable(ad.getOptionLabels()).map(Arrays::asList).orElse(null);
        dto.defaultValue = Optional.ofNullable(ad.getDefaultValue()).map(Arrays::asList).orElse(null);

        return dto;
    }

    public static boolean hasMetatype(final BundleContext context, final Object metatypeService, final Configuration config) {
        final MetaTypeService metatype = (MetaTypeService) metatypeService;
        for (final Bundle bundle : context.getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            if (metatypeInfo == null) {
                continue;
            }
            final String   pid                 = config.getPid();
            final String   factoryPID          = config.getFactoryPid();
            final String[] metatypePIDs        = metatypeInfo.getPids();
            final String[] metatypeFactoryPIDs = metatypeInfo.getFactoryPids();
            final boolean  pidExists           = Arrays.asList(metatypePIDs).contains(pid);
            final boolean  factoryPidExists    = Optional.ofNullable(factoryPID)
                    .map(fPID -> Arrays.asList(metatypeFactoryPIDs).contains(factoryPID)).orElse(false);
            if (pidExists || factoryPidExists) {
                return true;
            }
        }
        return false;
    }

    @SafeVarargs
    private static <T> List<T> joinLists(final List<T>... lists) {
        return Stream.of(lists).flatMap(Collection::stream).collect(Collectors.toList());
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
        SINGLETON, FACTORY
    }

}
