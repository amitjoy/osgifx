package in.bytehue.osgifx.console.feature;

import java.util.Map;

import org.osgi.dto.DTO;

public class FeatureConfigurationDTO extends DTO {

    String              pid;
    String              factoryPid;
    Map<String, Object> values;

}
