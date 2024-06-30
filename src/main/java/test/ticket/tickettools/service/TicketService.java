package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;

import java.util.List;
import java.util.Map;

public interface TicketService {
    ServiceResponse getCurrentUser(QueryTaskInfo queryTaskInfo);
    //添加任务
    ServiceResponse addTaskInfo(TaskInfo taskInfo);
    //重置任务
    ServiceResponse initTask(InitTaskParam initTaskParam);
    //添加任务详情
    ServiceResponse addTaskDetail(TaskDetailEntity taskDetailEntity);
    //更新任务
    void updateTask(TaskEntity taskEntity);

    ServiceResponse<PageableResponse<TaskInfoListResponse>> queryTask(QueryTaskInfo queryTaskInfo);

    ServiceResponse<TaskInfo> getTask(Long taskId,Boolean yn);

    ServiceResponse delete(Long taskId,Boolean yn);

    List<TaskDetailEntity> selectUnpaid();

    Boolean updateTaskDetail(TaskDetailEntity taskDetailEntity);
    //添加手机信息
    ServiceResponse addPhoneInfo(AccountInfoEntity accountInfoEntity);

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
    ServiceResponse<String> pay(PlaceOrderInfo placeOrderInfo);
}
