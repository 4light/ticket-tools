package test.ticket.tickettools.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.service.ChnMuseumTicketService;
import test.ticket.tickettools.service.JntTicketService;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableScheduling
public class DoChnMusSnatchingSchedule {
    @Resource
    ChnMuseumTicketService chnMuseumTicketServiceImpl;

    @Scheduled(cron = "0 59 19 * * ?")
    public void initData(){
        chnMuseumTicketServiceImpl.initData();
    }

    @Scheduled(cron = "0/3 2-30 20 * * ?")
    public void doJntTicketSnatch(){
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("DataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        List<DoSnatchInfo> doSnatchInfos = chnMuseumTicketServiceImpl.snatchingTicket();
        if(ObjectUtils.isEmpty(doSnatchInfos)){
            return;
        }
        int size = doSnatchInfos.size();
        if(size!=0){
            pool.setMaxPoolSize(size);
            pool.setCorePoolSize(size);
            pool.setQueueCapacity(size);
        }
        pool.initialize();
        for (DoSnatchInfo doSnatchInfo : doSnatchInfos) {
            CompletableFuture.runAsync(() -> chnMuseumTicketServiceImpl.doSnatchingTicket(doSnatchInfo), pool);
        }
    }
}
