package test.ticket.tickettools.domain.bo;

import test.ticket.tickettools.domain.entity.TaskDetailEntity;

import java.io.Serializable;
import java.util.List;

public class InitTaskParam implements Serializable {
    private static final long serialVersionUID = 6028965692262694568L;
    private Long taskId;
    private List<TaskDetailEntity> taskDetailEntityList;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public List<TaskDetailEntity> getTaskDetailEntityList() {
        return taskDetailEntityList;
    }

    public void setTaskDetailEntityList(List<TaskDetailEntity> taskDetailEntityList) {
        this.taskDetailEntityList = taskDetailEntityList;
    }
}
