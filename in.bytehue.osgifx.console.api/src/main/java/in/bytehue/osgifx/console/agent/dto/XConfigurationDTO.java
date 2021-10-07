package in.bytehue.osgifx.console.agent.dto;

import java.util.Map;

import org.osgi.dto.DTO;

public class XConfigurationDTO extends DTO {

    public String              pid;
    public String              factoryPid;
    public String              location;
    public Map<String, Object> properties;
    public XObjectClassDefDTO  ocd;

}
