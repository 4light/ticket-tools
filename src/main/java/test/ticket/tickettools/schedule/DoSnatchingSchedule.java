package test.ticket.tickettools.schedule;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import test.ticket.tickettools.dao.AccountInfoDao;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.bo.ProxyInfo;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.bo.TaskInfo;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.constant.RedisKeyEnum;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.AccountService;
import test.ticket.tickettools.service.LoginService;
import test.ticket.tickettools.service.RedisService;
import test.ticket.tickettools.service.TicketService;
import test.ticket.tickettools.utils.DateUtils;
import test.ticket.tickettools.utils.ProxyUtil;
import test.ticket.tickettools.utils.ScreenshotUtil;
import test.ticket.tickettools.utils.TemplateUtil;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableScheduling
public class DoSnatchingSchedule {

    private static String getCurrentUserUrl = "https://pcticket.cstm.org.cn/prod-api/getUserInfoToIndividual";
    private static String searchByOrderNoUrl = "https://pcticket.cstm.org.cn/prod-api/order/OrderInfo/updateSearchByOrderNo";
    private static String searchPersonOrderUrl = "https://pcticket.cstm.org.cn/prod-api/order/OrderInfo/searchPersonOrder/";
    private static RestTemplate restTemplate = new RestTemplate();
    private static ExecutorService queryExecutor = Executors.newFixedThreadPool(5);

    @Resource
    TicketService ticketServiceImpl;
    @Resource
    TaskDao taskDao;
    @Resource
    AccountInfoDao accountInfoDao;
    @Resource
    LoginService loginService;
    @Resource
    TaskExecutorConfig taskExecutorConfig;
    @Resource
    RedisService redisService;

