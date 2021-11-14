package in.bytehue.osgifx.console.agent.dto;

import org.osgi.dto.DTO;

public class XLogEntryDTO extends DTO {

    public XBundleDTO bundle;
    public String     level;
    public String     message;
    public String     exception;
    public long       loggedAt;

}
