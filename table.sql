create table phone_info2
(
    id   int auto_increment
        primary key,
    name varchar(200) null
);

create table task
(
    id           bigint auto_increment
        primary key,
    login_phone  varchar(30)                        null,
    ip           varchar(30)                        null,
    use_date     datetime                           null comment '使用时间',
    auth         varchar(255)                       null comment '鉴权',
    done         tinyint  default 0                 null,
    create_date  datetime default CURRENT_TIMESTAMP null,
    update_date  datetime                           null,
    user_id      varchar(200)                       null,
    channel      int                                null,
    venue        int                                null,
    session      int                                null,
    yn           tinyint  default 0                 null,
    user_name    varchar(30)                        null comment '登录的用户名',
    account      mediumtext                         null comment '账号',
    nick_name    varchar(20)                        null comment '昵称 如:微信昵称',
    port         int                                null comment '端口',
    user_info_id bigint                             null comment '映射的userInfo表id',
    pwd          varchar(50)                        null comment '密码'
);

create table task_detail
(
    id              bigint auto_increment
        primary key,
    task_id         bigint                             null,
    user_name       varchar(200)                       null comment '用户名',
    ID_card         varchar(30)                        null comment '证件id',
    user_phone      varchar(20)                        null comment '电话号',
    age             int                                null comment '年龄',
    create_date     datetime default CURRENT_TIMESTAMP null,
    update_date     datetime                           null,
    done            tinyint  default 0                 null,
    payment         tinyint                            null,
    ticket_id       mediumtext                         null comment '票id',
    children_ticket tinyint                            null,
    order_number    varchar(200)                       null,
    yn              tinyint  default 0                 null,
    order_id        mediumtext                         null comment '订单号',
    price           int                                null comment '订单价格'
);

create table user_info
(
    id              bigint auto_increment
        primary key,
    phone_num       varchar(30)                        null,
    channel         int                                null,
    channel_user_id varchar(100)                       null comment '用户id',
    account         varchar(255)                       null comment '账号',
    create_date     datetime default CURRENT_TIMESTAMP null,
    update_date     datetime                           null,
    user_name       varchar(50)                        null comment '用户名',
    pwd             varchar(100)                       null comment '密码',
    nick_name       varchar(50)                        null comment '昵称',
    id_card         varchar(50)                        null comment '身份证、护照等有效证件',
    id_type         varchar(10)                        null comment '证件类型',
    headers         mediumtext                         null comment '请求头',
    yn              tinyint  default 0                 null comment '是否有效',
    ext             varchar(200)                       null comment '扩展信息',
    status          tinyint  default 0                 null comment '状态，是否被禁用 0否 1是'
);