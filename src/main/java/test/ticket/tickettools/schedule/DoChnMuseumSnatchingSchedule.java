package test.ticket.tickettools.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.bo.ProxyInfo;
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
public class DoChnMuseumSnatchingSchedule {
    @Resource
    DoSnatchTicketService chnMuseumTicketServiceImpl;

    @Scheduled(cron = "0 0/4 16-17 * * ?")
    public void initData() {
        LocalDateTime localDateTime=LocalDateTime.now();
        if(localDateTime.getHour()>17&&localDateTime.getMinute()>30){
            return;
        }
        chnMuseumTicketServiceImpl.initData(null);
    }

    @Scheduled(cron = "0/1 01-30 17 * * ?")
    public void doPalaceMuseumTicketSnatch() {
        List<DoSnatchInfo> doSnatchInfos = chnMuseumTicketServiceImpl.getDoSnatchInfos();
        if (ObjectUtils.isEmpty(doSnatchInfos)) {
            return;
        }
        int size = doSnatchInfos.size();
        List<ProxyInfo> proxyList = ProxyUtil.getProxyList(size);
        List<DoSnatchInfo> newDoSnatchInfos=new ArrayList<>();
        for (int i = 0; i < doSnatchInfos.size(); i++) {
            DoSnatchInfo doSnatchInfo = doSnatchInfos.get(i);
            ProxyInfo proxyInfo = proxyList.get(i);
            doSnatchInfo.setIp(proxyInfo.getIp());
            doSnatchInfo.setPort(proxyInfo.getPort());
            newDoSnatchInfos.add(doSnatchInfo);
        }
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("chnMuseumProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        for (DoSnatchInfo doSnatchInfo : newDoSnatchInfos) {
            CompletableFuture.runAsync(() -> chnMuseumTicketServiceImpl.doSnatchingTicket(doSnatchInfo), pool);
        }
    }
}
