package test.ticket.tickettools.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.service.PalaceMuseumTicketService;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableScheduling
public class DoPalaceMuseumSnatchingSchedule {
    @Resource
    PalaceMuseumTicketService palaceMuseumTicketServiceImpl;

    @Scheduled(cron = "0/8 59 19 * * ?")
    public void initData() {
        palaceMuseumTicketServiceImpl.initData();
    }

    @Scheduled(cron = "0/1 2-30 20 * * ?")
    public void doPalaceMuseumTicketSnatch() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("palaceMuseumProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        List<DoSnatchInfo> doSnatchInfos = palaceMuseumTicketServiceImpl.snatchingTicket();
        if (ObjectUtils.isEmpty(doSnatchInfos)) {
            return;
        }
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
