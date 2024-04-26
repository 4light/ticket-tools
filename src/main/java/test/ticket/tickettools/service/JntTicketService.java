package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.DoSnatchInfo;

import java.util.List;

public interface JntTicketService {

    //执行抢票任务
    void doSnatchingJnt(DoSnatchInfo doSnatchInfo);
    //获取待抢票的任务，并初始化登录态代理等信息
    void initData();
    //组装待抢票数据
    List<DoSnatchInfo> getDoSnatchInfos();
}
