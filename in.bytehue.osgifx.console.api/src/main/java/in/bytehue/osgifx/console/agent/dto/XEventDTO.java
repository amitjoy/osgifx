package in.bytehue.osgifx.console.agent.dto;

import java.time.LocalDateTime;
import java.util.Map;

import org.osgi.dto.DTO;

public class XEventDTO extends DTO {

    public String              topic;
    public LocalDateTime       received;
    public Map<String, String> properties;

}
