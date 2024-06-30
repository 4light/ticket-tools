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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableScheduling
public class DoPalaceMuseumSnatchingSchedule {
    @Resource
    DoSnatchTicketService palaceMuseumTicketServiceImpl;

    @Scheduled(cron = "0/1 59 19 * * ?")
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


    @Scheduled(cron = "0/1 01-10 21 * * ?")
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
}
