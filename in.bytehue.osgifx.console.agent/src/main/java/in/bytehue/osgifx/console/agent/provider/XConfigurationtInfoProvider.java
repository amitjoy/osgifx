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

    private static BundleContext      context;
    private static MetaTypeService    metatype;
    private static ConfigurationAdmin configAdmin;

    private XConfigurationtInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XConfigurationDTO> get(final BundleContext ctx, final ConfigurationAdmin cfgAdmin, final MetaTypeService mt) {
        configAdmin = cfgAdmin;
        metatype    = mt;
        context     = ctx;

        List<XConfigurationDTO> configsWithoutMetatype = null;
        List<XConfigurationDTO> configsWithMetatype    = null;
        List<XConfigurationDTO> metatypeWithoutConfigs = null;

        try {
            configsWithoutMetatype = findConfigsWithoutMetatype();
            configsWithMetatype    = findConfigsWithMetatype();
            metatypeWithoutConfigs = findMetatypeWithoutConfigs();
        } catch (IOException | InvalidSyntaxException e) {
            return Collections.emptyList();
        }
        return joinLists(configsWithoutMetatype, configsWithMetatype, metatypeWithoutConfigs);
    }

    private static List<XConfigurationDTO> findConfigsWithoutMetatype() throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Configuration config : configAdmin.listConfigurations(null)) {
            final boolean hasMetatype = hasMetatype(config);
            if (!hasMetatype) {
                dtos.add(toConfigDTO(config, null));
            }
        }
        return dtos;
    }

    private static List<XConfigurationDTO> findConfigsWithMetatype() throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Configuration config : configAdmin.listConfigurations(null)) {
            final boolean hasMetatype = hasMetatype(config);
            if (hasMetatype) {
                dtos.add(toConfigDTO(config, toOCD(config)));
            }
        }
        return dtos;
    }

    private static List<XConfigurationDTO> findMetatypeWithoutConfigs() throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Bundle bundle : context.getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            if (metatypeInfo == null) {
                continue;
            }
            for (final String pid : metatypeInfo.getPids()) {
                final boolean hasAssociatedConfiguration = checkConfigurationExistence(pid);
                if (!hasAssociatedConfiguration) {
                    final XObjectClassDefDTO ocd = toOcdDTO(pid, metatypeInfo);
                    dtos.add(toConfigDTO(null, ocd));
                }
            }
        }
        return dtos;
    }

    private static boolean checkConfigurationExistence(final String pid) throws IOException, InvalidSyntaxException {
        return configAdmin.listConfigurations("(service.pid=" + pid + ")") != null;
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

    private static XObjectClassDefDTO toOCD(final Configuration config) {
        for (final Bundle bundle : context.getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            if (metatypeInfo == null) {
                continue;
            }
            for (final String pid : metatypeInfo.getPids()) {
                if (pid.equals(config.getPid())) {
                    return toOcdDTO(pid, metatypeInfo);
                }
            }
        }
        return null;
    }

    private static XObjectClassDefDTO toOcdDTO(final String pid, final MetaTypeInformation metatypeInfo) {
        final ObjectClassDefinition ocd = metatypeInfo.getObjectClassDefinition(pid, null);
        final XObjectClassDefDTO    dto = new XObjectClassDefDTO();

        dto.id            = ocd.getID();
        dto.name          = ocd.getName();
        dto.description   = ocd.getDescription();
        dto.attributeDefs = Stream.of(ocd.getAttributeDefinitions(ALL)).map(XConfigurationtInfoProvider::toAdDTO).collect(toList());

        return dto;
    }

    private static XAttributeDefDTO toAdDTO(final AttributeDefinition ad) {
        final XAttributeDefDTO dto = new XAttributeDefDTO();

        dto.id           = ad.getID();
        dto.name         = ad.getName();
        dto.cardinality  = ad.getCardinality();
        dto.description  = ad.getDescription();
        dto.type         = toMetatypeClazz(ad.getType());
        dto.optionValues = Optional.ofNullable(ad.getOptionLabels()).map(Arrays::asList).orElse(null);

        return dto;
    }

    private static String toMetatypeClazz(final int type) {
        switch (type) {
            case AttributeDefinition.INTEGER:
                return Integer.class.getName();
            case AttributeDefinition.FLOAT:
            case AttributeDefinition.DOUBLE:
                return Double.class.getName();
            case AttributeDefinition.BOOLEAN:
                return Boolean.class.getName();
            case AttributeDefinition.LONG:
                return Long.class.getName();
            case AttributeDefinition.PASSWORD:
            case AttributeDefinition.STRING:
                return String.class.getName();
            default: // TODO other types
                return String.class.getName();
        }
    }

    private static boolean hasMetatype(final Configuration config) {
        for (final Bundle bundle : context.getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            if (metatypeInfo == null) {
                continue;
            }
            for (final String pid : metatypeInfo.getPids()) {
                if (pid.equals(config.getPid())) {
                    return true;
                }
            }
        }
        return false;
    }

    @SafeVarargs
    private static <T> List<T> joinLists(final List<T>... lists) {
        return Stream.of(lists).flatMap(Collection::stream).collect(Collectors.toList());
    }

}
