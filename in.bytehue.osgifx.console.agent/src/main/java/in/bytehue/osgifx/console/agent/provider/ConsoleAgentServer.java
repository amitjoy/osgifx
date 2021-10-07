package in.bytehue.osgifx.console.agent.provider;

import static in.bytehue.osgifx.console.agent.provider.ConsoleAgentHelper.valueOf;
import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;
import static org.osgi.service.metatype.ObjectClassDefinition.ALL;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.startlevel.StartLevelRuntimeHandler;
import aQute.remote.agent.AgentServer;
import in.bytehue.osgifx.console.agent.ConsoleAgent;
import in.bytehue.osgifx.console.agent.dto.XAttributeDefDTO;
import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import in.bytehue.osgifx.console.agent.dto.XFrameworkEventsDTO;
import in.bytehue.osgifx.console.agent.dto.XObjectClassDefDTO;
import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceDTO;

public final class ConsoleAgentServer extends AgentServer implements ConsoleAgent {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceTracker<MetaTypeService, MetaTypeService>                 metatypeTracker;
    private final ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrTracker;
    private final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>           configAdminTracker;

    private final AtomicInteger infoFrameworkEvents    = new AtomicInteger();
    private final AtomicInteger warningFrameworkEvents = new AtomicInteger();
    private final AtomicInteger errorFrameworkEvents   = new AtomicInteger();

    public ConsoleAgentServer(final String name, final BundleContext context, final File cache) {
        this(name, context, cache, StartLevelRuntimeHandler.absent());
    }

    public ConsoleAgentServer(final String name, final BundleContext context, final File cache,
            final StartLevelRuntimeHandler startlevels) {
        super(name, context, cache, startlevels);

        metatypeTracker    = new ServiceTracker<>(context, MetaTypeService.class, null);
        scrTracker         = new ServiceTracker<>(context, ServiceComponentRuntime.class, null);
        configAdminTracker = new ServiceTracker<>(context, ConfigurationAdmin.class, null);

        scrTracker.open();
        configAdminTracker.open();
    }

    @Override
    public List<XBundleDTO> getAllBundles() {
        return XBundleInfoProvider.get(getContext());
    }

    @Override
    public List<XComponentDTO> getAllComponents() {
        return XComponentInfoProvider.get(scrTracker.getService());
    }

    @Override
    public List<XConfigurationDTO> getAllConfigurations() {
        return XConfigurationtInfoProvider.get(configAdminTracker.getService());
    }

    @Override
    public List<XPropertyDTO> getAllProperties() {
        return XPropertytInfoProvider.get(getContext());
    }

    @Override
    public List<XEventDTO> getAllEvents() {
        return XEventInfoProvider.get(getContext());
    }

    @Override
    public List<XServiceDTO> getAllServices() {
        return XServiceInfoProvider.get(getContext());
    }

    @Override
    public Collection<ComponentDescriptionDTO> getComponentDescriptionDTOs() {
        // @formatter:off
        return Optional.ofNullable(scrTracker.getService())
                       .map(ServiceComponentRuntime::getComponentDescriptionDTOs)
                       .orElse(emptyList());
        // @formatter:on
    }

    @Override
    public Collection<ComponentConfigurationDTO> getComponentConfigurationDTOs(final ComponentDescriptionDTO description) {
        // @formatter:off
        return Optional.ofNullable(scrTracker.getService())
                       .map(scr -> scr.getComponentConfigurationDTOs(description))
                       .orElse(emptyList());
        // @formatter:on
    }

    @Override
    public String enableComponent(final String name) {
        final ServiceComponentRuntime service = scrTracker.getService();
        if (service == null) {
            return "Service Component Runtime service is unavailable";
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = service.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            if (dto.name.equals(name)) {
                try {
                    service.enableComponent(dto).onFailure(e -> builder.append(e.getMessage()).append(lineSeparator())).getValue();
                } catch (InvocationTargetException | InterruptedException e) {
                    builder.append(e.getMessage()).append(lineSeparator());
                }
                final String response = builder.toString();
                return response.isEmpty() ? null : response;
            }
        }
        return null;
    }

