package in.bytehue.osgifx.console.feature;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

public class FeatureDTO extends DTO {

    public String                               archiveURL;
    public IdDTO                                id;
    public String                               name;
    public List<String>                         categories;
    public String                               description;
    public String                               docURL;
    public String                               vendor;
    public String                               license;
    public String                               scm;
    public boolean                              isComplete;
    public List<FeatureBundleDTO>               bundles;
    public Map<String, FeatureConfigurationDTO> configurations;
    public Map<String, FeatureExtensionDTO>     extensions;

}
