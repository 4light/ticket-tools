package test.ticket.tickettools.domain.bo;

import test.ticket.tickettools.domain.entity.TaskDetailEntity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class TaskInfo implements Serializable {
    private static final long serialVersionUID = 2517797866070411812L;
    private Long id;
    private String account;
    private String auth;
    //渠道
    private Integer channel;
    //场馆
    private Integer venue;
    //场次
    private Integer session;
    private Date useDate;
    //来源 0:页面 1：插件
    private Integer source;
    private Long userId;
    private Long userInfoId;
    private String creator;
    private List<TaskDetailEntity> userList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getVenue() {
        return venue;
    }

    public void setVenue(Integer venue) {
        this.venue = venue;
    }

    public Integer getSession() {
        return session;
    }

    public void setSession(Integer session) {
        this.session = session;
    }

    public Date getUseDate() {
        return useDate;
    }

    public void setUseDate(Date useDate) {
        this.useDate = useDate;
    }

    public Integer getSource() {
        return source;
    }

    public void setSource(Integer source) {
        this.source = source;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserInfoId() {
        return userInfoId;
    }

    public void setUserInfoId(Long userInfoId) {
        this.userInfoId = userInfoId;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public List<TaskDetailEntity> getUserList() {
        return userList;
    }

    public void setUserList(List<TaskDetailEntity> userList) {
        this.userList = userList;
    }
}
