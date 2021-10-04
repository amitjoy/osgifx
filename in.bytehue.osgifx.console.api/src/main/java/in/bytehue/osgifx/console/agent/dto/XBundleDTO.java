package in.bytehue.osgifx.console.agent.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

public class XBundleDTO extends DTO {

    public long                  id;
    public String                state;
    public String                symbolicName;
    public String                version;
    public String                location;
    public String                category;
    public boolean               isFragment;
    public long                  lastModified;
    public String                documentation;
    public String                vendor;
    public String                description;
    public int                   startLevel;
    public List<XPackageDTO>     exportedPackages;
    public List<XPackageDTO>     importedPackages;
    public List<XBundleInfoDTO>  wiredBundles;
    public List<XServiceInfoDTO> registeredServices;
    public Map<String, String>   manifestHeaders;
    public List<XServiceInfoDTO> usedServices;
    public List<XBundleInfoDTO>  hostBundles;
    public List<XBundleInfoDTO>  fragmentsAttached;

}
