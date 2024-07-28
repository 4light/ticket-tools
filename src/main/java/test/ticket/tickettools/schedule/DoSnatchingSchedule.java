package test.ticket.tickettools.schedule;


import cn.hutool.core.util.RandomUtil;
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
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.TicketService;
import test.ticket.tickettools.utils.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableScheduling
public class DoSnatchingSchedule {

    private static String getBlockUrl = "https://pcticket.cstm.org.cn/prod-api/pool/getBlock";
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

    /**
     * 执行放票当天的任务
     */
    @Scheduled(cron = "0/2 0-3 18 * * ?")
    public void doSnatching() {
        doSnatchingBatch();
    }
    @Scheduled(cron = "0/2 15-17 18 * * ?")
    public void doSnatching2() {
        doSnatchingBatch();
    }
    private void doSnatchingBatch(){
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
        allOf.thenRun(() -> log.info("放票日批次任务执行完成"));
    }

    /**
     * 去除放票当天的任务需要单个执行的任务
     */
    @Scheduled(cron = "* 0-18 18 * * ?")
    public void doSnatchingExcludeTarget() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        if (ObjectUtils.isEmpty(allTaskForRun)) {
            return;
        }
        LocalDate localDate = LocalDate.now().plusDays(7L);
        Date date = DateUtils.localDateToDate(localDate);
        allTaskForRun = allTaskForRun.stream().filter(o -> !ObjectUtils.nullSafeEquals(date, o.getUseDate())).collect(Collectors.toList());
        Map<Long, List<DoSnatchInfo>> mapByUseDate = allTaskForRun.stream()
                .collect(Collectors.groupingBy(DoSnatchInfo::getTaskId));
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("CSTMNormalDataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = mapByUseDate.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(size);
        pool.initialize();
        AtomicBoolean isTicketSnatched = new AtomicBoolean(false);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        // 对每个 useDate 异步检查并处理
        mapByUseDate.forEach((useDate, doSnatchInfos) -> {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (haveTicket(doSnatchInfos.get(0))) {
                    if (isTicketSnatched.compareAndSet(false, true)) {
                        ticketServiceImpl.snatchingTicket(doSnatchInfos.get(0));
                    }
                }
            }, pool);
            futures.add(future);
        });
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 关闭线程池
        pool.shutdown();
    }

    @Scheduled(cron = "* 18-59 18 * * ?")
    public void doSingleSnatch() {
        runNormalTask();
    }

    @Scheduled(cron = "* * 0-17,19-23 * * ?")
    public void doSingleSnatchOtherTime() {
        runNormalTask();
    }

    /*@Scheduled(cron = "* * 0-17,19-23 * * ?")
    public void doSingleSnatchOtherTime2() {
        runNormalTask();
    }*/

    @Scheduled(fixedDelay = 20000)
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
                if (task.getChannel() != ChannelEnum.CSTM.getCode()) {
                    continue;
                }
                AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(task.getUserInfoId());
                Map<Long, List<TaskDetailEntity>> orderIdTaskDetailMap = taskDetailEntityList.stream()
                        .collect(Collectors.groupingBy(TaskDetailEntity::getOrderId));
                for (Map.Entry<Long, List<TaskDetailEntity>> taskDetailEntry : orderIdTaskDetailMap.entrySet()) {
                    Long orderId = taskDetailEntry.getKey();
                    List<TaskDetailEntity> value = taskDetailEntry.getValue();
                    HttpEntity entity = new HttpEntity<>(getHeader(ObjectUtils.isEmpty(accountInfoEntity)?value.get(0).getOrderCreatorAuth():accountInfoEntity.getHeaders(), orderId));
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
                            CompletableFuture.runAsync(() -> {
                                String auth = ObjectUtils.isEmpty(accountInfoEntity)?value.get(0).getOrderCreatorAuth():accountInfoEntity.getHeaders();
                                ScreenshotUtil.takeScreenshot(String.valueOf(orderId), auth, "task" + task.getId() + "-" + UUID.randomUUID());
                            });
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

    //@Scheduled(cron = "* 0/1 * * * ?")
    public void doUpdate() {
        List<TaskEntity> allUnDoneTask = ticketServiceImpl.getAllUnDoneTask();
        for (TaskEntity taskEntity : allUnDoneTask) {
            Long userInfoId = taskEntity.getUserInfoId();
            if(ObjectUtils.isEmpty(userInfoId)){
                updateAccountAndAuth(taskEntity);
            }else{
                AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
                if(!checkAuth(accountInfoEntity.getHeaders())){
                    updateAccountAndAuth(taskEntity);
                }
            }
        }
    }
   @Scheduled(fixedDelay = 60000)
    public void doUpdateAccountPool() {
        /*LocalDateTime now=LocalDateTime.now();
        int hour = now.getHour();
        if(hour>=2&&hour<=5){
            return;
        }*/
        log.info("开始更新账号池");
        int accountNum=15;
        AccountInfoEntity query=new AccountInfoEntity();
        query.setCreator("system");
        query.setYn(false);
        query.setChannel(ChannelEnum.CSTM.getCode());
        List<AccountInfoEntity> accountInfoEntityList = accountInfoDao.selectByEntity(query);
        int currentNum=0;
        if(!ObjectUtils.isEmpty(accountInfoEntityList)){
            for (AccountInfoEntity accountInfoEntity : accountInfoEntityList) {
                if(ObjectUtils.isEmpty(accountInfoEntity.getHeaders())||!checkAuth(accountInfoEntity.getHeaders())){
                    accountInfoDao.del(accountInfoEntity.getId());
                    currentNum++;
                }
            }
            if(accountInfoEntityList.size()-currentNum<accountNum){
                for (int i = 0; i < accountNum-accountInfoEntityList.size()-currentNum; i++) {
                    String phoneNo =insertAccount();
                    if(ObjectUtils.isEmpty(phoneNo)){
                        log.info("获取账号失败或账号池数据插入失败");
                        return;
                    }
                    ticketServiceImpl.updateAuth(phoneNo);
                }
            }
        }else{
            for (int i = 0; i < accountNum; i++) {
                String phoneNo =insertAccount();
                if(ObjectUtils.isEmpty(phoneNo)){
                    log.info("账号池数据插入失败");
                    return;
                }
                ticketServiceImpl.updateAuth(phoneNo);
            }
        }
        log.info("更新账号池结束");
    }

    public void updateAccountAndAuth(TaskEntity taskEntity){
        String phoneNo = VirtualPhoneUtil.getPhoneNo();
        AccountInfoEntity account = new AccountInfoEntity();
        account.setUserName("三方号" + phoneNo);
        account.setAccount(phoneNo);
        account.setChannel(ChannelEnum.CSTM.getCode());
        account.setCreator(taskEntity.getCreator());
        account.setYn(false);
        account.setStatus(false);
        account.setCreateDate(new Date());
        Integer integer = accountInfoDao.insertOrUpdate(account);
        if(integer>0) {
            ticketServiceImpl.updateAuth(phoneNo);
            taskEntity.setAccount(phoneNo);
            taskEntity.setUserInfoId(account.getId());
            Integer res = taskDao.updateTask(taskEntity);
            log.info("更新任务结果：{}",res>0);
        }else{
            log.info("更新任务购票账号异常");
        }
    }

    private Boolean checkAuth(String authorization) {
        if (StringUtils.isEmpty(authorization)) {
            return false;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");
        headers.set("authorization", authorization);
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
        headers.set("Referer", "https://pcticket.cstm.org.cn/personal/check_info?name=%E4%B8%BB%E5%B1%95%E5%8E%85");
        headers.set("Sec-Ch-Ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"");
        headers.set("Sec-Ch-Ua-Mobile", "?0");
        headers.set("Sec-Ch-Ua-Platform", "\"macOS\"");
        headers.set("Sec-Fetch-Dest", "empty");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        HttpEntity entity = new HttpEntity<>(headers);
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(RandomUtil.randomInt(2000,5000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ResponseEntity<JSONObject> getUserRes= TemplateUtil.kuaiDaiLiTemp().exchange(getBlockUrl, HttpMethod.GET, entity, JSONObject.class);
            if(ObjectUtils.isEmpty(getUserRes)){
                return false;
            }
            JSONObject body = getUserRes.getBody();
            if (!ObjectUtils.isEmpty(body)) {
                if (body.getIntValue("code") == 200) {
                    return true;
                }
            }
            log.info("check异常返回:{}",body);
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
        Map<Long, List<DoSnatchInfo>> mapByUseDate = allTaskForRun.stream()
                .collect(Collectors.groupingBy(DoSnatchInfo::getTaskId));
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setThreadNamePrefix("CSTMNormalDataProcessor-");
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        int size = mapByUseDate.size();
        pool.setMaxPoolSize(size);
        pool.setCorePoolSize(size);
        pool.setQueueCapacity(100);
        pool.initialize();
        // 对每个 useDate 异步检查并处理
        mapByUseDate.forEach((useDate, doSnatchInfos) -> {
            CompletableFuture.runAsync(() -> {
                if (haveTicket(doSnatchInfos.get(0))) {
                    ticketServiceImpl.snatchingTicket(doSnatchInfos.get(0));
                }
            }, pool);
        });
        pool.shutdown();
    }

    private Boolean haveTicket(DoSnatchInfo doSnatchInfo) {
        try {
            String url = "https://pcticket.cstm.org.cn/prod-api/pool/ingore/getHall?saleMode=1&openPerson=1&queryDate=%s";
            String format = String.format(url, DateUtils.dateToStr(doSnatchInfo.getUseDate(), "yyyy/MM/dd"));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authority", "pcticket.cstm.org.cn");
            headers.set("accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            HttpEntity entity=new HttpEntity(headers);
            ResponseEntity<JSONObject> exchange = TemplateUtil.initSSLTemplate().exchange(format, HttpMethod.GET, entity, JSONObject.class);
            /*HttpResponse response = HttpUtil.createGet(format)
                    .header("Connection", "keep-alive")
                    .header("Referer", "https://pcticket.cstm.org.cn/")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36/")
                    .timeout(2000)
                    .execute();*/
            JSONObject responseJson = exchange.getBody();
            if (ObjectUtils.isEmpty(responseJson)) {
                return false;
            }
            //JSONObject responseJson = JSON.parseObject(body);
            JSONArray data = responseJson.getJSONArray("data");
            if (ObjectUtils.isEmpty(data)) {
                return false;
            }
            if(data.getJSONObject(0).getIntValue("ticketPool") > 0){
                log.info("{}日期下余票{}", doSnatchInfo.getUseDate(), data.getJSONObject(0).getIntValue("ticketPool"));
                return true;
            }else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    private String insertAccount(){
        String phoneNo = VirtualPhoneUtil.getPhoneNo();
        if(ObjectUtils.isEmpty(phoneNo)){
            return null;
        }
        AccountInfoEntity account = new AccountInfoEntity();
        account.setUserName("三方号" + phoneNo);
        account.setAccount(phoneNo);
        account.setChannel(ChannelEnum.CSTM.getCode());
        account.setCreator("system");
        account.setYn(false);
        account.setStatus(false);
        account.setCreateDate(new Date());
        if(accountInfoDao.insertOrUpdate(account)>0){
            return phoneNo;
        }
        return null;
    }

}
