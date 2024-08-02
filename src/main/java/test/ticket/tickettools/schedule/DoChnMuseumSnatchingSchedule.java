package test.ticket.tickettools.schedule;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.bo.ProxyInfo;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.DoSnatchTicketService;
import test.ticket.tickettools.utils.DateUtils;
import test.ticket.tickettools.utils.ProxyUtil;
import test.ticket.tickettools.utils.TemplateUtil;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static test.ticket.tickettools.utils.EncDecUtil.doAES;

@Slf4j
@Configuration
@EnableScheduling
public class DoChnMuseumSnatchingSchedule {
    @Resource
    DoSnatchTicketService chnMuseumTicketServiceImpl;
    @Resource
    TaskDao taskDao;

    //@Scheduled(cron = "* 0/4 16-17 * * ?")
    public void initData() {
        LocalDateTime localDateTime=LocalDateTime.now();
        if(localDateTime.getHour()>17&&localDateTime.getMinute()>30){
            return;
        }
        chnMuseumTicketServiceImpl.initData(null);
    }

    //@Scheduled(cron = "0/1 0-3 17 * * ?")
    public void doPalaceMuseumTicketSnatch() {
        List<DoSnatchInfo> doSnatchInfos = chnMuseumTicketServiceImpl.getDoSnatchInfos();
        if (ObjectUtils.isEmpty(doSnatchInfos)) {
            return;
        }
        int size = doSnatchInfos.size();
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("chnMuseumProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        doSnatchInfos.forEach(o->{
            CompletableFuture.runAsync(() -> chnMuseumTicketServiceImpl.doSnatchingTicket(o), pool);
        });
    }


    private void doLoginChnMu(){
        String checkTime=getCheckTime();
        String subCheckTime = checkTime.substring(13, checkTime.length() - 1);
        /*JSONObject checkJson = JSON.parseObject(subCheckTime);
        log.info("getCheckTime:{}", checkTime);
        String data = doSnatchInfo.getChannelUserId() + ":" + checkTime.substring(26, 36) + "000" + ":" + DateUtils.dateToStr(doSnatchInfo.getUseDate(), "yyyy/MM/dd") + ":" + hallId + ":" + hallScheduleId + ":2";
        String nonce = doAES(data, "AyrKJRXPO3nR5Abc");
        String formatUrl = String.format(getBlockUrl, nonce);
        log.info("formatUrl:{}", formatUrl);
        HttpEntity getBlockEntity = new HttpEntity(headers);
        log.info("header:{}", headers);
        JSONObject getBlockRes = TemplateUtil.getResponse(restTemplate, formatUrl, HttpMethod.GET, getBlockEntity);
        if (ObjectUtils.isEmpty(getBlockRes) || !StrUtil.equals("操作成功", getBlockRes.getString("msg"))) {
            log.info("获取验证码失败:{}", getBlockRes);
            runTaskCache.remove(taskId);
            return;
        }*/
    }

    public static String getCheckTime() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Host", "vv.video.qq.com");
        httpHeaders.set("Accept", "*/*");
        httpHeaders.set("Connection", "keep-alive");
        httpHeaders.set("Accept-Encoding", "gzip;q=1.0, compress;q=0.5");
        httpHeaders.set("Accept-Language", "zh-CN,zh;q=0.9");
        HttpEntity httpEntity = new HttpEntity(httpHeaders);
        ResponseEntity getCheckTimeRes = TemplateUtil.kuaiDaiLiTemp().exchange("http://vv.video.qq.com/checktime?otype=json", HttpMethod.GET, httpEntity, String.class);
        return getCheckTimeRes.getBody().toString();
    }
}
