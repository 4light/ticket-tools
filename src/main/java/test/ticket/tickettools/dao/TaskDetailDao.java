package test.ticket.tickettools.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;

import java.util.List;

@Repository
public interface TaskDetailDao {
    Integer insert(TaskDetailEntity taskDetailEntity);
    List<TaskDetailEntity> selectByTaskId(Long id);
}
