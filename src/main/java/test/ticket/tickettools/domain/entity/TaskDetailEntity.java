package test.ticket.tickettools.domain.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("task_detail")
public class TaskDetailEntity {
    @TableId(value = "pk_productId",type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("user_name")
    private String userName;

    @TableField("ID_card")
    private String IDCard;

    @TableField("user_phone")
    private String userPhone;

    @TableField("age")
    private Integer age;

    @TableField("create_date")
    private Date createDate;

    @TableField("update_date")
    private Date updateDate;

    @TableField("done")
    private Boolean done;
}
