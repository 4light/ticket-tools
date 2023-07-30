package test.ticket.tickettools.domain.bo;

import java.util.Date;

public class QueryTaskInfo {
    private String loginPhone;
    //场馆
    private Integer channel;
    private Date useDate;
    private Boolean done;
    private PageParam page;

    public String getLoginPhone() {
        return loginPhone;
    }

    public void setLoginPhone(String loginPhone) {
        this.loginPhone = loginPhone;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Date getUseDate() {
        return useDate;
    }

    public void setUseDate(Date useDate) {
        this.useDate = useDate;
    }

    public Boolean getDone() {
        return done;
    }

    public PageParam getPage() {
        return page;
    }

    public void setPage(PageParam page) {
        this.page = page;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }
}
