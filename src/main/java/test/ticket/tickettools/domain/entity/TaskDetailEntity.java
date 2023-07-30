package test.ticket.tickettools.domain.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class TaskDetailEntity {
    private Long id;

    private Long taskId;

    private String userName;

    @JsonProperty(value="IDCard")
    private String IDCard;

    private String userPhone;

    private Integer age;

    private Date createDate;

    private Date updateDate;

    private Boolean done;

    private Boolean payment;

    public TaskDetailEntity() {
    }

    public TaskDetailEntity(Long id, Long taskId, String userName, String IDCard, String userPhone, Integer age, Date createDate, Date updateDate, Boolean done, Boolean payment) {
        this.id = id;
        this.taskId = taskId;
        this.userName = userName;
        this.IDCard = IDCard;
        this.userPhone = userPhone;
        this.age = age;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.done = done;
        this.payment = payment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
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

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public Boolean getPayment() {
        return payment;
    }

    public void setPayment(Boolean payment) {
        this.payment = payment;
    }
}
