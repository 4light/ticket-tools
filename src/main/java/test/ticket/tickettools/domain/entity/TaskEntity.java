package test.ticket.tickettools.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("task")
public class TaskEntity {

    @TableId(value = "pk_productId",type = IdType.AUTO)
    private Long id;

    @TableField("login_phone")
    private String loginPhone;

    @TableField("ip")
    private String ip;

    @TableField("use_date")
    private Date useDate;

    @TableField("auth")
    private String auth;
    //是否抢完
    @TableField("done")
    private Boolean done;

    @TableField("create_date")
    private Date createDate;

    @TableField("update_date")
    private Date updateDate;

    @TableField("user_id")
    private Integer userId;
    //渠道 如科技馆、故宫
    @TableField("channel")
    private Integer channel;
    //场馆
    @TableField("venue")
    private Integer venue;
    //场次
    @TableField("session")
    private Integer session;





}
