package in.bytehue.osgifx.console.agent.provider;

import static java.util.Collections.emptyList;
import static org.osgi.framework.Constants.SYSTEM_BUNDLE_ID;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.startlevel.StartLevelRuntimeHandler;
import aQute.remote.agent.AgentServer;
import in.bytehue.osgifx.console.agent.ConsoleAgent;
import in.bytehue.osgifx.console.agent.dto.XBundleDTO;
import in.bytehue.osgifx.console.agent.dto.XComponentDTO;
import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XEventDTO;
import in.bytehue.osgifx.console.agent.dto.XPropertyDTO;
import in.bytehue.osgifx.console.agent.dto.XServiceDTO;

@Capability(namespace = SERVICE_NAMESPACE, attribute = "objectClass:List<String>=in.bytehue.osgifx.console.agent.ConsoleAgent")
public final class ConsoleAgentServer extends AgentServer implements ConsoleAgent {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> scrTracker;
    private final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>           configAdminTracker;

    public ConsoleAgentServer(final String name, final BundleContext context, final File cache) {
        this(name, context, cache, StartLevelRuntimeHandler.absent());
    }

    public ConsoleAgentServer(final String name, final BundleContext context, final File cache,
            final StartLevelRuntimeHandler startlevels) {
        super(name, context, cache, startlevels);

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
        return XComponentInfoProvider.get(getContext(), scrTracker.getService());
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
    public void enableComponent(final ComponentDescriptionDTO description) {
        // @formatter:off
        Optional.ofNullable(scrTracker.getService())
                .map(scr -> scr.enableComponent(description))
                .ifPresent(p -> {
                    try {
                        p.getValue();
                    } catch (final Exception e) {
                        logger.error("Cannot enable component '{}'", description);
                    }
                });
        // @formatter:on
    }

    @Override
    public void disableComponent(final ComponentDescriptionDTO description) {
        // @formatter:off
        Optional.ofNullable(scrTracker.getService())
                .map(scr -> scr.disableComponent(description))
                .ifPresent(p -> {
                    try {
                        p.getValue();
                    } catch (final Exception e) {
                        logger.error("Cannot disable component '{}'", description);
                    }
                });
        // @formatter:on
    }

    @Override
    public Collection<ServiceReferenceDTO> getServiceReferences(final String filter) throws Exception {
        return getFramework().services;
    }

    @Override
    public Collection<XConfigurationDTO> listConfigurations(final String filter) throws IOException, InvalidSyntaxException {
        final ConfigurationAdmin configAdmin = configAdminTracker.getService();
        if (configAdmin == null) {
            return Collections.emptyList();
        }
        final List<XConfigurationDTO> configurations = new ArrayList<>();
        for (final Configuration configuration : configAdmin.listConfigurations(filter)) {
            final XConfigurationDTO dto = toDTO(configuration);
            configurations.add(dto);
        }
        return configurations;
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
        runtime.put("OS Architechture", System.getProperty("os.arch"));
        runtime.put("Uptime", String.valueOf(getSystemUptime()));

        return runtime;
    }

    private static long getSystemUptime() {
        final RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    private XConfigurationDTO toDTO(final Configuration configuration) {
        final XConfigurationDTO dto = new XConfigurationDTO();

        dto.pid        = configuration.getPid();
        dto.factoryPid = configuration.getFactoryPid();
        dto.properties = ConsoleAgentHelper.toMap(configuration.getProperties());

        return dto;
    }

}
