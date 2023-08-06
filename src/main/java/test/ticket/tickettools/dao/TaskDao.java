package test.ticket.tickettools.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.TaskEntity;

import java.util.List;

@Repository
public interface TaskDao{
    Integer insert(TaskEntity taskEntity);
    TaskEntity selectByPrimaryKey(Long id);
    Integer updateTask(TaskEntity taskEntity);
    Integer deleteByPrimaryKey(Long id);
    List<TaskEntity> fuzzyQuery(TaskEntity taskEntity);
    //获取需要抢票的任务
    List<TaskEntity> getUnDoneTasks(TaskEntity taskEntity);
    //获取所有需要抢票的任务
    List<TaskEntity> getAllUnDoneTasks(TaskEntity taskEntity);
}
