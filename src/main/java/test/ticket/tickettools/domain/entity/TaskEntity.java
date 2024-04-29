package test.ticket.tickettools.domain.entity;


import java.util.Date;

public class TaskEntity {

    private Long id;

    private String loginPhone;

    private String ip;

    private Date useDate;

    private String auth;
    //是否抢完
    private Boolean done;

    private Date createDate;

    private Date updateDate;

    private Long userId;
    //渠道 如科技馆、故宫
    private Integer channel;
    //场馆
    private Integer venue;
    //场次 故宫0：上午 1：下午
    private Integer session;

    //删除标识
    private Boolean yn;
    //用户名
    private String userName;
    //账号
    private String account;
    //昵称
    private String nickName;
    //端口
    private Integer port;
    private Long userInfoId;
    private String pwd;


    public TaskEntity() {
    }

    public TaskEntity(Long id, String loginPhone, String ip, Date useDate, String auth, Boolean done, Date createDate, Date updateDate, Long userId, Integer channel, Integer venue, Integer session, Boolean yn, String userName, String account, String nickName, Integer port, Long userInfoId, String pwd) {
        this.id = id;
        this.loginPhone = loginPhone;
        this.ip = ip;
        this.useDate = useDate;
        this.auth = auth;
        this.done = done;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.userId = userId;
        this.channel = channel;
        this.venue = venue;
        this.session = session;
        this.yn = yn;
        this.userName = userName;
        this.account = account;
        this.nickName = nickName;
        this.port = port;
        this.userInfoId = userInfoId;
        this.pwd = pwd;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoginPhone() {
        return loginPhone;
    }

    public void setLoginPhone(String loginPhone) {
        this.loginPhone = loginPhone;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Date getUseDate() {
        return useDate;
    }

    public void setUseDate(Date useDate) {
        this.useDate = useDate;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Boolean getYn() {
        return yn;
    }

    public void setYn(Boolean yn) {
        this.yn = yn;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Long getUserInfoId() {
        return userInfoId;
    }

    public void setUserInfoId(Long userInfoId) {
        this.userInfoId = userInfoId;
    }
}
