package test.ticket.tickettools.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;

import java.util.List;

@Repository
public interface TaskDetailDao {
    Integer insert(TaskDetailEntity taskDetailEntity);
    Integer insertBatch(List<TaskDetailEntity> taskDetailEntityList);
    Integer updateTaskDetail(TaskDetailEntity taskDetailEntity);
    List<TaskDetailEntity> selectByTaskId(Long id);
    List<TaskDetailEntity> queryAllTaskDetailById(Long id);
    List<TaskDetailEntity> selectUnpaid();
    List<TaskDetailEntity> selectByEntity(TaskDetailEntity entity);
    List<TaskDetailEntity> selectByTaskIdLimit(Long id);
    void updateTaskDetailBath(@Param("list")List<TaskDetailEntity> taskDetailIds);
    void deleteTaskDetailBath(@Param("list")List<TaskDetailEntity> taskDetailIds);
    Integer deleteByTaskId(Long taskId);
    Integer initTaskDetail(@Param("list")List<TaskDetailEntity> taskDetailIds);
    Integer updateByTaskId(Long id);
    Integer updateEntityByTaskId(TaskDetailEntity taskDetailEntity);
    Integer deleteByTaskIdAndCardId(TaskDetailEntity taskDetailEntity);
}
