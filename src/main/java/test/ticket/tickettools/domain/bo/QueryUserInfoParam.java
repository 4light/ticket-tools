package test.ticket.tickettools.domain.bo;

import java.io.Serializable;

public class QueryUserInfoParam implements Serializable {
    private static final long serialVersionUID = -8054135469657277379L;
    private Long id;
    private String nickName;
    private String userName;
    private PageParam page;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public PageParam getPage() {
        return page;
    }

    public void setPage(PageParam page) {
        this.page = page;
    }
}
