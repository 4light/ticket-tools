package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.QueryTaskInfo;
import test.ticket.tickettools.domain.bo.ScheduleInfo;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;

import java.util.List;

public interface TicketService {
    //获取场次信息
    List<ScheduleInfo> getScheduleInfo();
    //添加任务
    ServiceResponse addTask(TaskEntity taskEntity);
    //添加任务详情
    ServiceResponse addTaskDetail(TaskDetailEntity taskDetailEntity);
    ServiceResponse<List<TaskEntity>> queryTask(QueryTaskInfo queryTaskInfo);
    ServiceResponse<TaskEntity> getTask(Long taskId);
    ServiceResponse<List<TaskDetailEntity>> queryTaskDetail(Long taskId);
    //添加手机信息
    ServiceResponse addPhoneInfo(PhoneInfoEntity phoneInfoEntity);
    void snatchingTicket();
}
