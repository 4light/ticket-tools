package test.ticket.tickettools.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.config.TaskExecutorConfig;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.service.JntTicketService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableScheduling
public class DoJntSnatchingSchedule {
    @Resource
    JntTicketService jntTicketServiceImpl;


    //@Scheduled(cron = "0 26 21 * * ?")
    public void initData(){
        jntTicketServiceImpl.initData();
    }

    //@Scheduled(cron = "0/15 0-59 21 * * ?")
    public void doJntTicketSnatch(){
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("DataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        List<DoSnatchInfo> doSnatchInfos = jntTicketServiceImpl.getDoSnatchInfos();
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
            CompletableFuture.runAsync(() -> jntTicketServiceImpl.doSnatchingJnt(doSnatchInfo), pool);
        }
    }
}
