package in.bytehue.osgifx.console.application.dto;

public final class ConnectionSettingDTO {

    private String host;
    private int    port;
    private int    timeout;

    public ConnectionSettingDTO(final String host, final int port, final int timeout) {
        this.host    = host;
        this.port    = port;
        this.timeout = timeout;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

}
