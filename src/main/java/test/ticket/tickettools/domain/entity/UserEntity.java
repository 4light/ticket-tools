package test.ticket.tickettools.domain.entity;

import java.util.Date;

public class UserEntity {
    private Long id;
    private String userName;
    private String pwd;
    private String role;
    private Boolean status;
    private Boolean yn;
    private Date create_date;
    private Date update_date;

    public UserEntity(Long id, String userName, String pwd, String role, Boolean status, Boolean yn, Date create_date, Date update_date) {
        this.id = id;
        this.userName = userName;
        this.pwd = pwd;
        this.role = role;
        this.status = status;
        this.yn = yn;
        this.create_date = create_date;
        this.update_date = update_date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getYn() {
        return yn;
    }

    public void setYn(Boolean yn) {
        this.yn = yn;
    }

    public Date getCreate_date() {
        return create_date;
    }

    public void setCreate_date(Date create_date) {
        this.create_date = create_date;
    }

    public Date getUpdate_date() {
        return update_date;
    }

    public void setUpdate_date(Date update_date) {
        this.update_date = update_date;
    }
}
