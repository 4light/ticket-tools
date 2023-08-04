package test.ticket.tickettools.domain.bo;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DoSnatchInfo {
    private Long taskId;
    private List<Long> taskDetailIds;
    private Long userId;
    private String loginPhone;
    private String authorization;
    private Date useDate;
    private Integer session;
    private Map<String,String> nameIDMap;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public List<Long> getTaskDetailIds() {
        return taskDetailIds;
    }

    public void setTaskDetailIds(List<Long> taskDetailIds) {
        this.taskDetailIds = taskDetailIds;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLoginPhone() {
        return loginPhone;
    }

    public void setLoginPhone(String loginPhone) {
        this.loginPhone = loginPhone;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    public Date getUseDate() {
        return useDate;
    }

    public void setUseDate(Date useDate) {
        this.useDate = useDate;
    }

    public Integer getSession() {
        return session;
    }

    public void setSession(Integer session) {
        this.session = session;
    }

    public Map<String, String> getNameIDMap() {
        return nameIDMap;
    }

    public void setNameIDMap(Map<String, String> nameIDMap) {
        this.nameIDMap = nameIDMap;
    }
}
