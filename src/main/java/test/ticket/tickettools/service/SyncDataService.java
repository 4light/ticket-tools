package test.ticket.tickettools.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.dao.AccountInfoDao;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.dao.TaskDetailDao;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.constant.RedisKeyEnum;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.utils.DateUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SyncDataService {
    @Resource
    RedisService redisService;
    @Resource
    TaskDao taskDao;
    @Resource
    TaskDetailDao taskDetailDao;
    @Resource
    AccountInfoDao accountInfoDao;

    public void syncTickingDayData(){
        log.info("同步放票日数据");
        /*LocalDate now = LocalDate.now();
        LocalDate snatchDate = now.plusDays(7L);
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.CSTM.getCode());
        taskEntity.setUseDate(DateUtils.localDateToDate(snatchDate));
        List<TaskEntity> taskEntities = taskDao.getUnDoneTasks(taskEntity);
        if (ObjectUtils.isEmpty(taskEntities)) {
            return;
        }
        List<String> taskIds=new ArrayList<>();
        for (TaskEntity entity : taskEntities) {
            taskIds.add(String.valueOf(entity.getId()));
            Long id = entity.getId();
            Long userInfoId = entity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
            TaskDetailEntity queryEntity=new TaskDetailEntity();
            queryEntity.setTaskId(id);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(queryEntity);
            if (ObjectUtils.isEmpty(taskDetailEntities)) {
                entity.setDone(true);
                taskDao.updateTask(entity);
            }
            List<List<TaskDetailEntity>> partition = Lists.partition(taskDetailEntities, 5);
            List<String> result = new ArrayList<>();
            for (List<TaskDetailEntity> taskDetailEntityList : partition) {
                DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
                List<Long> taskDetailIds = taskDetailEntityList.stream()
                        .map(TaskDetailEntity::getId) // 提取每个对象的 ID
                        .collect(Collectors.toList());
                Map<String, String> idNameMap = taskDetailEntityList.stream()
                        .collect(Collectors.toMap(TaskDetailEntity::getIDCard, TaskDetailEntity::getUserName));
                doSnatchInfo.setTaskId(id);
                doSnatchInfo.setCreator(entity.getCreator());
                doSnatchInfo.setUserId(Long.valueOf(accountInfoEntity.getChannelUserId()));
                doSnatchInfo.setAccount(entity.getAccount());
                doSnatchInfo.setAuthorization(accountInfoEntity.getHeaders());
                doSnatchInfo.setSession(entity.getSession());
                doSnatchInfo.setUseDate(entity.getUseDate());
                doSnatchInfo.setTaskDetailIds(taskDetailIds);
                doSnatchInfo.setIdNameMap(idNameMap);
                doSnatchInfo.setType(RedisKeyEnum.TICKETINGDAY.getCode());
                result.add(JSON.toJSONString(doSnatchInfo));
            }
            redisService.saveList(RedisKeyEnum.TASK.getCode()+id,result);
        }
        redisService.saveList(RedisKeyEnum.TICKETINGDAY.getCode(),taskIds);*/
    }
    public void syncNormalData(){
        log.info("同步日常数据");
        /*List<String> list = redisService.getList(RedisKeyEnum.NORMAL.getCode());
        for (String key : list) {
            redisService.deleteKey(key);
        }
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUseDate(DateUtils.localDateToDate(LocalDate.now().minusDays(1L)));
        taskEntity.setChannel(ChannelEnum.CSTM.getCode());
        List<TaskEntity> allUnDoneTasks = taskDao.getAllUnDoneTasks(taskEntity);
        if (ObjectUtils.isEmpty(allUnDoneTasks)) {
            return ;
        }
        List<String> taskAndDetailIds=new ArrayList<>();
        List<String> normalTasks=new ArrayList<>();
        for (TaskEntity entity : allUnDoneTasks) {
            normalTasks.add(String.valueOf(entity.getId()));
            Long userInfoId = entity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
            TaskDetailEntity query = new TaskDetailEntity();
            query.setTaskId(entity.getId());
            query.setDone(false);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(query);
            if (ObjectUtils.isEmpty(taskDetailEntities)) {
                entity.setDone(true);
                taskDao.updateTask(entity);
            }
            for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
                DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
                doSnatchInfo.setType(RedisKeyEnum.NORMAL.getCode());
                doSnatchInfo.setCreator(entity.getCreator());
                doSnatchInfo.setTaskId(entity.getId());
                doSnatchInfo.setUserId(accountInfoEntity.getChannelUserId()==null?null:Long.valueOf(accountInfoEntity.getChannelUserId()));
                doSnatchInfo.setAccount(entity.getAccount());
                doSnatchInfo.setAuthorization(accountInfoEntity.getHeaders());
                doSnatchInfo.setUseDate(entity.getUseDate());
                doSnatchInfo.setSession(entity.getSession());
                doSnatchInfo.setTaskDetailIds(Arrays.asList(taskDetailEntity.getId()));
                doSnatchInfo.setSession(entity.getSession());
                doSnatchInfo.setIdNameMap(new HashMap<String, String>() {{
                    put(taskDetailEntity.getIDCard(), taskDetailEntity.getUserName());
                }});
                redisService.saveList(entity.getId()+":"+taskDetailEntity.getId(),Arrays.asList(JSON.toJSONString(doSnatchInfo)));
                taskAndDetailIds.add(entity.getId()+":"+taskDetailEntity.getId());
            }
            redisService.saveList(RedisKeyEnum.TASK.getCode(), normalTasks);
            redisService.saveList(RedisKeyEnum.NORMAL.getCode(), taskAndDetailIds);
        }*/
    }
}
