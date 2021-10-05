package in.bytehue.osgifx.console.application.dialog;

import java.util.Objects;

public final class ConnectionSettingDTO {

    public String host;
    public int    port;
    public int    timeout;

    public ConnectionSettingDTO() {
        // needed for GSON
    }

    public ConnectionSettingDTO(final String host, final int port, final int timeout) {
        this.host    = host;
        this.port    = port;
        this.timeout = timeout;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, timeout);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ConnectionSettingDTO other = (ConnectionSettingDTO) obj;
        return Objects.equals(host, other.host) && port == other.port && timeout == other.timeout;
    }

    @Override
    public String toString() {
        return "ConnectionSettingDTO [host=" + host + ", port=" + port + ", timeout=" + timeout + "]";
    }

}
