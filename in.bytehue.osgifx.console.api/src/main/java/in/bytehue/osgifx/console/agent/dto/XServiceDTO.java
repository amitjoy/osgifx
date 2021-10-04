package in.bytehue.osgifx.console.agent.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

public class XServiceDTO extends DTO {

    public long                 id;
    public XBundleInfoDTO       bundle;
    public Map<String, String>  properties;
    public List<XBundleInfoDTO> usingBundles;

}
