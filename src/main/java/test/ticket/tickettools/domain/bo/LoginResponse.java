package test.ticket.tickettools.domain.bo;

import java.io.Serializable;
import java.util.Date;

public class LoginResponse implements Serializable {
    private static final long serialVersionUID = -3876172281708732791L;
    private Long id;
    private String nickName;
    private String userName;
    private String role;
    private Date createDate;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}

