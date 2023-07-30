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
    List<TaskEntity> fuzzyQuery(TaskEntity taskEntity);
    List<TaskEntity> getDoneTasks(TaskEntity taskEntity);
}
