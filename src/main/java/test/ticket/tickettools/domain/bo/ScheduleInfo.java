package test.ticket.tickettools.domain.bo;

import java.io.Serializable;
import java.util.Date;

public class ScheduleInfo implements Serializable {
    private static final long serialVersionUID = 4290283206696217582L;
    //场次id
    private Integer hallScheduleId;
    //场次名称
    private String scheduleName;
    //场次开始时间
    private Date startTime;
    //场次结束时间
    private Date endTime;

    public Integer getHallScheduleId() {
        return hallScheduleId;
    }

    public void setHallScheduleId(Integer hallScheduleId) {
        this.hallScheduleId = hallScheduleId;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
