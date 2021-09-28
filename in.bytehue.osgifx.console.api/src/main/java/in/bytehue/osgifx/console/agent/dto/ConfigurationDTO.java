package in.bytehue.osgifx.console.agent.dto;

import java.util.Map;

import org.osgi.dto.DTO;

/**
 * Data Transfer Object for a configuration.
 */
public class ConfigurationDTO extends DTO {

    /**
     * Persistent Identifier
     */
    public String pid;

    /**
     * Factory Identifier
     */
    public String factoryPid;

    /**
     * Associated properties
     */
    public Map<String, Object> properties;

}
