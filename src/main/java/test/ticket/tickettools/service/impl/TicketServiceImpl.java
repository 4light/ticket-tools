package test.ticket.tickettools.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import test.ticket.tickettools.dao.PhoneInfoDao;
import test.ticket.tickettools.dao.TaskDetailDao;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.domain.bo.QueryTaskInfo;
import test.ticket.tickettools.domain.bo.ScheduleInfo;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.constant.ResponseCodeEnum;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.TicketService;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TicketServiceImpl implements TicketService {

    @Resource
    TaskDao taskDao;

    @Resource
    TaskDetailDao taskDetailDao;

    @Resource
    PhoneInfoDao phoneInfoDao;

    @Override
    public List<ScheduleInfo> getScheduleInfo() {
        return null;
    }

    @Override
    public ServiceResponse addTask(TaskEntity taskEntity) {
        if(taskDao.insert(taskEntity)>0){
            return ServiceResponse.createBySuccess();
        }
        return ServiceResponse.createByError();
    }

    @Override
    public ServiceResponse addTaskDetail(TaskDetailEntity taskDetailEntity) {
        if(taskDetailDao.insert(taskDetailEntity)>0){
            return ServiceResponse.createBySuccess();
        }
        return ServiceResponse.createByError();
    }

    @Override
    public ServiceResponse<List<TaskEntity>> queryTask(QueryTaskInfo queryTaskInfo) {
        TaskEntity query=new TaskEntity();
        BeanUtil.copyProperties(queryTaskInfo,query);
        return ServiceResponse.createBySuccess(taskDao.fuzzyQueryTestSheet(query));
    }

    @Override
    public ServiceResponse<TaskEntity> getTask(Long taskId) {
        return ServiceResponse.createBySuccess(taskDao.selectByPrimaryKey(taskId));
    }

    @Override
    public ServiceResponse<List<TaskDetailEntity>> queryTaskDetail(Long taskId) {
        return ServiceResponse.createBySuccess(taskDetailDao.selectByTaskId(taskId));
    }

    @Override
    public ServiceResponse addPhoneInfo(PhoneInfoEntity phoneInfoEntity) {
        Integer res = phoneInfoDao.insert(phoneInfoEntity);
        if(res>0){
            ServiceResponse.createBySuccess();
        }
        return ServiceResponse.createByError();
    }

    @Override
    public void snatchingTicket() {

    }
}
