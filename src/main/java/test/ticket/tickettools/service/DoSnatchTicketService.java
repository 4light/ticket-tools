package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.bo.TaskInfo;
import test.ticket.tickettools.domain.entity.TaskEntity;

import java.util.List;

public interface DoSnatchTicketService {

    void initData(TaskEntity taskEntity);
    //获取抢票数据
    List<TaskEntity> getAllUndoneTask();
    //获取抢票数据
    List<DoSnatchInfo> getDoSnatchInfos();
    //执行抢票任务
    void doSnatchingTicket(DoSnatchInfo doSnatchInfo);
}
