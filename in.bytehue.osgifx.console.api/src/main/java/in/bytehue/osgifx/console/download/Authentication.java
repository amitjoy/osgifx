package in.bytehue.osgifx.console.download;

public class Authentication {

    public enum AuthType {
        BASIC
    }

    public AuthType authType = AuthType.BASIC;
    public String   username;
    public String   password;

}