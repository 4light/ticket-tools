package test.ticket.tickettools.schedule;

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
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.AccountService;
import test.ticket.tickettools.service.LoginService;
import test.ticket.tickettools.service.TicketService;
import test.ticket.tickettools.utils.DateUtils;
import test.ticket.tickettools.utils.TemplateUtil;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * 执行放票当天的任务
     */
    @Scheduled(cron = "0/1 0-30 18 * * ?")
    public void doSnatching() {
        Map<String, DoSnatchInfo> taskForRun = ticketServiceImpl.getTaskForRun();
        if(ObjectUtils.isEmpty(taskForRun)){
            return;
        }
        for (Map.Entry<String, DoSnatchInfo> entity : taskForRun.entrySet()) {
            CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(entity.getValue()), taskExecutorConfig.getAsyncExecutor());
        }
    }

    /**
     * 去除放票当天的任务需要单个执行的任务
     */
    @Scheduled(cron = "0/1 0-30 18 * * ?")
    public void doSnatchingExcludeTarget() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        LocalDate localDate=LocalDate.now().plusDays(7L);
        Date date = DateUtils.localDateToDate(localDate);
        allTaskForRun=allTaskForRun.stream().filter(o->!date.equals(o.getUseDate())).collect(Collectors.toList());
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), taskExecutorConfig.getAsyncExecutor());
        }
    }

    @Scheduled(cron = "0/1 31-59 18 * * ?")
    public void doSingleSnatch() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), taskExecutorConfig.getAsyncExecutor());
        }
    }

    @Scheduled(cron = "0/1 * 7-17 * * ?")
    public void doSingleSnatchOtherTime() {
        List<DoSnatchInfo> allTaskForRun = ticketServiceImpl.getAllTaskForRun();
        for (DoSnatchInfo doSnatchInfo : allTaskForRun) {
            CompletableFuture.runAsync(() -> ticketServiceImpl.snatchingTicket(doSnatchInfo), taskExecutorConfig.getAsyncExecutor());
        }
    }
    @Scheduled(cron = "0/1 * 0-6,19-23 * * ?")
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
        }catch (Exception e){
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

    private HttpHeaders getHeader(String auth,Long orderId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");
        headers.set("authorization", auth);
        headers.set("Referer", "https://pcticket.cstm.org.cn/personal/order_detail?orderId="+orderId);
        headers.set("Sec-Ch-Ua", "\"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"\n");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        return headers;
    }
}
