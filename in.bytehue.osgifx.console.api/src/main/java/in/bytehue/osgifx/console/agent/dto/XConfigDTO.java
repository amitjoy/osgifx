package in.bytehue.osgifx.console.agent.dto;

import java.util.Map;

import org.osgi.dto.DTO;

public class XConfigDTO extends DTO {

    public String              pid;
    public String              factoryPid;
    public Map<String, String> properties;
    public Map<Long, String>   usingBundles;

}
