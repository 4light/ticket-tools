package test.ticket.tickettools.domain.bo;

public class QueryUserInfoRequest {
    private String userName;
    private String phoneNum;
    private PageParam page;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public PageParam getPage() {
        return page;
    }

    public void setPage(PageParam page) {
        this.page = page;
    }
}
