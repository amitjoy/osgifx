package in.bytehue.osgifx.console.agent.provider;

import static in.bytehue.osgifx.console.agent.provider.ConsoleAgentHelper.valueOf;
import static java.util.stream.Collectors.toList;
import static org.osgi.service.metatype.ObjectClassDefinition.ALL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

import in.bytehue.osgifx.console.agent.dto.XAttributeDefDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XObjectClassDefDTO;

public final class XConfigurationtInfoProvider {

    private XConfigurationtInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XConfigurationDTO> get(final BundleContext context, final ConfigurationAdmin configAdmin,
            final MetaTypeService metatype) {
        List<XConfigurationDTO> configsWithoutMetatype = null;
        List<XConfigurationDTO> configsWithMetatype    = null;
        List<XConfigurationDTO> metatypeWithoutConfigs = null;
        List<XConfigurationDTO> metatypeFactories      = null;

        try {
            configsWithoutMetatype = findConfigsWithoutMetatype(context, configAdmin, metatype);
            configsWithMetatype    = findConfigsWithMetatype(context, configAdmin, metatype);
            metatypeWithoutConfigs = findMetatypeWithoutConfigs(context, configAdmin, metatype);
            metatypeFactories      = findMetatypeFactories(context, metatype);
        } catch (IOException | InvalidSyntaxException e) {
            return Collections.emptyList();
        }
        return joinLists(configsWithoutMetatype, configsWithMetatype, metatypeWithoutConfigs, metatypeFactories);
    }

    private static List<XConfigurationDTO> findConfigsWithoutMetatype(final BundleContext context, final ConfigurationAdmin configAdmin,
            final MetaTypeService metatype) throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Configuration config : configAdmin.listConfigurations(null)) {
            final boolean hasMetatype = hasMetatype(config, context, metatype);
            if (!hasMetatype) {
                dtos.add(toConfigDTO(config, null));
            }
        }
        return dtos;
    }

    private static List<XConfigurationDTO> findConfigsWithMetatype(final BundleContext context, final ConfigurationAdmin configAdmin,
            final MetaTypeService metatype) throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Configuration config : configAdmin.listConfigurations(null)) {
            final boolean hasMetatype = hasMetatype(config, context, metatype);
            if (hasMetatype) {
                dtos.add(toConfigDTO(config, toOCD(config, context, metatype)));
            }
        }
        return dtos;
    }

    private static List<XConfigurationDTO> findMetatypeWithoutConfigs(final BundleContext context, final ConfigurationAdmin configAdmin,
            final MetaTypeService metatype) throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Bundle bundle : context.getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            if (metatypeInfo == null) {
                continue;
            }
            for (final String pid : metatypeInfo.getPids()) {
                final boolean hasAssociatedConfiguration = checkConfigurationExistence(pid, configAdmin);
                if (!hasAssociatedConfiguration) {
                    final XObjectClassDefDTO ocd = toOcdDTO(pid, metatypeInfo, ConfigurationType.SIMPLE);
                    dtos.add(toConfigDTO(null, ocd));
                }
            }
            for (final String fpid : metatypeInfo.getFactoryPids()) {
                final boolean hasAssociatedConfiguration = checkFactoryConfigurationExistence(fpid, configAdmin);
                if (!hasAssociatedConfiguration) {
                    final XObjectClassDefDTO ocd = toOcdDTO(fpid, metatypeInfo, ConfigurationType.FACTORY);
                    dtos.add(toConfigDTO(null, ocd));
                }
            }
        }
        return dtos;
    }

    private static List<XConfigurationDTO> findMetatypeFactories(final BundleContext context, final MetaTypeService metatype) {
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Bundle bundle : context.getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            if (metatypeInfo == null) {
                continue;
            }
            for (final String fpid : metatypeInfo.getFactoryPids()) {
                final XObjectClassDefDTO ocd       = toOcdDTO(fpid, metatypeInfo, ConfigurationType.FACTORY);
                final XConfigurationDTO  configDTO = toConfigDTO(null, ocd);
                configDTO.isFactory = true;
                dtos.add(configDTO);
            }
        }
        return dtos;
    }

    private static boolean checkConfigurationExistence(final String pid, final ConfigurationAdmin configAdmin)
            throws IOException, InvalidSyntaxException {
        return configAdmin.listConfigurations("(service.pid=" + pid + ")") != null;
    }

    private static boolean checkFactoryConfigurationExistence(final String factoryPid, final ConfigurationAdmin configAdmin)
            throws IOException, InvalidSyntaxException {
        return configAdmin.listConfigurations("(service.factoryPid=" + factoryPid + ")") != null;
    }

    private static XConfigurationDTO toConfigDTO(final Configuration configuration, final XObjectClassDefDTO ocd) {
        final XConfigurationDTO dto = new XConfigurationDTO();

        dto.ocd        = ocd;
        dto.pid        = Optional.ofNullable(configuration).map(Configuration::getPid).orElse(null);
        dto.factoryPid = Optional.ofNullable(configuration).map(Configuration::getFactoryPid).orElse(null);
        dto.properties = Optional.ofNullable(configuration).map(c -> valueOf(configuration.getProperties())).orElse(null);
        dto.location   = Optional.ofNullable(configuration).map(Configuration::getBundleLocation).orElse(null);

        return dto;
    }

    private static XObjectClassDefDTO toOCD(final Configuration config, final BundleContext context, final MetaTypeService metatype) {
        for (final Bundle bundle : context.getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            if (metatypeInfo == null) {
                continue;
            }
            final String configPID        = config.getPid();
            final String configFactoryPID = config.getFactoryPid();

            for (final String pid : metatypeInfo.getPids()) {
                if (pid.equals(configPID)) {
                    return toOcdDTO(configPID, metatypeInfo, ConfigurationType.SIMPLE);
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

    private static XObjectClassDefDTO toOcdDTO(final String ocdId, final MetaTypeInformation metatypeInfo, final ConfigurationType type) {
        final ObjectClassDefinition ocd = metatypeInfo.getObjectClassDefinition(ocdId, null);
        final XObjectClassDefDTO    dto = new XObjectClassDefDTO();

        dto.id                 = ocd.getID();
        dto.pid                = type == ConfigurationType.SIMPLE ? ocdId : null;
        dto.factoryPid         = type == ConfigurationType.FACTORY ? ocdId : null;
        dto.name               = ocd.getName();
        dto.description        = ocd.getDescription();
        dto.descriptorLocation = metatypeInfo.getBundle().getSymbolicName();
        dto.attributeDefs      = Stream.of(ocd.getAttributeDefinitions(ALL)).map(XConfigurationtInfoProvider::toAdDTO).collect(toList());

        return dto;
    }

    private static XAttributeDefDTO toAdDTO(final AttributeDefinition ad) {
        final XAttributeDefDTO dto = new XAttributeDefDTO();

        dto.id           = ad.getID();
        dto.name         = ad.getName();
        dto.cardinality  = ad.getCardinality();
        dto.description  = ad.getDescription();
        dto.type         = ad.getType();
        dto.optionValues = Optional.ofNullable(ad.getOptionLabels()).map(Arrays::asList).orElse(null);
        dto.defaultValue = Optional.ofNullable(ad.getDefaultValue()).map(Arrays::asList).orElse(null);

        return dto;
    }

    private static boolean hasMetatype(final Configuration config, final BundleContext context, final MetaTypeService metatype) {
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

    private enum ConfigurationType {
        SIMPLE,
        FACTORY
    }

}
