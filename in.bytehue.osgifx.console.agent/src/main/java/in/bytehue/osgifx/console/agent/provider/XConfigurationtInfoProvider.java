package in.bytehue.osgifx.console.agent.provider;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import in.bytehue.osgifx.console.agent.dto.XConfigurationDTO;

public final class XConfigurationtInfoProvider {

    private XConfigurationtInfoProvider() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static List<XConfigurationDTO> get(final ConfigurationAdmin configAdmin) {
        try {
            return Stream.of(configAdmin.listConfigurations(null)).map(XConfigurationtInfoProvider::toDTO).collect(Collectors.toList());
        } catch (IOException | InvalidSyntaxException e) {
            return Collections.emptyList();
        }
    }

    private static XConfigurationDTO toDTO(final Configuration configuration) {
        final XConfigurationDTO dto = new XConfigurationDTO();

        dto.pid        = configuration.getPid();
        dto.factoryPid = configuration.getFactoryPid();
        dto.properties = ConsoleAgentHelper.valueOf(configuration.getProperties());
        dto.location   = configuration.getBundleLocation();

        return dto;
    }

}
