package test.ticket.tickettools.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.DoSnatchTicketService;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableScheduling
public class DoChnMuseumSnatchingSchedule {
    @Resource
    DoSnatchTicketService chnMuseumTicketServiceImpl;

    @Scheduled(cron = "0/2 58 19 * * ?")
    public void initData() {
        List<TaskEntity> allUndoneTask = chnMuseumTicketServiceImpl.getAllUndoneTask();
        if (ObjectUtils.isEmpty(allUndoneTask)) {
            return;
        }
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("chnMuseumDataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = allUndoneTask.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        for (TaskEntity taskEntity : allUndoneTask) {
            CompletableFuture.runAsync(() ->chnMuseumTicketServiceImpl.initData(taskEntity), pool);

        }
    }

    @Scheduled(cron = "0/1 02-29 20 * * ?")
    public void doPalaceMuseumTicketSnatch() {
        List<DoSnatchInfo> doSnatchInfos = chnMuseumTicketServiceImpl.getDoSnatchInfos();
        if (ObjectUtils.isEmpty(doSnatchInfos)) {
            return;
        }
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("chnMuseumProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = doSnatchInfos.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        for (DoSnatchInfo doSnatchInfo : doSnatchInfos) {
            CompletableFuture.runAsync(() -> chnMuseumTicketServiceImpl.doSnatchingTicket(doSnatchInfo), pool);
        }
    }
}
