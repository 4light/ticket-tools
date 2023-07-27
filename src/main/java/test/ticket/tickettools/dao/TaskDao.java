package test.ticket.tickettools.dao;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.TaskEntity;

@Repository
public interface TaskDao{
    Integer insert(TaskEntity taskEntity);
    TaskEntity selectByPrimaryKey(Long id);
    Page<TaskEntity> fuzzyQueryTestSheet(@Param("taskEntity")TaskEntity taskEntity);
}
