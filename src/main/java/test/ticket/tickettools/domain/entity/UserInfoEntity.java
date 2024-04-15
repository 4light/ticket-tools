package test.ticket.tickettools.domain.entity;


import java.util.Date;

public class UserInfoEntity {
    private Long id;

    private String phoneNum;

    private Integer channel;

    private Long userId;
    private String content;
    private Date createDate;
    private Date updateDate;

    public UserInfoEntity() {
    }

    public UserInfoEntity(Long id, String phoneNum, Integer channel, Long userId, String content, Date createDate, Date updateDate) {
        this.id = id;
        this.phoneNum = phoneNum;
        this.channel = channel;
        this.userId = userId;
        this.content = content;
        this.createDate = createDate;
        this.updateDate = updateDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
