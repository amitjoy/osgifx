package in.bytehue.osgifx.console.agent.dto;

import java.util.List;

import org.osgi.dto.DTO;

public class XObjectClassDefDTO extends DTO {

    public String                 id;
    public String                 name;
    public String                 description;
    public String                 descriptorLocation;
    public List<XAttributeDefDTO> attributeDefs;

}
