package in.bytehue.osgifx.console.agent.dto;

import org.osgi.dto.DTO;

public class XPackageDTO extends DTO {

    public String       name;
    public String       version;
    public XpackageType type;

    public enum XpackageType {
        EXPORT,
        IMPORT
    }

}
