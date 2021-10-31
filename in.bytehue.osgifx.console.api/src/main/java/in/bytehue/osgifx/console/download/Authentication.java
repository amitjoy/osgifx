package in.bytehue.osgifx.console.download;

public class Authentication {
    public enum AuthType {
        BASIC
    }

    private AuthType authType = AuthType.BASIC;
    private String   username;
    private String   password;

    public Authentication() {
    }

    public Authentication(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(final AuthType authType) {
        this.authType = authType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}