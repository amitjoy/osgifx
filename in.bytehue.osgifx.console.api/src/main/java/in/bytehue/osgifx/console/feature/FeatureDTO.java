package in.bytehue.osgifx.console.feature;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

public class FeatureDTO extends DTO {

    IdDTO                                id;
    String                               name;
    List<String>                         categories;
    String                               description;
    String                               docURL;
    String                               vendor;
    String                               license;
    String                               scm;
    boolean                              isComplete;
    List<FeatureBundleDTO>               bundles;
    Map<String, FeatureConfigurationDTO> configurations;
    Map<String, FeatureExtensionDTO>     extensions;
    Map<String, Object>                  variables;

}
