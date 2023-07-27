package test.ticket.tickettools.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("phone_info")
public class PhoneInfoEntity {
    @TableId(value = "pk_productId", type = IdType.AUTO)
    private Long id;

    @TableField("phone_num")
    private String phoneNum;

    @TableField("channel")
    private Integer channel;

    @TableField("user_id")
    private Long userId;
    @TableField("content")
    private String content;
    @TableField("create_date")
    private Date createDate;
    @TableField("update_date")
    private Date updateDate;
}
