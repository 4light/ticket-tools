package test.ticket.tickettools.dao;

import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.TaskEntity;

import java.util.List;

@Repository
public interface TaskDao{
    Integer insert(TaskEntity taskEntity);
    TaskEntity selectByPrimaryKey(Long id);
    Integer updateTask(TaskEntity taskEntity);
    Integer updateAuthByPhone(TaskEntity taskEntity);
    Integer deleteByPrimaryKey(Long id);
    List<TaskEntity> fuzzyQuery(TaskEntity taskEntity);
    //获取当天需要抢票的任务
    List<TaskEntity> getUnDoneTasks(TaskEntity taskEntity);
    //获取所有需要抢票的任务
    List<TaskEntity> getAllUnDoneTasks(TaskEntity taskEntity);
    List<TaskEntity> getUnpaidTasks(Long taskId);
}
