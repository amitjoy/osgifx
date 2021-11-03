package in.bytehue.osgifx.console.feature;

import java.util.Map;

import org.osgi.dto.DTO;

public class FeatureConfigurationDTO extends DTO {

    public String              pid;
    public String              factoryPid;
    public Map<String, Object> values;

}
