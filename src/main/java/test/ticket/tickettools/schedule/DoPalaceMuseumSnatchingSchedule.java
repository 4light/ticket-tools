package test.ticket.tickettools.schedule;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.bo.ProxyInfo;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.DoSnatchTicketService;
import test.ticket.tickettools.utils.DateUtils;
import test.ticket.tickettools.utils.EncDecUtil;
import test.ticket.tickettools.utils.ProxyUtil;
import test.ticket.tickettools.utils.TemplateUtil;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableScheduling
public class DoPalaceMuseumSnatchingSchedule {
    @Resource
    DoSnatchTicketService palaceMuseumTicketServiceImpl;
    @Resource
    TaskDao taskDao;

    //@Scheduled(cron = "0/1 58 19 * * ?")
    public void initData() {
        List<TaskEntity> allUndoneTask = palaceMuseumTicketServiceImpl.getAllUndoneTask();
        if (ObjectUtils.isEmpty(allUndoneTask)) {
            return;
        }
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("palaceMuseumDataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = allUndoneTask.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        for (TaskEntity taskEntity : allUndoneTask) {
            CompletableFuture.runAsync(() ->palaceMuseumTicketServiceImpl.initData(taskEntity), pool);
        }
    }


    @Scheduled(cron = "0/2 14-49 20 * * ?")
    public void doPalaceMuseumTicketSnatch() {
        List<DoSnatchInfo> doSnatchInfos = palaceMuseumTicketServiceImpl.getDoSnatchInfos();
        if (ObjectUtils.isEmpty(doSnatchInfos)) {
            return;
        }
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("palaceMuseumProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = doSnatchInfos.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        for (DoSnatchInfo doSnatchInfo : doSnatchInfos) {
            CompletableFuture.runAsync(() -> palaceMuseumTicketServiceImpl.doSnatchingTicket(doSnatchInfo), pool);
        }
    }
    //@Scheduled(cron = "* 0/5 20-21 * * ?")
    public void updateTaskProxy() {
        List<TaskEntity> allUndoneTask = palaceMuseumTicketServiceImpl.getAllUndoneTask();
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(20, 5);
        LocalTime end = LocalTime.of(20, 30);
        if (now.isAfter(start) && now.isBefore(end)) {
            // 在20:05到20:30之间执行任务
            List<ProxyInfo> proxyList = ProxyUtil.getProxyList(allUndoneTask.size());
            for (int i = 0; i < allUndoneTask.size(); i++) {
                TaskEntity currentEntity = allUndoneTask.get(i);
                ProxyInfo proxyInfo = proxyList.get(i);
                currentEntity.setIp(proxyInfo.getIp());
                currentEntity.setPort(proxyInfo.getPort());
                taskDao.updateTask(currentEntity);
            }
        }
    }
}
