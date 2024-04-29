package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.DoSnatchInfo;

import java.util.List;

public interface ChnMuseumTicketService {

    void initData();
    //获取抢票数据
    List<DoSnatchInfo> snatchingTicket();
    //执行抢票任务
    void doSnatchingTicket(DoSnatchInfo doSnatchInfo);
}
