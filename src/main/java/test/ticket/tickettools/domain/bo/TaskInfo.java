package test.ticket.tickettools.domain.bo;

import test.ticket.tickettools.domain.entity.TaskDetailEntity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class TaskInfo implements Serializable {
    private static final long serialVersionUID = 2517797866070411812L;
    private Long taskId;
    private String loginPhone;
    private String auth;
    //渠道
    private Integer channel;
    //场馆
    private Integer venue;
    //场次
    private Integer session;
    private Date useDate;
    private List<TaskDetailEntity> userList;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getLoginPhone() {
        return loginPhone;
    }

    public void setLoginPhone(String loginPhone) {
        this.loginPhone = loginPhone;
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

    public List<TaskDetailEntity> getUserList() {
        return userList;
    }

    public void setUserList(List<TaskDetailEntity> userList) {
        this.userList = userList;
    }
}
