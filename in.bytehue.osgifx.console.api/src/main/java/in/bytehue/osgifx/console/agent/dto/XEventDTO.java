package in.bytehue.osgifx.console.agent.dto;

import java.util.Map;

import org.osgi.dto.DTO;

public class XEventDTO extends DTO {

    public long                received;
    public String              topic;
    public Map<String, String> properties;

}
