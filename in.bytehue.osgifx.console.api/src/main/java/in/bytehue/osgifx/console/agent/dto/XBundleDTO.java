package in.bytehue.osgifx.console.agent.dto;

import java.util.Map;

import org.osgi.dto.DTO;

public class XBundleDTO extends DTO {

    public long                id;
    public String              state;
    public String              symbolicName;
    public String              version;
    public String              location;
    public String              category;
    public boolean             isFragment;
    public long                lastModified;
    public String              documentation;
    public String              vendor;
    public String              description;
    public int                 startLevel;
    public Map<String, String> exportedPackages;
    public Map<String, String> importedPackages;
    public Map<Long, String>   wiredBundles;
    public Map<Long, String>   registeredServices;
    public Map<String, String> manifestHeaders;
    public Map<Long, String>   usedServices;
    public Map<Long, String>   hostBundles;
    public Map<Long, String>   fragmentsAttached;

}
