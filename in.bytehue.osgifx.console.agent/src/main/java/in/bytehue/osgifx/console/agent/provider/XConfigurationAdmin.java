package in.bytehue.osgifx.console.agent.provider;

import static in.bytehue.osgifx.console.agent.dto.XResultDTO.ERROR;
import static in.bytehue.osgifx.console.agent.dto.XResultDTO.SUCCESS;
import static in.bytehue.osgifx.console.agent.provider.AgentServer.createResult;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;
import in.bytehue.osgifx.console.agent.dto.XObjectClassDefDTO;
import in.bytehue.osgifx.console.agent.dto.XResultDTO;

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
            final Configuration configuration = configAdmin.getFactoryConfiguration(factoryPid, "?");
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
                dtos.add(toConfigDTO(config, null));
            }
        }
        return dtos;
    }

    private XConfigurationDTO toConfigDTO(final Configuration configuration, final XObjectClassDefDTO ocd) {
        final XConfigurationDTO dto = new XConfigurationDTO();

        dto.ocd        = ocd;
        dto.pid        = Optional.ofNullable(configuration).map(Configuration::getPid).orElse(null);
        dto.factoryPid = Optional.ofNullable(configuration).map(Configuration::getFactoryPid).orElse(null);
        dto.properties = Optional.ofNullable(configuration).map(c -> AgentServer.valueOf(configuration.getProperties())).orElse(null);
        dto.location   = Optional.ofNullable(configuration).map(Configuration::getBundleLocation).orElse(null);

        return dto;
    }

}
