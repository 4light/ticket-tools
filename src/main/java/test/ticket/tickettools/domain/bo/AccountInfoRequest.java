package test.ticket.tickettools.domain.bo;

public class AccountInfoRequest {
    private Integer channel;
    private String userName;
    private String account;
    private String pwd;
    private String creator;
    private String operator;
    private PageParam page;

    public AccountInfoRequest(Integer channel, String userName, String account, String pwd, String creator, String operator, PageParam page) {
        this.channel = channel;
        this.userName = userName;
        this.account = account;
        this.pwd = pwd;
        this.creator = creator;
        this.operator = operator;
        this.page = page;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }



    public PageParam getPage() {
        return page;
    }

    public void setPage(PageParam page) {
        this.page = page;
    }
}
