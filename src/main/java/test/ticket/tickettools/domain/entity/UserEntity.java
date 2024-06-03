package test.ticket.tickettools.domain.entity;

import java.util.Date;

public class UserEntity {
    private Long id;
    private String nickName;
    private String userName;
    private String pwd;
    private String role;
    private Boolean status;
    private Boolean yn;
    private String ext;
    private Date createDate;
    private Date updateDate;

    public UserEntity() {
    }

    public UserEntity(Long id, String nickName, String userName, String pwd, String role, Boolean status, Boolean yn, String ext, Date createDate, Date updateDate) {
        this.id = id;
        this.nickName = nickName;
        this.userName = userName;
        this.pwd = pwd;
        this.role = role;
        this.status = status;
        this.yn = yn;
        this.ext = ext;
        this.createDate = createDate;
        this.updateDate = updateDate;
    }

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

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
}
