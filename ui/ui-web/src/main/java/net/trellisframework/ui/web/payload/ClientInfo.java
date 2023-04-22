package net.trellisframework.ui.web.payload;

import net.trellisframework.core.payload.Payload;

public class ClientInfo implements Payload {
    private String ip;
    private String session;

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSession() {
        return this.session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public ClientInfo() {
    }

    public ClientInfo(String ip, String session) {
        this.ip = ip;
        this.session = session;
    }

    public String toString() {
        return "ClientInfo{ip='" + this.ip + '\'' + ", session='" + this.session + '\'' + '}';
    }
}
