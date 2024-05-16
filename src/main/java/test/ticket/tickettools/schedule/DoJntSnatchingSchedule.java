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
import test.ticket.tickettools.utils.DateUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Configuration
@EnableScheduling
public class DoJntSnatchingSchedule {
    @Resource
    JntTicketService jntTicketServiceImpl;


    @Scheduled(cron = "0/2 28-29 12 * * ?")
    public void initData(){
        try {
            jntTicketServiceImpl.initData();
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
            CompletableFuture.runAsync(() -> jntTicketServiceImpl.doSnatchingJnt(doSnatchInfo),pool);
        }
    }

}
