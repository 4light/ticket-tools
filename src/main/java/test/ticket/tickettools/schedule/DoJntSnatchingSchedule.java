package test.ticket.tickettools.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.bo.ProxyInfo;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.DoSnatchTicketService;
import test.ticket.tickettools.utils.ProxyUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Configuration
@EnableScheduling
public class DoJntSnatchingSchedule {
    @Resource
    DoSnatchTicketService jntTicketServiceImpl;


    @Scheduled(cron = "0/30 28-29 12 * * ?")
    public void initData(){
        List<TaskEntity> allUndoneTask = jntTicketServiceImpl.getAllUndoneTask();
        if (ObjectUtils.isEmpty(allUndoneTask)) {
            return;
        }
        int size = allUndoneTask.size();
        List<ProxyInfo> proxyList = ProxyUtil.getProxyList(size);
        List<TaskEntity> newTaskEntityList=new ArrayList<>();
        for (int i = 0; i < allUndoneTask.size(); i++) {
            TaskEntity taskEntity = allUndoneTask.get(i);
            ProxyInfo proxyInfo = proxyList.get(i);
            taskEntity.setIp(proxyInfo.getIp());
            taskEntity.setPort(proxyInfo.getPort());
            newTaskEntityList.add(taskEntity);
        }
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("jntTicketDataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        for (TaskEntity taskEntity : newTaskEntityList) {
            CompletableFuture.runAsync(()->jntTicketServiceImpl.initData(taskEntity),pool);
        }
    }

    @Scheduled(cron = "0/2 30-32 12 * * ?")
    public void doJntTicketSnatch(){
        List<DoSnatchInfo> doSnatchInfos = jntTicketServiceImpl.getDoSnatchInfos();
        if(ObjectUtils.isEmpty(doSnatchInfos)){
            return;
        }
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("jntProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = doSnatchInfos.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        for (DoSnatchInfo doSnatchInfo : doSnatchInfos) {
            CompletableFuture.runAsync(() -> jntTicketServiceImpl.doSnatchingTicket(doSnatchInfo),pool);
        }
    }
}
