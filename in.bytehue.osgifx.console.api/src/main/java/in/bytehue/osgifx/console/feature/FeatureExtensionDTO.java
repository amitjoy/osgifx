package in.bytehue.osgifx.console.feature;

import java.util.List;

import org.osgi.dto.DTO;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;

public class FeatureExtensionDTO extends DTO {

    String                   name;
    Type                     type;
    Kind                     kind;
    String                   json;
    List<String>             text;
    List<FeatureArtifactDTO> artifacts;

}
