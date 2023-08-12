package test.ticket.tickettools.domain.bo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

public class PlaceOrderInfo {
    private Long taskId;
    private Long id;
    private String userName;
    @JsonProperty(value="IDCard")
    private String IDCard;
    private String authorization;
    private Integer ticketNum;
    private Date date;
    private String loginPhone;
    private Integer childTicketNum;
    private List ticketInfoList;
    private List<Long> taskDetailIds;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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

    public String getIDCard() {
        return IDCard;
    }

    public void setIDCard(String IDCard) {
        this.IDCard = IDCard;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    public Integer getTicketNum() {
        return ticketNum;
    }

    public void setTicketNum(Integer ticketNum) {
        this.ticketNum = ticketNum;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getLoginPhone() {
        return loginPhone;
    }

    public void setLoginPhone(String loginPhone) {
        this.loginPhone = loginPhone;
    }

    public Integer getChildTicketNum() {
        return childTicketNum;
    }

    public void setChildTicketNum(Integer childTicketNum) {
        this.childTicketNum = childTicketNum;
    }

    public List getTicketInfoList() {
        return ticketInfoList;
    }

    public void setTicketInfoList(List ticketInfoList) {
        this.ticketInfoList = ticketInfoList;
    }

    public List<Long> getTaskDetailIds() {
        return taskDetailIds;
    }

    public void setTaskDetailIds(List<Long> taskDetailIds) {
        this.taskDetailIds = taskDetailIds;
    }
}
