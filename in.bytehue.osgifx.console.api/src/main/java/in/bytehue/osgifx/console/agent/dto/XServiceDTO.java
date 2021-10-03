package in.bytehue.osgifx.console.agent.dto;

import java.util.Map;
import java.util.Map.Entry;

import org.osgi.dto.DTO;

public class XServiceDTO extends DTO {

    public long                id;
    public Entry<String, Long> bundle;
    public Map<String, String> properties;
    public Map<Long, String>   usingBundles;

}
