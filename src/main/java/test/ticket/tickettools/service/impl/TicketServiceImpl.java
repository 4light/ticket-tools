package test.ticket.tickettools.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.dao.PhoneInfoDao;
import test.ticket.tickettools.dao.TaskDetailDao;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.TicketService;
import test.ticket.tickettools.service.WebSocketServer;
import test.ticket.tickettools.utils.DateUtils;
import test.ticket.tickettools.utils.ImageUtils;

import javax.annotation.Resource;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TicketServiceImpl implements TicketService {
    //获取场次url
    private static String getScheduleUrl = "https://pcticket.cstm.org.cn/prod-api/pool/getScheduleByHallId?hallId=1&openPerson=1&queryDate=%s&saleMode=1&single=true";
    //获取场次下余票url
    private static String getPriceByScheduleIdUrl = "https://pcticket.cstm.org.cn/prod-api/pool/getPriceByScheduleId?hallId=1&openPerson=1&queryDate=%s&saleMode=1&scheduleId=";
    //添加人员url
    private static String addUrl = "https://pcticket.cstm.org.cn/prod-api/system/individualContact/add";
    //获取验证码图片
    private static String getCheckImagUrl = "https://pcticket.cstm.org.cn/prod-api/pool/getBlock";
    //提交订单
    private static String shoppingCartUrl = "https://pcticket.cstm.org.cn/prod-api/config/orderRule/shoppingCart";
    private static String getCurrentUserUrl = "https://pcticket.cstm.org.cn/prod-api/getUserInfoToIndividual";
    //购物车接口
    private static String getShoppingCart = "https://pcticket.cstm.org.cn/prod-api/query/order/getShoppingCart";
    //提交订单
    private static String placeOrderUrl = "https://pcticket.cstm.org.cn/prod-api/config/orderRule/placeOrder";
    private static String wxPayForPcUrl = "https://pcticket.cstm.org.cn/prod-api/order/OrderInfo/wxPayForPc";

    private static CloseableHttpClient httpClient = HttpClientBuilder.create()
            .setMaxConnTotal(100) // 设置最大连接数
            .setMaxConnPerRoute(20) // 设置每个路由的最大连接数
            .build();

    private static HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

    private static RestTemplate restTemplate = new RestTemplate(requestFactory);

    @Resource
    TaskDao taskDao;

    @Resource
    TaskDetailDao taskDetailDao;

    @Resource
    PhoneInfoDao phoneInfoDao;

    @Override
    public List<ScheduleInfo> getScheduleInfo() {
        return null;
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Override
    public ServiceResponse addTaskInfo(TaskInfo taskInfo) {
        TaskEntity taskEntity = new TaskEntity();
        BeanUtil.copyProperties(taskInfo, taskEntity);
        if (ObjectUtils.isEmpty(taskInfo.getTaskId())) {
            taskEntity.setCreateDate(new Date());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authority", "pcticket.cstm.org.cn");
            headers.set("accept", "application/json");
            headers.set("authorization", taskInfo.getAuth());
            headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            HttpEntity entity = new HttpEntity<>(headers);
            ResponseEntity getUserRes = restTemplate.exchange(getCurrentUserUrl, HttpMethod.GET, entity, String.class);
            String userInfoStr = getUserRes.getBody().toString();
            JSONObject userInfoJson = JSON.parseObject(userInfoStr);
            JSONObject userInfo = userInfoJson == null ? null : userInfoJson.getJSONObject("user");
            if (userInfo == null) {
                log.info("获取用户信息失败：{}", userInfoJson);
            }
            long userId = userInfo.getLongValue("userId");
            taskEntity.setUserId(userId);
            Integer insert = taskDao.insert(taskEntity);
            if (insert > 0) {
                List<TaskDetailEntity> userList = taskInfo.getUserList();
                if (ObjectUtils.isEmpty(userList)) {
                    return ServiceResponse.createBySuccessMessgge("详情数据为空");
                }
                userList.forEach(o -> {
                    o.setTaskId(taskEntity.getId());
                    o.setCreateDate(new Date());
                });
                Integer res = taskDetailDao.insertBatch(userList);
                if (res == userList.size()) {
                    return ServiceResponse.createBySuccess();
                } else {
                    return ServiceResponse.createByErrorMessage("保存任务详情异常");
                }
            }
            return ServiceResponse.createByErrorMessage("保存任务异常");
        } else {
            taskEntity.setUpdateDate(new Date());
            Integer insert = taskDao.insert(taskEntity);
            if (insert > 0) {
                List<TaskDetailEntity> userList = taskInfo.getUserList();
                userList.forEach(o -> {
                    o.setUpdateDate(new Date());
                });
                Integer res = taskDetailDao.insertBatch(userList);
                if (res == userList.size()) {
                    return ServiceResponse.createBySuccess();
                } else {
                    return ServiceResponse.createByErrorMessage("保存任务详情异常");
                }
            }
            return ServiceResponse.createByErrorMessage("保存任务异常");
        }
    }

    @Override
    public ServiceResponse addTaskDetail(TaskDetailEntity taskDetailEntity) {
        if (taskDetailDao.insert(taskDetailEntity) > 0) {
            return ServiceResponse.createBySuccess();
        }
        return ServiceResponse.createByError();
    }

    @Override
    public void updateTask(TaskEntity taskEntity) {
        Integer integer = taskDao.updateTask(taskEntity);
        if (integer > 0) {
            log.info("更新任务成功");
        } else {
            log.error("更新任务失败");
        }
    }

    @Override
    public ServiceResponse<PageableResponse<TaskInfoListResponse>> queryTask(QueryTaskInfo queryTaskInfo) {
        TaskEntity query = new TaskEntity();
        query.setLoginPhone(queryTaskInfo.getLoginPhone());
        query.setUseDate(queryTaskInfo.getUseDate());
        List<TaskEntity> taskEntities = taskDao.fuzzyQuery(query);
        List<TaskInfoListResponse> list = new ArrayList<>();
        for (TaskEntity taskEntity : taskEntities) {
            Long id = taskEntity.getId();
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByTaskId(id);
            for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
                TaskInfoListResponse taskInfoListResponse = new TaskInfoListResponse();
                taskInfoListResponse.setTaskId(id);
                taskInfoListResponse.setId(taskDetailEntity.getId());
                taskInfoListResponse.setAuthorization(taskEntity.getAuth());
                taskInfoListResponse.setLoginPhone(taskEntity.getLoginPhone());
                taskInfoListResponse.setUseDate(taskEntity.getUseDate());
                taskInfoListResponse.setUserName(taskDetailEntity.getUserName());
                taskInfoListResponse.setIDCard(taskDetailEntity.getIDCard());
                taskInfoListResponse.setDone(taskDetailEntity.getDone());
                taskInfoListResponse.setPayment(taskDetailEntity.getPayment());
                taskInfoListResponse.setUpdateDate(taskDetailEntity.getUpdateDate());
                taskInfoListResponse.setTicketId(taskDetailEntity.getTicketId());
                taskInfoListResponse.setChildrenTicket(taskDetailEntity.getChildrenTicket());
                list.add(taskInfoListResponse);
            }
        }
        return ServiceResponse.createBySuccess(PageableResponse.listCovPageInfo(queryTaskInfo.getPage(), list));
    }

    @Override
    public ServiceResponse<TaskInfo> getTask(Long taskId) {
        TaskInfo taskInfo = new TaskInfo();
        TaskEntity taskEntity = taskDao.selectByPrimaryKey(taskId);
        taskInfo.setTaskId(taskEntity.getId());
        taskInfo.setAuth(taskEntity.getAuth());
        taskInfo.setChannel(taskEntity.getChannel());
        taskInfo.setLoginPhone(taskEntity.getLoginPhone());
        taskInfo.setUseDate(taskEntity.getUseDate());
        taskInfo.setSession(taskEntity.getSession());
        taskInfo.setVenue(taskEntity.getVenue());
        taskInfo.setUserList(taskDetailDao.selectByTaskId(taskId));
        return ServiceResponse.createBySuccess(taskInfo);
    }


    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Override
    public ServiceResponse delete(Long taskId) {
        Integer integer = taskDao.deleteByPrimaryKey(taskId);
        if(integer>0){
            Integer res = taskDetailDao.deleteByTaskId(taskId);
            if(res>0){
                return ServiceResponse.createBySuccess();
            }
            return ServiceResponse.createByErrorMessage("删除详情失败");
        }
        return ServiceResponse.createByErrorMessage("删除任务失败");
    }

    @Override
    public List<TaskDetailEntity> selectUnpaid() {
        return taskDetailDao.selectUnpaid();
    }

    @Override
    public ServiceResponse addPhoneInfo(PhoneInfoEntity phoneInfoEntity) {
        log.info("手机信息:{}", phoneInfoEntity);
        Integer res = phoneInfoDao.insertOrUpdate(phoneInfoEntity);
        if (res > 0) {
            ServiceResponse.createBySuccess();
        }
        return ServiceResponse.createByError();
    }

    @Override
    public ServiceResponse getPhoneMsg(String phoneNum) {
        PhoneInfoEntity phoneInfoEntity = new PhoneInfoEntity();
        phoneInfoEntity.setPhoneNum(phoneNum);
        return ServiceResponse.createBySuccess(phoneInfoDao.select(phoneInfoEntity).getContent());
    }

    @Override
    public Map<String, DoSnatchInfo> getTaskForRun() {
        Map<String, DoSnatchInfo> result = new HashMap<>();
        LocalDate now = LocalDate.now();
        LocalDate snatchDate = now.plusDays(7L);
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUseDate(DateUtils.localDateToDate(snatchDate));
        List<TaskEntity> taskEntities = taskDao.getUnDoneTasks(taskEntity);
        if (ObjectUtils.isEmpty(taskEntities)) {
            return result;
        }
        for (TaskEntity entity : taskEntities) {
            DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
            Long id = entity.getId();
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByTaskId(id);
            if (ObjectUtils.isEmpty(taskDetailEntities)) {
                entity.setDone(true);
                taskDao.updateTask(entity);
            }
            List<Long> taskDetailIds = taskDetailEntities.stream()
                    .map(TaskDetailEntity::getId) // 提取每个对象的 ID
                    .collect(Collectors.toList());
            Map<String, String> nameIdMap = taskDetailEntities.stream()
                    .collect(Collectors.toMap(TaskDetailEntity::getUserName, TaskDetailEntity::getIDCard));
            doSnatchInfo.setTaskId(id);
            doSnatchInfo.setUserId(entity.getUserId());
            doSnatchInfo.setLoginPhone(entity.getLoginPhone());
            doSnatchInfo.setAuthorization(entity.getAuth());
            doSnatchInfo.setSession(entity.getSession());
            doSnatchInfo.setUseDate(entity.getUseDate());
            doSnatchInfo.setTaskDetailIds(taskDetailIds);
            doSnatchInfo.setNameIDMap(nameIdMap);
            result.put(entity.getLoginPhone(), doSnatchInfo);
        }
        return result;
    }


    @Override
    public List<DoSnatchInfo> getAllTaskForRun() {
        List<DoSnatchInfo> result = new ArrayList<>();
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUseDate(DateUtils.localDateToDate(LocalDate.now()));
        List<TaskEntity> allUnDoneTasks = taskDao.getAllUnDoneTasks(taskEntity);
        if (ObjectUtils.isEmpty(allUnDoneTasks)) {
            return result;
        }
        for (TaskEntity entity : allUnDoneTasks) {
            TaskDetailEntity query = new TaskDetailEntity();
            query.setTaskId(entity.getId());
            query.setDone(false);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(query);
            if (ObjectUtils.isEmpty(taskDetailEntities)) {
                entity.setDone(true);
                taskDao.updateTask(entity);
            }
            for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
                DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
                doSnatchInfo.setTaskId(entity.getId());
                doSnatchInfo.setUserId(entity.getUserId());
                doSnatchInfo.setLoginPhone(entity.getLoginPhone());
                doSnatchInfo.setAuthorization(entity.getAuth());
                doSnatchInfo.setUseDate(entity.getUseDate());
                doSnatchInfo.setSession(entity.getSession());
                doSnatchInfo.setTaskDetailIds(Arrays.asList(taskDetailEntity.getId()));
                doSnatchInfo.setNameIDMap(new HashMap<String, String>() {{
                    put(taskDetailEntity.getUserName(), taskDetailEntity.getIDCard());
                }});
                result.add(doSnatchInfo);
            }
        }
        return result;
    }

    @Override
    public List<TaskEntity> getAllUnDoneTask() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUseDate(DateUtils.localDateToDate(LocalDate.now()));
        return taskDao.getAllUnDoneTasks(taskEntity);
    }

    @Override
    public ServiceResponse updateTaskDetail(UpdateTaskDetailRequest updateTaskDetailRequest) {
        TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
        taskDetailEntity.setId(updateTaskDetailRequest.getTaskDetailId());
        taskDetailEntity.setTaskId(updateTaskDetailRequest.getTaskId());
        if (!ObjectUtils.isEmpty(updateTaskDetailRequest.getPayment())) {
            taskDetailEntity.setPayment(updateTaskDetailRequest.getPayment());
        }
        taskDetailEntity.setUpdateDate(new Date());
        Integer integer = taskDetailDao.updateTaskDetail(taskDetailEntity);
        if (integer > 0) {
            return ServiceResponse.createBySuccess();
        }
        return ServiceResponse.createByErrorMessage("更新失败");
    }

    @Override
    public void snatchingTicket(DoSnatchInfo doSnatchInfo) {
        Map<String, String> nameIDMap = doSnatchInfo.getNameIDMap();
        String useDate = DateUtil.format(doSnatchInfo.getUseDate(), "yyyy-MM-dd hh:mm:ss");
        getScheduleUrl = String.format(getScheduleUrl, DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"));
        getPriceByScheduleIdUrl = String.format(getPriceByScheduleIdUrl, DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"));
        try {
            RestTemplate restTemplate = new RestTemplate();
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
            headers.set("authorization", doSnatchInfo.getAuthorization());
            headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            HttpEntity entity = new HttpEntity<>(headers);
            long userId = doSnatchInfo.getUserId();
            String phone = doSnatchInfo.getLoginPhone();
            //获取场次下余票
            ResponseEntity getPriceByScheduleRes = restTemplate.exchange(getPriceByScheduleIdUrl + doSnatchInfo.getSession(), HttpMethod.GET, entity, String.class);
            JSONObject getPriceByScheduleJson = JSON.parseObject(getPriceByScheduleRes.getBody().toString());
            //log.info("获取到的场次下余票为:{}",getPriceByScheduleJson);
            //获取成人票和儿童票
            JSONArray getPriceByScheduleData = getPriceByScheduleJson == null ? null : getPriceByScheduleJson.getJSONArray("data");
            if (ObjectUtils.isEmpty(getPriceByScheduleData)) {
                //log.info("获取到的场次失败");
                return;
            }
            boolean flag = true;
            int priceId = 0;
            int childrenPriceId = 0;
            int discountPriceId = 0;
            int olderPriceId = 0;
            Map<String, Integer> priceNameCountMap = new HashMap<>();
            for (String value : nameIDMap.values()) {
                int ageForIdcard = getAgeForIdcard(value);
                if (ageForIdcard > 0 && ageForIdcard <= 8) {
                    if (priceNameCountMap.containsKey("childrenTicket")) {
                        priceNameCountMap.put("childrenTicket", priceNameCountMap.get("childrenTicket") + 1);
                    } else {
                        priceNameCountMap.put("childrenTicket", 1);
                    }
                    continue;
                }
                if (ageForIdcard > 8 && ageForIdcard <= 18) {
                    if (priceNameCountMap.containsKey("discountTicket")) {
                        priceNameCountMap.put("discountTicket", priceNameCountMap.get("discountTicket") + 1);
                    } else {
                        priceNameCountMap.put("discountTicket", 1);
                    }
                    continue;
                }
                if (ageForIdcard >= 60 && ageForIdcard <= 199) {
                    if (priceNameCountMap.containsKey("olderTicket")) {
                        priceNameCountMap.put("olderTicket", priceNameCountMap.get("olderTicket") + 1);
                    } else {
                        priceNameCountMap.put("olderTicket", 1);
                    }
                    continue;
                }
                if (ageForIdcard >= 0 && ageForIdcard <= 100) {
                    if (priceNameCountMap.containsKey("normalTicket")) {
                        priceNameCountMap.put("normalTicket", priceNameCountMap.get("normalTicket") + 1);
                    } else {
                        priceNameCountMap.put("normalTicket", 1);
                    }
                }
            }
            int ticketPool = 0;
            int childrenTicketPool = 0;
            int discountTicketPool = 0;
            int olderTicketPool = 0;
            for (int i = 0; i < getPriceByScheduleData.size(); i++) {
                JSONObject obj = getPriceByScheduleData.getJSONObject(i);
                if ("普通票".equals(obj.getString("priceName")) || "儿童免费票".equals(obj.getString("priceName")) || "优惠票".equals(obj.getString("priceName")) || "老人免费票".equals(obj.getString("priceName"))) {
                    if ("普通票".equals(obj.getString("priceName"))) {
                        ticketPool = obj.getIntValue("ticketPool");
                        priceId = obj.getInteger("priceId");
                    }
                }
                if ("儿童免费票".equals(obj.getString("priceName"))) {
                    childrenTicketPool = obj.getIntValue("ticketPool");
                    childrenPriceId = obj.getInteger("priceId");
                }
                if ("优惠票".equals(obj.getString("priceName"))) {
                    discountTicketPool = obj.getIntValue("ticketPool");
                    discountPriceId = obj.getInteger("priceId");
                }
                if ("老人免费票".equals(obj.getString("priceName"))) {
                    olderTicketPool = obj.getIntValue("ticketPool");
                    olderPriceId = obj.getInteger("priceId");
                }
            }
            ;
            for (Map.Entry<String, Integer> priceNameCountEntry : priceNameCountMap.entrySet()) {
                if ("normalTicket".equals(priceNameCountEntry.getKey())) {
                    flag = flag && ticketPool >= priceNameCountEntry.getValue();
                    if (flag) {
                        ticketPool = ticketPool - priceNameCountEntry.getValue();
                    }
                }
                if ("childrenTicket".equals(priceNameCountEntry.getKey())) {
                    flag = flag && childrenTicketPool >= priceNameCountEntry.getValue();
                    //如果余票不足看普票数量
                    if (!flag) {
                        if ((ticketPool - priceNameCountEntry.getValue()) >= 0) {
                            ticketPool = ticketPool - priceNameCountEntry.getValue();
                            flag = true;
                        }
                    }
                }
                if ("discountTicket".equals(priceNameCountEntry.getKey())) {
                    flag = flag && discountTicketPool >= priceNameCountEntry.getValue();
                    //如果余票不足看普票数量
                    if (!flag) {
                        if ((ticketPool - priceNameCountEntry.getValue()) >= 0) {
                            ticketPool = ticketPool - priceNameCountEntry.getValue();
                            flag = true;
                        }
                    }
                }
                if ("olderTicket".equals(priceNameCountEntry.getKey())) {
                    flag = flag && olderTicketPool >= priceNameCountEntry.getValue();
                    //如果余票不足看普票数量
                    if (!flag) {
                        if ((ticketPool - priceNameCountEntry.getValue()) >= 0) {
                            ticketPool = ticketPool - priceNameCountEntry.getValue();
                            flag = true;
                        }
                    }
                }
            }

            //余票充足
            if (flag) {
                //几个人添加几次
                for (Map.Entry<String, String> entry : nameIDMap.entrySet()) {
                    HttpEntity addEntity = new HttpEntity<>(buildAddParam(entry.getValue(), entry.getKey(), userId), headers);
                    restTemplate.exchange(addUrl, HttpMethod.POST, addEntity, String.class);
                }
                ResponseEntity<JSONObject> getCheckImagRes = restTemplate.exchange(getCheckImagUrl, HttpMethod.GET, entity, JSONObject.class);
                JSONObject getCheckImageJson = getCheckImagRes.getBody();
                if (!StringUtils.isEmpty(getCheckImageJson)) {
                    JSONObject data = getCheckImageJson.getJSONObject("data");
                    String jigsawImageBase64 = data == null ? null : data.getString("jigsawImageBase64");
                    String originalImageBase64 = data == null ? null : data.getString("originalImageBase64");
                    String secretKey = data == null ? null : data.getString("secretKey");
                    String token = data == null ? null : data.getString("token");
                    String imageUuid = UUID.randomUUID().toString();
                    String sliderImageName = "." + File.separator + imageUuid + "_" + "slider.png";
                    String backImageName = "." + File.separator + imageUuid + "_" + "back.png";
                    ImageUtils.imagCreate(jigsawImageBase64, sliderImageName, 155, 47);
                    ImageUtils.imagCreate(originalImageBase64, backImageName, 155, 310);
                    //图片验证码处理
                    Double x = getPoint(sliderImageName, backImageName, imageUuid);
                    //log.info("uuid的值为：{}", imageUuid);
                    //log.info("x的值为：{}", x);
                    String point = doSecretKey(x, secretKey);
                    HttpEntity shoppingCartUrlEntity = new HttpEntity<>(buildParam(token, priceNameCountMap.get("childrenTicket"), point, doSnatchInfo.getSession(), useDate, priceId, childrenPriceId, discountPriceId, olderPriceId, phone, nameIDMap), headers);
                    ResponseEntity<String> exchange = restTemplate.exchange(shoppingCartUrl, HttpMethod.POST, shoppingCartUrlEntity, String.class);
                    log.info(exchange.getBody());
                    String body = exchange.getBody();
                    JSONObject bodyJson = JSON.parseObject(body);
                    //WebSocketServer.sendInfo("余票不足","web");
                    if (!ObjectUtils.isEmpty(bodyJson) && bodyJson.getIntValue("code") == 200) {
                        ResponseEntity<String> shoppingCartRes = restTemplate.exchange(getShoppingCart, HttpMethod.GET, entity, String.class);
                        String shoppingCartBody = shoppingCartRes.getBody();
                        JSONObject shoppingCartJson = JSON.parseObject(shoppingCartBody);
                        JSONArray dataArr = shoppingCartJson == null ? null : shoppingCartJson.getJSONArray("data");
                        List<TaskDetailEntity> taskDetailEntities = new ArrayList<>();
                        StringBuffer stringBuffer = new StringBuffer();
                        if (!ObjectUtils.isEmpty(dataArr)) {
                            for (int i = 0; i < dataArr.size(); i++) {
                                JSONObject item = dataArr.getJSONObject(i);
                                String certificateInfo = item.getString("certificateInfo");
                                nameIDMap.forEach((key, val) -> {
                                    stringBuffer.append("<p>用户:").append(key).append("</p>");
                                    stringBuffer.append("<p>身份证号:").append(val).append("</p>");
                                    if (ObjectUtils.nullSafeEquals(val, certificateInfo)) {
                                        TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
                                        taskDetailEntity.setTaskId(doSnatchInfo.getTaskId());
                                        taskDetailEntity.setIDCard(val);
                                        taskDetailEntity.setUpdateDate(new Date());
                                        taskDetailEntity.setChildrenTicket(item.getIntValue("isChildFreeTicket") == 1);
                                        taskDetailEntity.setTicketId(item.getLongValue("id"));
                                        taskDetailEntities.add(taskDetailEntity);
                                    }
                                });
                            }
                        }
                        taskDetailDao.updateTaskDetailBath(taskDetailEntities);
                        WebSocketServer.sendInfo(socketMsg("抢票成功", stringBuffer.toString(), 0), null);
                    }
                    try {
                        Files.delete(Paths.get(sliderImageName));
                        Files.delete(Paths.get(backImageName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }


    @Override
    public String pay(PlaceOrderInfo placeOrderInfo) {
        if (!ObjectUtils.isEmpty(placeOrderInfo)) {
            return "weixin://wxpay/bizpayurl?pr=I1zL7CSzz";
        }
        RestTemplate restTemplate = new RestTemplate();
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
        headers.set("authorization", placeOrderInfo.getAuthorization());
        headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        JSONObject param = new JSONObject();
        param.put("childTicketNum", placeOrderInfo.getChildTicketNum());
        param.put("date", DateUtil.format(placeOrderInfo.getDate(), "yyyy-MM-dd"));
        param.put("phone", placeOrderInfo.getLoginPhone());
        param.put("platform", 1);
        param.put("poolFlag", 1);
        param.put("realNameFlag", 1);
        param.put("saleMode", 1);
        param.put("ticketInfoList", placeOrderInfo.getTicketInfoList());
        param.put("ticketNum", placeOrderInfo.getTicketNum());
        param.put("useTicketType", 1);
        HttpEntity entity = new HttpEntity<>(param, headers);
        ResponseEntity<JSONObject> exchange = restTemplate.exchange(placeOrderUrl, HttpMethod.POST, entity, JSONObject.class);
        JSONObject placeOrderRes = exchange.getBody();
        if (!ObjectUtils.isEmpty(placeOrderRes)) {
            JSONObject orderData = placeOrderRes.getJSONObject("data");
            String orderNumber = orderData.getString("orderNumber");
            TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
            taskDetailEntity.setId(placeOrderInfo.getId());
            taskDetailEntity.setOrderNumber(orderNumber);
            taskDetailEntity.setUpdateDate(new Date());
            taskDetailDao.updateTaskDetail(taskDetailEntity);
            int orderId = orderData.getIntValue("orderId");
            JSONObject payParam = new JSONObject();
            payParam.put("id", orderId);
            payParam.put("payType", 0);
            HttpEntity payEntity = new HttpEntity<>(payParam, headers);
            ResponseEntity<JSONObject> payResEntity = restTemplate.exchange(wxPayForPcUrl, HttpMethod.POST, payEntity, JSONObject.class);
            JSONObject payRes = payResEntity.getBody();
            if (!ObjectUtils.isEmpty(payRes) && payRes.getIntValue("code") == 200) {
                return payRes.getString("data");
            }
        }
        return null;
    }

    private String socketMsg(String title, String msg, Integer time) {
        JSONObject res = new JSONObject();
        res.put("title", title);
        res.put("msg", msg);
        res.put("time", time);
        return JSON.toJSONString(res);
    }

    /**
     * 获取滑动距离
     *
     * @param backImagePath
     * @param sliderImagePath
     * @param uid
     * @return
     */
    public Double getPoint(String backImagePath, String sliderImagePath, String uid) {
        Mat backImageMat = opencv_imgcodecs.imread(backImagePath, opencv_imgcodecs.IMREAD_GRAYSCALE);
        Mat sliderImageMat = opencv_imgcodecs.imread(sliderImagePath, opencv_imgcodecs.IMREAD_GRAYSCALE);
        opencv_imgproc.threshold(backImageMat, backImageMat, 215, 255, opencv_imgproc.THRESH_BINARY);
        opencv_imgproc.threshold(sliderImageMat, sliderImageMat, 215, 255, opencv_imgproc.THRESH_BINARY);
        //保存为黑白图片
        opencv_imgcodecs.imwrite("./" + uid + "_backBlack.png", backImageMat);
        opencv_imgcodecs.imwrite("./" + uid + "_sliderBlack.png", sliderImageMat);
        Mat result = new Mat();
        opencv_imgproc.matchTemplate(sliderImageMat, backImageMat, result, opencv_imgproc.TM_CCORR_NORMED);
        opencv_core.normalize(result, result, 1, 0, opencv_core.NORM_MINMAX, -1, new Mat());
        DoublePointer doublePointer = new DoublePointer(new double[2]);
        Point maxLoc = new Point();
        opencv_core.minMaxLoc(result, null, doublePointer, null, maxLoc, null);
        opencv_imgproc.rectangle(sliderImageMat, maxLoc, new Point(maxLoc.x() + backImageMat.cols(), maxLoc.y() + backImageMat.rows()), new Scalar(0, 255, 0, 1));
        try {
            Files.delete(Paths.get("./" + uid + "_backBlack.png"));
            Files.delete(Paths.get("./" + uid + "_sliderBlack.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int real = maxLoc.x() * 330 / 310;
        //log.info("real:{}", real);
        return real * 310 / 330.0;
    }

    /**
     * js加密
     *
     * @param x
     * @param secretKey
     * @return
     */
    public String doSecretKey(Double x, String secretKey) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval(new java.io.InputStreamReader(TicketServiceImpl.class.getResourceAsStream("/META-INF/resources/webjars/crypto-js/3.1.9-1/crypto-js.js")));
            // 读取 JavaScript 文件并执行
            String scriptFile = "getPoint.js";
            engine.eval(new java.io.FileReader(scriptFile));
            JSONObject param = new JSONObject();
            param.put("x", x);
            param.put("y", 5);
            // 获取 JavaScript 函数的执行结果
            Invocable invocable = (Invocable) engine;
            Object result = invocable.invokeFunction("getPoint", param.toString(), secretKey);
            if (result != null) {
                return result.toString();
            }
        } catch (ScriptException | java.io.FileNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 构建参数
     *
     * @param captchaToken
     * @param childTicketNum
     * @param point
     * @param hallScheduleId
     * @param useDate
     * @param priceId
     * @param childrenPriceId
     * @param olderTicketPriceId
     * @param phone
     * @return
     */
    public Object buildParam(String captchaToken, Integer childTicketNum, String point, Integer hallScheduleId, String useDate, Integer priceId, Integer childrenPriceId, Integer discountPriceId, Integer olderTicketPriceId, String phone, Map<String, String> iDMap) {
        JSONObject param = new JSONObject();
        param.put("captchaToken", captchaToken);
        param.put("childTicketNum", childTicketNum);
        param.put("date", useDate);
        param.put("phone", phone);
        param.put("platform", 1);
        param.put("pointJson", point);
        param.put("poolFlag", 1);
        param.put("realNameFlag", 1);
        param.put("saleMode", 1);
        param.put("ticketNum", iDMap.size());
        param.put("useTicketType", 1);
        List ticketInfoList = new ArrayList();
        for (Map.Entry<String, String> entry : iDMap.entrySet()) {
            int ageForIdCard = getAgeForIdcard(entry.getValue());
            JSONObject ticketInfo = new JSONObject();
            ticketInfo.put("certificate", 1);
            ticketInfo.put("certificateInfo", entry.getValue());
            ticketInfo.put("cinemaFlag", 0);
            ticketInfo.put("hallId", 1);
            ticketInfo.put("hallScheduleId", hallScheduleId);
            if (ageForIdCard > 0 && ageForIdCard <= 8) {
                ticketInfo.put("isChildFreeTicket", 1);
            } else {
                ticketInfo.put("isChildFreeTicket", 0);
            }
            ticketInfo.put("platform", 1);
            ticketInfo.put("realNameFlag", 1);
            ticketInfo.put("saleMode", 1);
            ticketInfo.put("status", 0);
            if (ageForIdCard >= 0 && ageForIdCard <= 100) {
                ticketInfo.put("ticketPriceId", priceId);
            }
            if (ageForIdCard > 0 && ageForIdCard <= 8) {
                ticketInfo.put("ticketPriceId", childrenPriceId);
            }
            if (ageForIdCard > 8 && ageForIdCard <= 18) {
                ticketInfo.put("ticketPriceId", discountPriceId);
            }
            if (ageForIdCard >= 60 && ageForIdCard <= 199) {
                ticketInfo.put("ticketPriceId", olderTicketPriceId);
            }
            ticketInfo.put("useDate", useDate);
            ticketInfo.put("userName", entry.getKey());
            ticketInfoList.add(ticketInfo);
        }
        param.put("ticketInfoList", ticketInfoList);
        return param;
    }


    /**
     * 通过身份证获取年龄
     *
     * @param idcard
     * @return
     */
    public int getAgeForIdcard(String idcard) {
        try {
            int age = 0;
            if (StringUtils.isEmpty(idcard)) {
                return age;
            }

            String birth = "";
            if (idcard.length() == 18) {
                birth = idcard.substring(6, 14);
            } else if (idcard.length() == 15) {
                birth = "19" + idcard.substring(6, 12);
            }

            int year = Integer.valueOf(birth.substring(0, 4));
            int month = Integer.valueOf(birth.substring(4, 6));
            int day = Integer.valueOf(birth.substring(6));
            Calendar cal = Calendar.getInstance();
            age = cal.get(Calendar.YEAR) - year;
            //周岁计算
            if (cal.get(Calendar.MONTH) < (month - 1) || (cal.get(Calendar.MONTH) == (month - 1) && cal.get(Calendar.DATE) < day)) {
                age--;
            }
            return age;
        } catch (Exception e) {
            e.getMessage();
        }
        return -1;
    }


    /**
     * 构建添加人员入参
     *
     * @param certificateNumber
     * @param name
     * @param userId
     * @return
     */
    private JSONObject buildAddParam(String certificateNumber, String name, Long userId) {
        JSONObject param = new JSONObject();
        param.put("certificateNumber", certificateNumber);
        param.put("certificateType", 1);
        param.put("isShowError", "N");
        param.put("name", name);
        param.put("userId", userId);
        return param;
    }
}
