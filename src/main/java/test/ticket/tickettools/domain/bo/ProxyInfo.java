package test.ticket.tickettools.domain.bo;

import java.io.Serializable;

public class ProxyInfo{
    private String ip;
    private Integer port;

    public ProxyInfo(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
