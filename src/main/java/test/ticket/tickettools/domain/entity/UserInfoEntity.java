package test.ticket.tickettools.domain.entity;


import java.util.Date;

public class UserInfoEntity {
    private Long id;

    private String phoneNum;

    private Integer channel;

    private String channelUserId;
    private String account;
    private Date createDate;
    private Date updateDate;
    private String userName;
    private String pwd;
    private String nickName;
    private String idCard;
    private String idType;
    private String headers;
    private Boolean yn;
    private String ext;
    private Boolean status;

    public UserInfoEntity() {
    }

    public UserInfoEntity(Long id, String phoneNum, Integer channel, String channelUserId, String account, Date createDate, Date updateDate, String userName, String pwd, String nickName, String idCard, String idType, String headers, Boolean yn, String ext, Boolean status) {
        this.id = id;
        this.phoneNum = phoneNum;
        this.channel = channel;
        this.channelUserId = channelUserId;
        this.account = account;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.userName = userName;
        this.pwd = pwd;
        this.nickName = nickName;
        this.idCard = idCard;
        this.idType = idType;
        this.headers = headers;
        this.yn = yn;
        this.ext = ext;
        this.status = status;
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

    public String getChannelUserId() {
        return channelUserId;
    }

    public void setChannelUserId(String channelUserId) {
        this.channelUserId = channelUserId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
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

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
