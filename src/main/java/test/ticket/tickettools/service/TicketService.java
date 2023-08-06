package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;

import java.util.List;
import java.util.Map;

public interface TicketService {
    //获取场次信息
    List<ScheduleInfo> getScheduleInfo();
    //添加任务
    ServiceResponse addTaskInfo(TaskInfo taskInfo);
    //添加任务详情
    ServiceResponse addTaskDetail(TaskDetailEntity taskDetailEntity);
    //更新任务
    void updateTask(TaskEntity taskEntity);

    ServiceResponse<PageableResponse<TaskInfoListResponse>> queryTask(QueryTaskInfo queryTaskInfo);

    ServiceResponse<TaskInfo> getTask(Long taskId);

    ServiceResponse delete(Long taskId);

    List<TaskDetailEntity> selectUnpaid();

    ServiceResponse updateTaskDetail(UpdateTaskDetailRequest updateTaskDetailRequest);
    //添加手机信息
    ServiceResponse addPhoneInfo(PhoneInfoEntity phoneInfoEntity);

    ServiceResponse getPhoneMsg(String phoneNum);
    //获取需要执行的任务
    Map<String, DoSnatchInfo> getTaskForRun();
    //获取所有需要单个执行的任务
    List<DoSnatchInfo> getAllTaskForRun();
    //获取所有未执行完的任务
    List<TaskEntity> getAllUnDoneTask();
    //执行抢票任务
    void snatchingTicket(DoSnatchInfo doSnatchInfo);
    //支付
    String pay(PlaceOrderInfo placeOrderInfo);
}
