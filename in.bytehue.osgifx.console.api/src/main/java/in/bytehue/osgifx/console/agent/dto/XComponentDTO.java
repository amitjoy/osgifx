package in.bytehue.osgifx.console.agent.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;

public class XComponentDTO extends DTO {

    public String                         id;
    public String                         name;
    public String                         state;
    public String                         registeringBundle;
    public long                           registeringBundleId;
    public String                         factory;
    public String                         scope;
    public String                         implementationClass;
    public String                         configurationPolicy;
    public List<String>                   serviceInterfaces;
    public List<String>                   configurationPid;
    public Map<String, String>            properties;
    public List<ReferenceDTO>             references;
    public String                         failure;
    public String                         activate;
    public String                         deactivate;
    public String                         modified;
    public List<XSatisfiedReferenceDTO>   satisfiedReferences;
    public List<XUnsatisfiedReferenceDTO> unsatisfiedReferences;

}
