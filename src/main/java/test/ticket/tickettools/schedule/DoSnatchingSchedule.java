package test.ticket.tickettools.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import test.ticket.tickettools.TaskExecutorConfig;
import test.ticket.tickettools.domain.bo.DoSnatchingInfo;
import test.ticket.tickettools.service.TicketService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Configuration
@EnableScheduling
public class DoSnatchingSchedule {

    @Resource
    TicketService ticketServiceImpl;
    @Resource
    TaskExecutorConfig taskExecutorConfig;


    @Scheduled(cron = "0/1 * 18-19 * * ?")
    public void doSnatching(){
        Map<String, DoSnatchingInfo> taskForRun = ticketServiceImpl.getTaskForRun();
        for (Map.Entry<String, DoSnatchingInfo> entity : taskForRun.entrySet()) {
            CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(entity.getValue()), taskExecutorConfig.getAsyncExecutor());
        }
    }
}