    /**
     * 执行放票当天的任务
     */
    @Scheduled(cron = "0/10 0-10 18 * * ?")
    public void doSnatching() {
        List<DoSnatchInfo> taskForRun = ticketServiceImpl.getTaskForRun();
        if (ObjectUtils.isEmpty(taskForRun)) {
            return;
        }
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("CSTMDataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = taskForRun.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        List<CompletableFuture<Void>> futures = taskForRun.stream()
                .map(doSnatchInfo -> CompletableFuture.runAsync(
                        () -> ticketServiceImpl.snatchingTicket(doSnatchInfo),
                        pool
                ))
                .collect(Collectors.toList());
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        // 使用CompletableFuture.allOf等待所有抓票操作完成
        /*CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.thenRun(() -> log.info("日期{}下批次任务执行完成: ", useDate));

        Map<Date, List<DoSnatchInfo>> mapByUseDate = taskForRun.stream()
                .collect(Collectors.groupingBy(DoSnatchInfo::getUseDate));

        // 对每个useDate异步检查并处理
        mapByUseDate.forEach((useDate, doSnatchInfos) -> {
            CompletableFuture.supplyAsync(() -> haveTicket(doSnatchInfos.get(0).getAuthorization(), useDate), taskExecutorConfig.getAsyncExecutor())
                    .thenAccept(hasTicket -> {
                        if (hasTicket) {
                            // 异步执行抓票操作，并收集所有CompletableFuture
                            List<CompletableFuture<Void>> futures = doSnatchInfos.stream()
                                    .map(doSnatchInfo -> CompletableFuture.runAsync(
                                            () -> ticketServiceImpl.snatchingTicket(doSnatchInfo),
                                            taskExecutorConfig.getAsyncExecutor()
                                    ))
                                    .collect(Collectors.toList());

                            // 使用CompletableFuture.allOf等待所有抓票操作完成
                            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                            allOf.thenRun(() -> log.info("日期{}下批次任务执行完成: ", useDate));
                        }
                    });
        });*/
    }

    /**
     * 去除放票当天的任务需要单个执行的任务
     */
    //@Scheduled(cron = "0/1 0-10 18 * * ?")
    public void doSnatchingExcludeTarget() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        if (ObjectUtils.isEmpty(allTaskForRun)) {
            return;
        }
        LocalDate localDate = LocalDate.now().plusDays(7L);
        Date date = DateUtils.localDateToDate(localDate);
        allTaskForRun = allTaskForRun.stream().filter(o -> !ObjectUtils.nullSafeEquals(date, o.getUseDate())).collect(Collectors.toList());
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("CSTMNormalDataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = allTaskForRun.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        List<CompletableFuture<Void>> futures = allTaskForRun.stream()
                .map(doSnatchInfo -> CompletableFuture.runAsync(
                        () -> {
                            if(haveTicket(doSnatchInfo.getAuthorization(),doSnatchInfo.getUseDate())) {
                                ticketServiceImpl.snatchingTicket(doSnatchInfo);
                            }
                        },
                        pool
                ))
                .collect(Collectors.toList());
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        /*Map<Date, List<DoSnatchInfo>> mapByUseDate = allTaskForRun.stream()
                .collect(Collectors.groupingBy(DoSnatchInfo::getUseDate));
        mapByUseDate.forEach((useDate, doSnatchInfos) -> {
            CompletableFuture.supplyAsync(() -> haveTicket(doSnatchInfos.get(0).getAuthorization(), useDate), taskExecutorConfig.getAsyncExecutor())
                    .thenAccept(hasTicket -> {
                        if (hasTicket) {
                            // 异步执行抓票操作，并收集所有CompletableFuture
                            List<CompletableFuture<Void>> futures = doSnatchInfos.stream()
                                    .map(doSnatchInfo -> CompletableFuture.runAsync(
                                            () -> ticketServiceImpl.snatchingTicket(doSnatchInfo),
                                            taskExecutorConfig.getAsyncExecutor()
                                    ))
                                    .collect(Collectors.toList());

                            // 使用CompletableFuture.allOf等待所有抓票操作完成
                            //CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                            //allOf.thenRun(() -> log.info("日期{}下批次任务执行完成: ", useDate));
                        }
                    });
        });*/
    }

    //@Scheduled(cron = "0/1 06-59 18 * * ?")
    public void doSingleSnatch() {
        runNormalTask();
    }

    //@Scheduled(cron = "0/3 * 7-17 * * ?")
    public void doSingleSnatchOtherTime() {
        runNormalTask();
    }

    //@Scheduled(cron = "0/1 * 0-6,19-23 * * ?")
    public void doSingleSnatchOtherTime2() {
        runNormalTask();
    }

    @Scheduled(cron = "0/20 * * * * ?")
    public void updateOrderPayStatus() {
        try {
            RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
            List<TaskDetailEntity> taskDetailEntities = ticketServiceImpl.selectUnpaid();
            Map<Long, List<TaskDetailEntity>> taskDetailMap = taskDetailEntities.stream()
                    .collect(Collectors.groupingBy(TaskDetailEntity::getTaskId));
            for (Map.Entry<Long, List<TaskDetailEntity>> entry : taskDetailMap.entrySet()) {
                Long taskId = entry.getKey();
                List<TaskDetailEntity> taskDetailEntityList = entry.getValue();
                TaskEntity task = taskDao.selectByPrimaryKey(taskId);
                if(task.getChannel()!=ChannelEnum.CSTM.getCode()){
                    continue;
                }
                AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(task.getUserInfoId());
                Map<Long, List<TaskDetailEntity>> orderIdTaskDetailMap = taskDetailEntityList.stream()
                        .collect(Collectors.groupingBy(TaskDetailEntity::getOrderId));
                for (Map.Entry<Long, List<TaskDetailEntity>> taskDetailEntry : orderIdTaskDetailMap.entrySet()) {
                    Long orderId = taskDetailEntry.getKey();
                    CompletableFuture.runAsync(()->{
                        String headers = accountInfoEntity.getHeaders();
                        ScreenshotUtil.takeScreenshot(String.valueOf(orderId),headers.split(" ")[1],"task"+taskId+"-"+UUID.randomUUID());
                    });
                    List<TaskDetailEntity> value = taskDetailEntry.getValue();
                    HttpEntity entity = new HttpEntity<>(getHeader(accountInfoEntity.getHeaders(), orderId));
                    JSONObject response = TemplateUtil.getResponse(restTemplate, searchPersonOrderUrl + orderId, HttpMethod.GET, entity);
                    if (!ObjectUtils.isEmpty(response) && response.getIntValue("code") == 200) {
                        JSONObject data = response.getJSONObject("data");
                        JSONArray tickets = data.getJSONArray("tickets").getJSONArray(0);
                        Map<String, JSONObject> map = new HashMap<>();
                        for (int i = 0; i < tickets.size(); i++) {
                            JSONObject item = tickets.getJSONObject(i);
                            map.put(item.getString("certificateInfo"), item);
                        }
                        if (data.getIntValue("status") == 2) {
                            value.forEach(o -> {
                                o.setPayment(true);
                                o.setOrderNumber(map.get(o.getIDCard()).getString("ticketNumber"));
                                Boolean res = ticketServiceImpl.updateTaskDetail(o);
                                log.info("更新支付结果：{}", res);
                            });
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   //@Scheduled(cron = "* 0/6 * * * ?")
    public void updateTaskProxy() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUseDate(DateUtils.localDateToDate(LocalDate.now()));
        taskEntity.setChannel(ChannelEnum.CSTM.getCode());
        List<TaskEntity> allUnDoneTasks = taskDao.getAllUnDoneTasks(taskEntity);
        List<ProxyInfo> proxyList = ProxyUtil.getProxyList(allUnDoneTasks.size());
        for (int i = 0; i < allUnDoneTasks.size(); i++) {
            TaskEntity currentEntity = allUnDoneTasks.get(i);
            ProxyInfo proxyInfo = proxyList.get(i);
            currentEntity.setIp(proxyInfo.getIp());
            currentEntity.setPort(proxyInfo.getPort());
            taskDao.updateTask(currentEntity);
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

    private HttpHeaders getHeader(String auth, Long orderId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");
        headers.set("authorization", auth);
        headers.set("Referer", "https://pcticket.cstm.org.cn/personal/order_detail?orderId=" + orderId);
        headers.set("Sec-Ch-Ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"\n");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        return headers;
    }


    private void runNormalTask() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        if (ObjectUtils.isEmpty(allTaskForRun)) {
            return;
        }
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            if(haveTicket(doSnatchInfo.getAuthorization(),doSnatchInfo.getUseDate())) {
                ticketServiceImpl.snatchingTicket(doSnatchInfo);
            }
        }
        /*ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("CSTMNormalDataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = allTaskForRun.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        List<CompletableFuture<Void>> futures = allTaskForRun.stream()
                .map(doSnatchInfo -> CompletableFuture.runAsync(
                        () -> {
                            if(haveTicket(doSnatchInfo.getAuthorization(),doSnatchInfo.getUseDate())) {
                                ticketServiceImpl.snatchingTicket(doSnatchInfo);
                            }
                        },
                        pool
                ))
                .collect(Collectors.toList());
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));*/
        /*Map<Date, List<DoSnatchInfo>> mapByUseDate = allTaskForRun.stream()
                .collect(Collectors.groupingBy(DoSnatchInfo::getUseDate));
        // 对每个useDate异步检查并处理
        mapByUseDate.forEach((useDate, doSnatchInfos) -> {
            CompletableFuture.supplyAsync(() -> haveTicket(doSnatchInfos.get(0).getAuthorization(),useDate), taskExecutorConfig.getAsyncExecutor())
                    .thenAccept(hasTicket -> {
                        if (hasTicket) {
                            doSnatchInfos.stream()
                                    .map(doSnatchInfo -> CompletableFuture.runAsync(
                                            () -> ticketServiceImpl.snatchingTicket(doSnatchInfo),
                                            taskExecutorConfig.getAsyncExecutor()
                                    ))
                                    .collect(Collectors.toList());
                        }
                    });
        });*/
    }

    private Boolean haveTicket(String auth,Date date) {
        try {
            String url = "https://pcticket.cstm.org.cn/prod-api/pool/getScheduleByHallId?hallId=1&openPerson=1&queryDate=%s&saleMode=1&single=true";
            String format = String.format(url, DateUtils.dateToStr(date, "yyyy/MM/dd"));
            HttpResponse response = HttpUtil.createGet(format)
                    .header("Authorization",auth)
                    .header("Connection","keep-alive")
                    .header("Referer","https://pcticket.cstm.org.cn/")
                    .header("Accept","application/json")
                    .header("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36/")
                    .timeout(2000)
                    .execute();
            String body = response.body();
            if (ObjectUtils.isEmpty(body)) {
                return false;
            }
            JSONObject responseJson = JSON.parseObject(body);
            JSONArray data = responseJson.getJSONArray("data");
            if (ObjectUtils.isEmpty(data)) {
                return false;
            }
            log.info("{}日期下余票{}",date,data.getJSONObject(0).getIntValue("ticketPool"));
            return data.getJSONObject(0).getIntValue("ticketPool")>0;
        } catch (Exception e) {

        }
        return false;
    }

}
