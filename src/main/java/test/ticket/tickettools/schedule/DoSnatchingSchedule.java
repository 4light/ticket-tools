package test.ticket.tickettools.schedule;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.config.TaskExecutorConfig;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.bo.TaskInfo;
import test.ticket.tickettools.domain.bo.UpdateTaskDetailRequest;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.LoginService;
import test.ticket.tickettools.service.TicketService;
import test.ticket.tickettools.utils.DateUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableScheduling
public class DoSnatchingSchedule {

    private static String getCurrentUserUrl = "https://pcticket.cstm.org.cn/prod-api/getUserInfoToIndividual";
    private static String searchByOrderNoUrl = "https://pcticket.cstm.org.cn/prod-api/order/OrderInfo/updateSearchByOrderNo";
    private static String updateSearchByOrderNoUrl = "https://pcticket.cstm.org.cn/prod-api/order/OrderInfo/updateSearchByOrderNo";
    private static RestTemplate restTemplate = new RestTemplate();

    @Resource
    TicketService ticketServiceImpl;
    @Resource
    LoginService loginService;

    /**
     * 执行放票当天的任务
     */
    @Scheduled(cron = "0/1 0-30 18 * * ?")
    public void doSnatching() {
        Map<String, DoSnatchInfo> taskForRun = ticketServiceImpl.getTaskForRun();
        if (ObjectUtils.isEmpty(taskForRun)) {
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(taskForRun.size());
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        for (Map.Entry<String, DoSnatchInfo> entity : taskForRun.entrySet()) {
            executor.execute(() -> {
                ticketServiceImpl.snatchingTicket(entity.getValue());
            });
        }
        // 提交完所有任务后，关闭线程池
        executor.shutdown();
        // 等待所有任务执行完毕
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 去除放票当天的任务需要单个执行的任务
     */
    @Scheduled(cron = "0/1 0-30 18 * * ?")
    public void doSnatchingExcludeTarget() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        if (ObjectUtils.isEmpty(allTaskForRun)) {
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(allTaskForRun.size());
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            executor.execute(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo));
        }
        // 提交完所有任务后，关闭线程池
        executor.shutdown();
        // 等待所有任务执行完毕
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0/1 31-59 18 * * ?")
    public void doSingleSnatch() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        if (ObjectUtils.isEmpty(allTaskForRun)) {
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(allTaskForRun.size());
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            //CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo),pool);
            executor.execute(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo));
        }
        // 提交完所有任务后，关闭线程池
        executor.shutdown();
        // 等待所有任务执行完毕
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "0/1 * 8-17,19-22 * * ?")
    public void doSingleSnatchOtherTime() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        if (ObjectUtils.isEmpty(allTaskForRun)) {
            return;
        }
        // 创建固定大小的线程池，确保线程数和数据量一样
        ExecutorService executor = Executors.newFixedThreadPool(allTaskForRun.size());
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            //CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), pool);
            executor.execute(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo));
        }
        // 提交完所有任务后，关闭线程池
        executor.shutdown();
        // 等待所有任务执行完毕
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    //@Scheduled(cron = "* 0/1 * * * ?")
    public void updateOrderPayStatus() {
        List<TaskDetailEntity> taskDetailEntities = ticketServiceImpl.selectUnpaid();
        for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
            if (taskDetailEntity.getDone() && !taskDetailEntity.getPayment() && taskDetailEntity.getOrderNumber() != null) {
                ServiceResponse<TaskInfo> task = ticketServiceImpl.getTask(taskDetailEntity.getTaskId());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("authority", "pcticket.cstm.org.cn");
                headers.set("accept", "application/json");
                headers.set("authorization", task.getData().getAuth());
                headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
                headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
                JSONObject param = new JSONObject();
                param.put("docNo", taskDetailEntity.getOrderNumber());
                param.put("payType", 0);
                HttpEntity entity = new HttpEntity<>(param, headers);
                ResponseEntity<JSONObject> exchange = restTemplate.exchange(updateSearchByOrderNoUrl, HttpMethod.POST, entity, JSONObject.class);
                JSONObject body = exchange.getBody();
                if (!ObjectUtils.isEmpty(body)) {
                    if (body.getIntValue("code") == 200) {
                        JSONObject data = body.getJSONObject("data");
                        if (data.getIntValue("status") == 2) {
                            taskDetailEntity.setPayment(true);
                            Boolean res = ticketServiceImpl.updateTaskDetail(taskDetailEntity);
                            log.info("更新支付结果：{}", res);
                        }
                    }
                }
            }
        }
    }


    public void updateAuth() {
        List<TaskEntity> allUnDoneTask = ticketServiceImpl.getAllUnDoneTask();
        for (TaskEntity taskEntity : allUnDoneTask) {
            if (ObjectUtils.isEmpty(taskEntity.getAuth())) {
                String auth = loginService.longinCSTM(taskEntity.getAccount());
                if (checkAuth(auth)) {
                    taskEntity.setAuth(auth);
                    taskEntity.setUpdateDate(new Date());
                    ticketServiceImpl.updateTask(taskEntity);
                }
            } else {
                if (!checkAuth(taskEntity.getAuth())) {
                    while (true) {
                        String auth = loginService.longinCSTM(taskEntity.getAccount());
                        if (checkAuth(auth)) {
                            taskEntity.setAuth(auth);
                            taskEntity.setUpdateDate(new Date());
                            ticketServiceImpl.updateTask(taskEntity);
                            break;
                        }
                    }
                }
            }
        }
    }

    private Boolean checkAuth(String authorization) {
        if (StringUtils.isEmpty(authorization)) {
            return false;
        }
        restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {
            {
                setConnectTimeout(20000);
                setReadTimeout(20000);
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authority", "pcticket.cstm.org.cn");
        headers.set("accept", "application/json");
        headers.set("authorization", authorization);
        headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        HttpEntity entity = new HttpEntity<>(headers);
        ResponseEntity<JSONObject> getUserRes = restTemplate.exchange(getCurrentUserUrl, HttpMethod.GET, entity, JSONObject.class);
        JSONObject body = getUserRes.getBody();
        if (!ObjectUtils.isEmpty(body)) {
            if (body.getIntValue("code") == 200) {
                return true;
            }
        }
        return false;
    }
}