    @Override
    public String disableComponent(final long id) {
        final ServiceComponentRuntime service = scrTracker.getService();
        if (service == null) {
            return "Service Component Runtime service is unavailable";
        }
        final StringBuilder                       builder         = new StringBuilder();
        final Collection<ComponentDescriptionDTO> descriptionDTOs = service.getComponentDescriptionDTOs();

        for (final ComponentDescriptionDTO dto : descriptionDTOs) {
            final Collection<ComponentConfigurationDTO> configurationDTOs = service.getComponentConfigurationDTOs(dto);
            for (final ComponentConfigurationDTO configDTO : configurationDTOs) {
                if (configDTO.id == id) {
                    try {
                        service.disableComponent(dto).onFailure(e -> builder.append(e.getMessage()).append(lineSeparator())).getValue();
                    } catch (InvocationTargetException | InterruptedException e) {
                        builder.append(e.getMessage()).append(lineSeparator());
                    }
                    final String response = builder.toString();
                    return response.isEmpty() ? null : response;
                }
            }
        }
        return null;
    }

    @Override
    public Collection<ServiceReferenceDTO> getServiceReferences(final String filter) throws Exception {
        return getFramework().services;
    }

    @Override
    public Collection<XConfigurationDTO> listConfigurations(final String filter) throws IOException, InvalidSyntaxException {
        final List<XConfigurationDTO> configsWithoutMetatype = findConfigsWithoutMetatype();
        final List<XConfigurationDTO> configsWithMetatype    = findConfigsWithMetatype();
        final List<XConfigurationDTO> metatypeWithoutConfigs = findMetatypeWithoutConfigs();

        // @formatter:off
        return Stream.of(configsWithoutMetatype, configsWithMetatype, metatypeWithoutConfigs)
                     .flatMap(Collection::stream)
                     .collect(Collectors.toList());
        // @formatter:on
    }

