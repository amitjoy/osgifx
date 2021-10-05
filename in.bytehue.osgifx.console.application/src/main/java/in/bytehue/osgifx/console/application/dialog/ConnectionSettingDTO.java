package in.bytehue.osgifx.console.application.dialog;

import org.osgi.dto.DTO;

public final class ConnectionSettingDTO extends DTO {

    public String host;
    public int    port;
    public int    timeout;

    public ConnectionSettingDTO() {
    }

    public ConnectionSettingDTO(final String host, final int port, final int timeout) {
        this.host    = host;
        this.port    = port;
        this.timeout = timeout;
    }

}
