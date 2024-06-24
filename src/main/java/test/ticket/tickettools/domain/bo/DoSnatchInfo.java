package test.ticket.tickettools.domain.bo;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DoSnatchInfo {
    private Long taskId;
    private List<Long> taskDetailIds;
    private String creator;
    private Long userId;
    private String loginPhone;
    private String authorization;
    private Date useDate;
    private String session;
    private Map<String,String> IdNameMap;
    private Long userInfoId;
    private String ip;
    private Integer port;
    private String account;
    private String pwd;
    private String headers;
    private String channelUserId;

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

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
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

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public Map<String, String> getIdNameMap() {
        return IdNameMap;
    }

    public void setIdNameMap(Map<String, String> idNameMap) {
        this.IdNameMap = idNameMap;
    }

    public Long getUserInfoId() {
        return userInfoId;
    }

    public void setUserInfoId(Long userInfoId) {
        this.userInfoId = userInfoId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
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

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getChannelUserId() {
        return channelUserId;
    }

    public void setChannelUserId(String channelUserId) {
        this.channelUserId = channelUserId;
    }
}
