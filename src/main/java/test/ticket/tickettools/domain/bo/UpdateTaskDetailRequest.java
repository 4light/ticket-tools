package test.ticket.tickettools.domain.bo;

import java.io.Serializable;

public class UpdateTaskDetailRequest implements Serializable {
    private static final long serialVersionUID = 3852118099211596867L;

    private Long taskId;
    private Long taskDetailId;
    private Boolean done;
    private Boolean payment;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getTaskDetailId() {
        return taskDetailId;
    }

    public void setTaskDetailId(Long taskDetailId) {
        this.taskDetailId = taskDetailId;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }

    public Boolean getPayment() {
        return payment;
    }

    public void setPayment(Boolean payment) {
        this.payment = payment;
    }
}
