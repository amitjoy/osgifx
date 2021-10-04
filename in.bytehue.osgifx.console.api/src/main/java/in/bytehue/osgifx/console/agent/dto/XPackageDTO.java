package in.bytehue.osgifx.console.agent.dto;

public class XPackageDTO {

    public String       name;
    public String       version;
    public XpackageType type;

    public enum XpackageType {
        EXPORT,
        IMPORT
    }

}
