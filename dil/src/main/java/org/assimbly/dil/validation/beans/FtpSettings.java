package org.assimbly.dil.validation.beans;

public class FtpSettings {

    private String user;
    private String host;
    private String pwd;
    private int port;
    private String protocol;
    private boolean explicitTLS;
    private String pkf;
    private String pkfd;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean getExplicitTLS() {
        return explicitTLS;
    }

    public void setExplicitTLS(boolean explicitTLS) {
        this.explicitTLS = explicitTLS;
    }

    public String getPkf() {
        return pkf;
    }

    public void setPkf(String pkf) {
        this.pkf = pkf;
    }

    public String getPkfd() {
        return pkfd;
    }

    public void setPkfd(String pkfd) {
        this.pkfd = pkfd;
    }
}
