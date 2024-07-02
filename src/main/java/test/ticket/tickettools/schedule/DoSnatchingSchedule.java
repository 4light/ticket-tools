package test.ticket.tickettools.schedule;

import cn.hutool.core.date.DateUtil;
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
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.bo.TaskInfo;
import test.ticket.tickettools.domain.constant.RedisKeyEnum;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.AccountService;
import test.ticket.tickettools.service.LoginService;
import test.ticket.tickettools.service.RedisService;
import test.ticket.tickettools.service.TicketService;
import test.ticket.tickettools.utils.DateUtils;
import test.ticket.tickettools.utils.TemplateUtil;

import javax.annotation.Resource;
import java.time.LocalDate;
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
    @Scheduled(cron = "0/1 0-10 18 * * ?")
    public void doSnatching() {
        LocalDate localDate = LocalDate.now();
        String getHallUrl = "https://pcticket.cstm.org.cn/prod-api/pool/ingore/getHall?saleMode=1&openPerson=1&queryDate=%s";
        String formatGetHallUrl = String.format(getHallUrl, DateUtil.format(DateUtils.localDateToDate(localDate.plusDays(7L)), "yyyy/MM/dd"));
        HttpResponse response = HttpUtil.createGet(formatGetHallUrl).execute();
        String body = response.body();
        if (ObjectUtils.isEmpty(body)) {
            return;
        }
        JSONObject bodyJson = JSON.parseObject(body);
        JSONArray data = bodyJson.getJSONArray("data");
        boolean haveTicket = false;
        for (int i = 0; i < data.size(); i++) {
            if (data.getJSONObject(i).getIntValue("hallId") == 1) {
                if (data.getJSONObject(i).getIntValue("ticketPool") > 0) {
                    haveTicket = true;
                    break;
                }
            }
        }
        if (haveTicket) {
            List<DoSnatchInfo> taskForRun = ticketServiceImpl.getTaskForRun();
            if (ObjectUtils.isEmpty(taskForRun)) {
                return;
            }
            for (DoSnatchInfo doSnatchInfo : taskForRun) {
                CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), taskExecutorConfig.getAsyncExecutor());
            }
        }
    }

    /**
     * 去除放票当天的任务需要单个执行的任务
     */
    @Scheduled(cron = "0/2 0-10 18 * * ?")
    public void doSnatchingExcludeTarget() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        LocalDate localDate = LocalDate.now().plusDays(7L);
        Date date = DateUtils.localDateToDate(localDate);
        allTaskForRun = allTaskForRun.stream().filter(o -> !date.equals(o.getUseDate())).collect(Collectors.toList());
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), taskExecutorConfig.getAsyncExecutor());
        }
    }

    @Scheduled(cron = "0/2 31-59 18 * * ?")
    public void doSingleSnatch() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), taskExecutorConfig.getAsyncExecutor());
        }

    }

    @Scheduled(cron = "0/2 * 7-17 * * ?")
    public void doSingleSnatchOtherTime() {

                List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
                for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
                    CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), taskExecutorConfig.getAsyncExecutor());
                }

    }

    @Scheduled(cron = "0/2 * 0-6,19-23 * * ?")
    public void doSingleSnatchOtherTime2() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), taskExecutorConfig.getAsyncExecutor());
        }

    }

    @Scheduled(cron = "0/30 * * * * ?")
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
                AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(task.getUserInfoId());
                Map<Long, List<TaskDetailEntity>> orderIdTaskDetailMap = taskDetailEntityList.stream()
                        .collect(Collectors.groupingBy(TaskDetailEntity::getOrderId));
                for (Map.Entry<Long, List<TaskDetailEntity>> taskDetailEntry : orderIdTaskDetailMap.entrySet()) {
                    Long orderId = taskDetailEntry.getKey();
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

    private void runNormal() {
        List<String> keys = redisService.getList(RedisKeyEnum.NORMAL.getCode());
        if (ObjectUtils.isEmpty(keys)) {
            return;
        }
        List<String> useDateList = redisService.getList(RedisKeyEnum.USEDATE.getCode());
        if (ObjectUtils.isEmpty(useDateList)) {
            return;
        }
        log.info("余票日期:{}", useDateList);
        List<DoSnatchInfo> allTaskForRun = new ArrayList<>();
        for (String key : keys) {
            if (ObjectUtils.isEmpty(key)) {
                continue;
            }
            List<String> value = redisService.getList(key);
            if (ObjectUtils.isEmpty(value)) {
                continue;
            }
            List<DoSnatchInfo> list = value.stream().map(o -> JSON.parseObject(o, DoSnatchInfo.class)).collect(Collectors.toList());
            for (DoSnatchInfo doSnatchInfo : list) {
                Date useDate = doSnatchInfo.getUseDate();
                String dateStr = DateUtils.dateToStr(useDate, "yyyy-MM-dd");
                if (useDateList.contains(dateStr)) {
                    allTaskForRun.add(doSnatchInfo);
                }
            }
        }
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), taskExecutorConfig.getAsyncExecutor());
        }
    }

    //@Scheduled(cron = "0/1 * * * * ?")
    static class QueryRes implements Callable<Boolean> {
        @Override
        public Boolean call() {
            HttpResponse response = HttpUtil.createGet("https://pcticket.cstm.org.cn/prod-api/pool/ingore/getCalendar?saleMode=1&openPerson=1").execute();
            String body = response.body();
            if (ObjectUtils.isEmpty(body)) {
                return false;
            }
            log.info("查询到结果");
            JSONObject bodyJson = JSON.parseObject(body);
            JSONArray data = bodyJson.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                JSONArray hallTicketPoolVOS = item.getJSONArray("hallTicketPoolVOS");
                if (ObjectUtils.isEmpty(hallTicketPoolVOS)) {
                    continue;
                }
                JSONObject hallTicketPoolVO = hallTicketPoolVOS.getJSONObject(0);
                if (!ObjectUtils.isEmpty(hallTicketPoolVO) && !hallTicketPoolVO.getString("closeContent").contains("暂无余票")) {
                    return true;
                }
            }
            return false;
        }
    }
}