    private List<XConfigurationDTO> findConfigsWithoutMetatype() throws IOException, InvalidSyntaxException {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return Collections.emptyList();
        }
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Configuration config : configAdmin.listConfigurations(null)) {
            final boolean hasMetatype = hasMetatype(config);
            if (!hasMetatype) {
                dtos.add(toConfigDTO(config, null));
            }
        }
        return dtos;
    }

    private List<XConfigurationDTO> findConfigsWithMetatype() throws IOException, InvalidSyntaxException {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return Collections.emptyList();
        }
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Configuration config : configAdmin.listConfigurations(null)) {
            final boolean hasMetatype = hasMetatype(config);
            if (hasMetatype) {
                dtos.add(toConfigDTO(config, toOCD(config)));
            }
        }
        return dtos;
    }

    private List<XConfigurationDTO> findMetatypeWithoutConfigs() throws IOException, InvalidSyntaxException {
        final MetaTypeService metatype = metatypeTracker.getService();
        if (metatype == null) {
            return null;
        }
        final List<XConfigurationDTO> dtos = new ArrayList<>();
        for (final Bundle bundle : getContext().getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
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

    private boolean checkConfigurationExistence(final String pid) throws IOException, InvalidSyntaxException {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return false;
        }
        return configAdmin.listConfigurations("(service.pid=pid)") != null;
    }

    private XConfigurationDTO toConfigDTO(final Configuration configuration, final XObjectClassDefDTO ocd) {
        final XConfigurationDTO dto = new XConfigurationDTO();

        dto.pid        = Optional.ofNullable(configuration).map(Configuration::getPid).orElse(null);
        dto.factoryPid = Optional.ofNullable(configuration).map(Configuration::getFactoryPid).orElse(null);
        dto.properties = Optional.ofNullable(configuration).map(c -> valueOf(configuration.getProperties())).orElse(null);
        dto.location   = Optional.ofNullable(configuration).map(Configuration::getBundleLocation).orElse(null);
        dto.ocd        = ocd;

        return dto;
    }

    private XObjectClassDefDTO toOCD(final Configuration config) {
        final MetaTypeService metatype = metatypeTracker.getService();
        if (metatype == null) {
            return null;
        }
        for (final Bundle bundle : getContext().getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            for (final String pid : metatypeInfo.getPids()) {
                if (pid.equals(config.getPid())) {
                    return toOcdDTO(pid, metatypeInfo);
                }
            }
        }
        return null;
    }

    private XObjectClassDefDTO toOcdDTO(final String pid, final MetaTypeInformation metatypeInfo) {
        final ObjectClassDefinition ocd = metatypeInfo.getObjectClassDefinition(pid, null);
        final XObjectClassDefDTO    dto = new XObjectClassDefDTO();

        dto.id            = ocd.getID();
        dto.name          = ocd.getName();
        dto.description   = ocd.getDescription();
        dto.attributeDefs = Stream.of(ocd.getAttributeDefinitions(ALL)).map(this::toAdDTO).collect(toList());

        return null;
    }

    private XAttributeDefDTO toAdDTO(final AttributeDefinition ad) {
        final XAttributeDefDTO dto = new XAttributeDefDTO();

        dto.id           = ad.getID();
        dto.name         = ad.getName();
        dto.cardinality  = ad.getCardinality();
        dto.description  = ad.getDescription();
        dto.type         = toMetatypeClazz(ad.getType());
        dto.optionValues = Arrays.asList(ad.getOptionLabels());

        return dto;
    }

    private Class<?> toMetatypeClazz(final int type) {
        switch (type) {
            case AttributeDefinition.INTEGER:
                return Integer.class;
            case AttributeDefinition.FLOAT:
            case AttributeDefinition.DOUBLE:
                return Double.class;
            case AttributeDefinition.BOOLEAN:
                return Boolean.class;
            case AttributeDefinition.LONG:
                return Long.class;
            case AttributeDefinition.PASSWORD:
            case AttributeDefinition.STRING:
                return String.class;
            default: // TODO other types
                return String.class;
        }
    }

    private boolean hasMetatype(final Configuration config) {
        final MetaTypeService metatype = metatypeTracker.getService();
        if (metatype == null) {
            return false;
        }
        for (final Bundle bundle : getContext().getBundles()) {
            final MetaTypeInformation metatypeInfo = metatype.getMetaTypeInformation(bundle);
            for (final String pid : metatypeInfo.getPids()) {
                if (pid.equals(config.getPid())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void deleteConfiguration(final String pid) throws IOException {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return;
        }
        try {
            for (final Configuration configuration : configAdmin.listConfigurations(null)) {
                if (configuration.getPid().equals(pid)) {
                    configuration.delete();
                }
            }
        } catch (final Exception e) {
            logger.error("Cannot delete configuration '{}'", pid);
        }
    }

    @Override
    public void updateConfiguration(final String pid, final Map<String, Object> newProperties) throws IOException {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return;
        }
        try {
            for (final Configuration configuration : configAdmin.listConfigurations(null)) {
                if (configuration.getPid().equals(pid)) {
                    configuration.update(new Hashtable<>(newProperties));
                }
            }
        } catch (final Exception e) {
            logger.error("Cannot update configuration '{}'", pid);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        scrTracker.close();
        configAdminTracker.close();
    }

    @Override
    public Map<String, String> runtimeInfo() {
        final Map<String, String> runtime      = new HashMap<>();
        final Bundle              systemBundle = getContext().getBundle(SYSTEM_BUNDLE_ID);

        runtime.put("Framework", systemBundle.getSymbolicName());
        runtime.put("Framework Version", systemBundle.getVersion().toString());
        runtime.put("Memory Total", String.valueOf(Runtime.getRuntime().totalMemory()));
        runtime.put("Memory Free", String.valueOf(Runtime.getRuntime().freeMemory()));
        runtime.put("OS Name", System.getProperty("os.name"));
        runtime.put("OS Version", System.getProperty("os.version"));
        runtime.put("OS Architecture", System.getProperty("os.arch"));
        runtime.put("Uptime", String.valueOf(getSystemUptime()));

        return runtime;
    }

    @Override
    public void frameworkEvent(final FrameworkEvent event) {
        super.frameworkEvent(event);
        switch (event.getType()) {
            case FrameworkEvent.INFO:
                infoFrameworkEvents.incrementAndGet();
                break;
            case FrameworkEvent.WARNING:
                warningFrameworkEvents.incrementAndGet();
                break;
            case FrameworkEvent.ERROR:
                errorFrameworkEvents.incrementAndGet();
                break;
            default:
                break;
        }
    }

    private static long getSystemUptime() {
        final RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    @Override
    public XFrameworkEventsDTO getFrameworkEventsOverview() {
        final XFrameworkEventsDTO dto = new XFrameworkEventsDTO();

        dto.info    = infoFrameworkEvents.get();
        dto.warning = warningFrameworkEvents.get();
        dto.error   = errorFrameworkEvents.get();

        return dto;
    }

}
