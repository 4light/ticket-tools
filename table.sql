create table if not exists phone_info2
(
	id int auto_increment
		primary key,
	name varchar(200) null
);

create table if not exists task
(
	id bigint auto_increment
		primary key,
	login_phone varchar(30) null,
	ip varchar(30) null,
	use_date datetime null comment '使用时间',
	auth varchar(255) null comment '鉴权',
	done tinyint default 0 null,
	create_date datetime default CURRENT_TIMESTAMP null,
	update_date datetime null,
	user_id varchar(200) null,
	channel int null,
	venue int null,
	session int null,
	yn tinyint default 0 null,
	user_name varchar(30) null comment '登录的用户名',
	headers mediumtext null comment '微信请求头',
	nick_name varchar(20) null comment '昵称 如:微信昵称'
);

create table if not exists task_detail
(
	id bigint auto_increment
		primary key,
	task_id bigint null,
	user_name varchar(200) null comment '用户名',
	ID_card varchar(30) null comment '证件id',
	user_phone varchar(20) null comment '电话号',
	age int null comment '年龄',
	create_date datetime default CURRENT_TIMESTAMP null,
	update_date datetime null,
	done tinyint default 0 null,
	payment tinyint null,
	ticket_id mediumtext null comment '票id',
	children_ticket tinyint null,
	order_number varchar(200) null,
	yn tinyint default 0 null
);

create table if not exists user_info
(
	id bigint auto_increment
		primary key,
	phone_num varchar(30) null,
	channel int null,
	user_id bigint null comment '使用时间',
	content varchar(255) null comment '鉴权',
	create_date datetime default CURRENT_TIMESTAMP null,
	update_date datetime null,
	user_name varchar(50) null comment '用户名',
	pwd varchar(100) null comment '密码',
	nick_name varchar(50) null comment '昵称',
	id_card varchar(50) null comment '身份证、护照等有效证件',
	id_type varchar(10) null comment '证件类型',
	headers mediumtext null comment '请求头'
);

