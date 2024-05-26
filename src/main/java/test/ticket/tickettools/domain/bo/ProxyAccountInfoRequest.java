package test.ticket.tickettools.domain.bo;

public class ProxyAccountInfoRequest {
    private String url;
    private String headers;

    public ProxyAccountInfoRequest(String url, String headers) {
        this.url = url;
        this.headers = headers;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }
}
