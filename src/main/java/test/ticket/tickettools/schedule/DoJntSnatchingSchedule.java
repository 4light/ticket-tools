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
import java.util.concurrent.*;

@Slf4j
@Configuration
@EnableScheduling
public class DoJntSnatchingSchedule {
    @Resource
    DoSnatchTicketService jntTicketServiceImpl;


    @Scheduled(cron = "0/2 28-29 12 * * ?")
    public void initData(){
        try {
            jntTicketServiceImpl.initData(null);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0/2 30-35 12 * * ?")
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
