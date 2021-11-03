package in.bytehue.osgifx.console.feature;

import java.util.List;

import org.osgi.dto.DTO;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;

public class FeatureExtensionDTO extends DTO {

    public String                   name;
    public Type                     type;
    public Kind                     kind;
    public String                   json;
    public List<String>             text;
    public List<FeatureArtifactDTO> artifacts;

}
