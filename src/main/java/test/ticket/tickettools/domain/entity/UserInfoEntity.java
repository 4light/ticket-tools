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
    private String userName;
    private String pwd;
    private String nickName;
    private String idCard;
    private String idType;
    private String headers;

    public UserInfoEntity() {
    }

    public UserInfoEntity(Long id, String phoneNum, Integer channel, Long userId, String content, Date createDate, Date updateDate, String userName, String pwd, String nickName, String idCard, String idType, String headers) {
        this.id = id;
        this.phoneNum = phoneNum;
        this.channel = channel;
        this.userId = userId;
        this.content = content;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.userName = userName;
        this.pwd = pwd;
        this.nickName = nickName;
        this.idCard = idCard;
        this.idType = idType;
        this.headers = headers;
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

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }
}
