package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.springframework.beans.BeanUtils;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bytedeco.javacpp.DoublePointer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.dao.AccountInfoDao;
import test.ticket.tickettools.dao.TaskDetailDao;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.dao.UserDao;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.domain.entity.UserEntity;
import test.ticket.tickettools.service.TicketService;
import test.ticket.tickettools.service.WebSocketServer;
import test.ticket.tickettools.utils.*;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TicketServiceImpl implements TicketService {
    //-----------------------------科技馆----------------------------------
    //查询个人信息
    private static String queryUserInfoUrl = "/prod-api/getUserInfoToIndividual";
    //获取场次url
    private static String getScheduleUrl = "https://pcticket.cstm.org.cn/prod-api/pool/getScheduleByHallId?hallId=1&openPerson=1&queryDate=%s&saleMode=1&single=true";
    //获取场次下余票url
    //private static String getPriceByScheduleIdUrl = "https://pcticket.cstm.org.cn/prod-api/pool/ingore/getCalendar?saleMode=1&openPerson=1";
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


    private String getPlaceMuUserInfoUrl ="https://lotswap.dpm.org.cn/lotsapi/leaguer/api/userLeaguer/manage/leaguerInfo?cipherText=0&merchantId=2655&merchantInfoId=2655";
    private String getChnMuUserInfoUrl ="https://uu.chnmuseum.cn/prod-api/getUserInfoToIndividual2Mini?p=wxmini";


    private static List<String> doneList = new ArrayList<>();
    private static Map<Long,Object> runTaskCache=new ConcurrentHashMap<>();
    private static Map<Long,Object> msgCache=new ConcurrentHashMap<>();

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
    AccountInfoDao accountInfoDao;
    @Resource
    UserDao userDao;


    @Override
    public ServiceResponse getCurrentUser(QueryTaskInfo queryTaskInfo) {
        try {
            if (ChannelEnum.CSTM.getCode().equals(queryTaskInfo.getChannel())) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("authority", "pcticket.cstm.org.cn");
                headers.set("accept", "application/json");
                headers.set("authorization", "Bearer " + queryTaskInfo.getApiToken());
                headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
                HttpEntity entity = new HttpEntity<>(headers);
                //获取场次下余票
                ResponseEntity getUserInfoRes = restTemplate.exchange(ChannelEnum.CSTM.getBaseUrl() + queryUserInfoUrl, HttpMethod.GET, entity, String.class);
                JSONObject getUserInfoJson = JSON.parseObject(getUserInfoRes.getBody().toString());
                if (getUserInfoJson == null || getUserInfoJson.getIntValue("code") != 200) {
                    log.info("获取用户信息失败：", getUserInfoJson);
                    return ServiceResponse.createByErrorMessage("获取用户信息失败,请确认是否已登录");
                }
                TaskEntity taskEntity = new TaskEntity();
                taskEntity.setAuth(queryTaskInfo.getApiToken());
                taskEntity.setAccount(getUserInfoJson.getJSONObject("user").getString("phoneNumber"));
                taskEntity.setUpdateDate(new Date());
                taskEntity.setChannel(ChannelEnum.CSTM.getCode());
                taskEntity.setUserId(getUserInfoJson.getJSONObject("user").getLongValue("userId"));
                taskDao.updateAuthByPhone(taskEntity);
                return ServiceResponse.createBySuccess(getUserInfoJson.get("user"));
            }
            if (ChannelEnum.MFU.equals(queryTaskInfo.getChannel())) {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Override
    public ServiceResponse addTaskInfo(TaskInfo taskInfo) {
        TaskEntity taskEntity = JSON.parseObject(JSON.toJSONString(taskInfo), TaskEntity.class);
        BeanUtils.copyProperties(taskInfo, taskEntity);
        Long userInfoId = taskInfo.getUserInfoId();
        AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
        if(ObjectUtils.isEmpty(accountInfoEntity)|| accountInfoEntity.getStatus()){
            return ServiceResponse.createByErrorMessage("购票账号未授权不能创建任务");
        }
        if (accountInfoEntity != null) {
            taskEntity.setAccount(accountInfoEntity.getAccount());
            taskEntity.setPwd(accountInfoEntity.getPwd());
        }
        if (ObjectUtils.isEmpty(taskInfo.getId())) {
            taskEntity.setCreateDate(new Date());
            taskEntity.setAuth(taskInfo.getAuth());
            taskEntity.setUserInfoId(taskInfo.getUserInfoId());
            taskEntity.setTaskName(taskInfo.getTaskName());
            Integer insert = taskDao.insert(taskEntity);
            if (insert > 0) {
                List<TaskDetailEntity> userList = taskInfo.getUserList();
                if (ObjectUtils.isEmpty(userList)) {
                    return ServiceResponse.createBySuccessMessgge("详情数据为空");
                }
                userList.forEach(o -> {
                    o.setTaskId(taskEntity.getId());
                    o.setPayment(false);
                    o.setCreateDate(new Date());
                });
                Integer res = taskDetailDao.insertBatch(userList);
                if (res == userList.size()) {
                    return ServiceResponse.createBySuccess();
                } else {
                    return ServiceResponse.createByErrorMessage("保存任务详情异常");
                }
            }
        } else {
            taskEntity.setUpdateDate(new Date());
            taskEntity.setTaskName(taskInfo.getTaskName());
            taskEntity.setAccount(accountInfoEntity.getAccount());
            taskEntity.setPwd(accountInfoEntity.getPwd());
            taskEntity.setUserInfoId(userInfoId);
            Integer insert = taskDao.updateTask(taskEntity);
            if (insert > 0) {
                List<TaskDetailEntity> all = taskDetailDao.selectByTaskId(taskEntity.getId());
                List<TaskDetailEntity> userList = taskInfo.getUserList();
                List<TaskDetailEntity> addList = userList.stream().filter(o -> o.getId() == null).collect(Collectors.toList());
                List<TaskDetailEntity> updateList = userList.stream().filter(o -> o.getId() != null).collect(Collectors.toList());
                List<TaskDetailEntity> deleteList = new ArrayList<>();
                List<Long> taskDetailIds = updateList.stream().map(TaskDetailEntity::getId).collect(Collectors.toList());
                if (updateList.size() != all.size()) {
                    all.forEach(allEntity -> {
                        if (!taskDetailIds.contains(allEntity.getId())) {
                            deleteList.add(allEntity);
                        }
                    });
                    if(deleteList.size()>0){
                        taskDetailDao.deleteTaskDetailBath(deleteList);
                    }
                }
                if (!ObjectUtils.isEmpty(addList)) {
                    addList.forEach(o -> {
                        o.setCreateDate(new Date());
                        o.setPayment(false);
                        o.setTaskId(taskEntity.getId());
                    });
                    taskDetailDao.insertBatch(addList);
                }
                if (!ObjectUtils.isEmpty(updateList)) {
                    taskDetailDao.updateTaskDetailBath(updateList);
                }
                return ServiceResponse.createBySuccess();
            }
        }
        return ServiceResponse.createByErrorMessage("保存任务异常");
    }

    @Override
    public ServiceResponse initTask(InitTaskParam initTaskParam) {
        String cancelTicketUrl="https://pcticket.cstm.org.cn/prod-api/order/ticketInfo/removeForShopping/";
        TaskEntity taskEntity=new TaskEntity();
        taskEntity.setDone(false);
        taskEntity.setId(initTaskParam.getTaskId());
        taskEntity.setUpdateDate(new Date());
        taskDao.updateTask(taskEntity);
        List<TaskDetailEntity> taskDetailEntityList = initTaskParam.getTaskDetailEntityList();
        TaskEntity queryEntity=new TaskEntity();
        queryEntity.setId(initTaskParam.getTaskId());
        TaskEntity currentTask = taskDao.queryTask(queryEntity);
        AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(currentTask.getUserInfoId());
        HttpHeaders headers=getHeader(accountInfoEntity.getHeaders());
        RestTemplate restTemplate=TemplateUtil.initSSLTemplate();
        HttpEntity entity=new HttpEntity(headers);
        List<String> failTicket=new ArrayList<>();
        for (TaskDetailEntity taskDetailEntity : taskDetailEntityList) {
            Long ticketId = taskDetailEntity.getTicketId();
            Long id = taskDetailEntity.getId();
            if(!ObjectUtils.isEmpty(ticketId)&&ticketId!=0&&!ObjectUtils.isEmpty(id)){
                JSONObject response = TemplateUtil.getResponse(restTemplate, cancelTicketUrl + ticketId, HttpMethod.DELETE, entity);
                if(!ObjectUtils.isEmpty(response)&&response.getIntValue("code")==200){
                    log.info("删除购物车订单结果:{}",response);
                    taskDetailEntity.setPrice(null);
                    taskDetailEntity.setDone(false);
                    //successEntities.add(taskDetailEntity);
                    taskDetailDao.updateTaskDetail(taskDetailEntity);
                }else{
                    failTicket.add(taskDetailEntity.getUserName());
                }
            }
        }
        if(ObjectUtils.isEmpty(failTicket)){
                return ServiceResponse.createBySuccessMessgge("重置成功");
        }
        return ServiceResponse.createByErrorMessage("以下人员重置失败:"+String.join(",",failTicket));
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
        query.setChannel(queryTaskInfo.getChannel());
        query.setAccount(queryTaskInfo.getAccount());
        query.setUseDate(queryTaskInfo.getUseDate());
        query.setUserInfoId(queryTaskInfo.getUserInfoId());
        query.setYn(queryTaskInfo.getYn());
        UserEntity byUsername = userDao.findByUsername(queryTaskInfo.getCreator());
        if(!StrUtil.equals("admin",byUsername.getRole())){
            query.setCreator(queryTaskInfo.getCreator());
        }
        List<TaskEntity> taskEntities = taskDao.fuzzyQuery(query);
        List<TaskInfoListResponse> list = new ArrayList<>();
        for (TaskEntity taskEntity : taskEntities) {
            Long id = taskEntity.getId();
            Long userInfoId = taskEntity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
            TaskDetailEntity queryEntity=new TaskDetailEntity();
            queryEntity.setTaskId(id);
            queryEntity.setDone(queryTaskInfo.getDone());
            queryEntity.setPayment(queryTaskInfo.getPayment());
            queryEntity.setUserName(queryTaskInfo.getUserName());
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(queryEntity);
            for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
                TaskInfoListResponse taskInfoListResponse = new TaskInfoListResponse();
                taskInfoListResponse.setTaskId(id);
                taskInfoListResponse.setTaskName(taskEntity.getTaskName());
                taskInfoListResponse.setAccount(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getUserName());
                taskInfoListResponse.setId(taskDetailEntity.getId());
                taskInfoListResponse.setAuthorization(accountInfoEntity.getHeaders());
                //使用名字好区分
                taskInfoListResponse.setAccountName(accountInfoEntity ==null?null: accountInfoEntity.getUserName());
                taskInfoListResponse.setTaskYn(taskEntity.getYn());
                taskInfoListResponse.setAccount(taskEntity.getAccount());
                taskInfoListResponse.setUseDate(taskEntity.getUseDate());
                taskInfoListResponse.setUserName(taskDetailEntity.getUserName());
                taskInfoListResponse.setIDCard(taskDetailEntity.getIDCard());
                taskInfoListResponse.setDone(taskDetailEntity.getDone());
                taskInfoListResponse.setPayment(taskDetailEntity.getPayment());
                taskInfoListResponse.setUpdateDate(taskDetailEntity.getUpdateDate());
                taskInfoListResponse.setTicketId(taskDetailEntity.getTicketId());
                taskInfoListResponse.setChildrenTicket(taskDetailEntity.getChildrenTicket());
                taskInfoListResponse.setChannel(taskEntity.getChannel());
                taskInfoListResponse.setOrderId(taskDetailEntity.getOrderId());
                taskInfoListResponse.setOrderNumber(taskDetailEntity.getOrderNumber());
                taskInfoListResponse.setPrice(taskDetailEntity.getPrice());
                taskInfoListResponse.setUserInfoId(taskEntity.getUserInfoId());
                taskInfoListResponse.setExt(taskDetailEntity.getExt());
                taskInfoListResponse.setCreator(taskEntity.getCreator());
                taskInfoListResponse.setCreateDate(taskEntity.getCreateDate());
                taskInfoListResponse.setTaskDetailYn(taskDetailEntity.getYn());
                list.add(taskInfoListResponse);
            }
        }
        return ServiceResponse.createBySuccess(PageableResponse.listCovPageInfo(queryTaskInfo.getPage(), list));
    }

    @Override
    public ServiceResponse<TaskInfo> getTask(Long taskId,Boolean yn) {
        TaskInfo taskInfo = new TaskInfo();
        TaskEntity query=new TaskEntity();
        query.setId(taskId);
        query.setYn(yn);
        TaskEntity taskEntity = taskDao.queryTask(query);
        taskInfo.setId(taskEntity.getId());
        taskInfo.setAuth(taskEntity.getAuth());
        taskInfo.setChannel(taskEntity.getChannel());
        taskInfo.setAccount(taskEntity.getAccount());
        taskInfo.setUseDate(taskEntity.getUseDate());
        taskInfo.setSession(taskEntity.getSession());
        taskInfo.setVenue(taskEntity.getVenue());
        taskInfo.setUserInfoId(taskEntity.getUserInfoId());
        taskInfo.setUserList(taskDetailDao.queryAllTaskDetailById(taskId));
        return ServiceResponse.createBySuccess(taskInfo);
    }


    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Override
    public ServiceResponse delete(Long taskId,Boolean yn) {
        TaskEntity taskEntity=new TaskEntity();
        taskEntity.setId(taskId);
        taskEntity.setYn(yn);
        Integer integer = taskDao.updateTask(taskEntity);
        if (integer > 0) {
            Integer res = taskDetailDao.deleteByTaskId(taskId);
            if (res > 0) {
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
    public ServiceResponse addPhoneInfo(AccountInfoEntity accountInfoEntity) {
        log.info("手机信息:{}", accountInfoEntity);
        Integer res = accountInfoDao.updateByChannelAccount(accountInfoEntity);
        if (res > 0) {
            ServiceResponse.createBySuccess();
        }
        return ServiceResponse.createByError();
    }

    @Override
    public ServiceResponse getPhoneMsg(String phoneNum) {
        AccountInfoEntity accountInfoEntity = new AccountInfoEntity();
        accountInfoEntity.setPhoneNum(phoneNum);
        return ServiceResponse.createBySuccess(accountInfoDao.selectList(accountInfoEntity).get(0).getAccount());
    }

    @Override
    public Map<String, DoSnatchInfo> getTaskForRun() {
        Map<String, DoSnatchInfo> result = new HashMap<>();
        LocalDate now = LocalDate.now();
        LocalDate snatchDate = now.plusDays(7L);
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.CSTM.getCode());
        taskEntity.setUseDate(DateUtils.localDateToDate(snatchDate));
        List<TaskEntity> taskEntities = taskDao.getUnDoneTasks(taskEntity);
        if (ObjectUtils.isEmpty(taskEntities)) {
            return result;
        }
        for (TaskEntity entity : taskEntities) {
            DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
            Long id = entity.getId();
            Long userInfoId = entity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByTaskIdLimit(id);
            if (ObjectUtils.isEmpty(taskDetailEntities)) {
                entity.setDone(true);
                taskDao.updateTask(entity);
            }
            List<Long> taskDetailIds = taskDetailEntities.stream()
                    .map(TaskDetailEntity::getId) // 提取每个对象的 ID
                    .collect(Collectors.toList());
            Map<String, String> idNameMap = taskDetailEntities.stream()
                    .collect(Collectors.toMap(TaskDetailEntity::getIDCard, TaskDetailEntity::getUserName));
            doSnatchInfo.setTaskId(id);
            doSnatchInfo.setCreator(entity.getCreator());
            doSnatchInfo.setUserId(Long.valueOf(accountInfoEntity.getChannelUserId()));
            doSnatchInfo.setAccount(entity.getAccount());
            doSnatchInfo.setAuthorization(accountInfoEntity.getHeaders());
            doSnatchInfo.setSession(entity.getSession());
            doSnatchInfo.setUseDate(entity.getUseDate());
            doSnatchInfo.setTaskDetailIds(taskDetailIds);
            doSnatchInfo.setIdNameMap(idNameMap);
            result.put(entity.getAccount(), doSnatchInfo);
        }
        return result;
    }


    @Override
    public List<DoSnatchInfo> getAllTaskForRun() {
        List<DoSnatchInfo> result = new ArrayList<>();
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setUseDate(DateUtils.localDateToDate(LocalDate.now()));
        taskEntity.setChannel(ChannelEnum.CSTM.getCode());
        List<TaskEntity> allUnDoneTasks = taskDao.getAllUnDoneTasks(taskEntity);
        if (ObjectUtils.isEmpty(allUnDoneTasks)) {
            return result;
        }
        for (TaskEntity entity : allUnDoneTasks) {
            Long userInfoId = entity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
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
                doSnatchInfo.setCreator(entity.getCreator());
                doSnatchInfo.setTaskId(entity.getId());
                doSnatchInfo.setUserId(accountInfoEntity.getChannelUserId()==null?null:Long.valueOf(accountInfoEntity.getChannelUserId()));
                doSnatchInfo.setAccount(entity.getAccount());
                doSnatchInfo.setAuthorization(accountInfoEntity.getHeaders());
                doSnatchInfo.setUseDate(entity.getUseDate());
                doSnatchInfo.setSession(entity.getSession());
                doSnatchInfo.setTaskDetailIds(Arrays.asList(taskDetailEntity.getId()));
                doSnatchInfo.setSession(entity.getSession());
                doSnatchInfo.setIdNameMap(new HashMap<String, String>() {{
                    put(taskDetailEntity.getIDCard(), taskDetailEntity.getUserName());
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
    public Boolean updateTaskDetail(TaskDetailEntity taskDetailEntity) {
        Integer integer = taskDetailDao.updateTaskDetail(taskDetailEntity);
        return integer > 0;
    }

    @Override
    public void snatchingTicket(DoSnatchInfo doSnatchInfo) {
        Long taskId = doSnatchInfo.getTaskId();
        /*if(runTaskCache.containsKey(taskId)){
            return;
        }
        runTaskCache.put(taskId,true);*/
        String getHallUrl = "https://pcticket.cstm.org.cn/prod-api/pool/ingore/getHall?saleMode=1&openPerson=1&queryDate=%s";
        Map<String, String> nameIDMap = doSnatchInfo.getIdNameMap();
        String formatGetHallUrl = String.format(getHallUrl, DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"));
        RestTemplate restTemplate=TemplateUtil.initSSLTemplate();
        try {
            HttpHeaders headers = getHeader(doSnatchInfo.getAuthorization());
            HttpEntity entity = new HttpEntity<>(headers);
            Long userId = doSnatchInfo.getUserId();
            String phone = doSnatchInfo.getAccount();
            //获取场次下余票
            /*ResponseEntity getPriceByScheduleRes = restTemplate.exchange(formatGetHallUrl, HttpMethod.GET, entity, String.class);
            JSONObject getPriceByScheduleJson = JSON.parseObject(getPriceByScheduleRes.getBody().toString());*/
            JSONObject getPriceByScheduleJson = TemplateUtil.getResponse(restTemplate,formatGetHallUrl,HttpMethod.GET,entity);
            if(!ObjectUtils.isEmpty(getPriceByScheduleJson)&&getPriceByScheduleJson.getIntValue("code")==401){
                if(!msgCache.containsKey(doSnatchInfo.getTaskId())) {
                    WebSocketServer.sendInfo(socketMsg("抢票异常", "账号:" + doSnatchInfo.getAccount() + "登录态异常", 0), doSnatchInfo.getCreator());
                }
                msgCache.put(doSnatchInfo.getTaskId(),true);
            }
            //log.info("获取到的场次下余票为:{}",getPriceByScheduleJson);
            //获取成人票和儿童票
            JSONArray getPriceByScheduleData = getPriceByScheduleJson == null ? null : getPriceByScheduleJson.getJSONArray("data");
            if (ObjectUtils.isEmpty(getPriceByScheduleData)) {
                runTaskCache.remove(taskId);
                return;
            }
            int ticketPoolNum=0;
            JSONArray priceTicketPoolVOS = new JSONArray();
            for (int i = 0; i < getPriceByScheduleData.size(); i++) {
                JSONObject priceByScheduleJson = getPriceByScheduleData.getJSONObject(i);
                if (priceByScheduleJson.getIntValue("hallId") == 1) {
                    ticketPoolNum=priceByScheduleJson.getInteger("ticketPool");
                    JSONArray scheduleTicketPoolVOS = priceByScheduleJson.getJSONArray("scheduleTicketPoolVOS");
                    for (int j = 0; j < scheduleTicketPoolVOS.size(); j++) {
                        priceTicketPoolVOS = scheduleTicketPoolVOS.getJSONObject(j).getJSONArray("priceTicketPoolVOS");
                        break;
                    }
                }
            }
            boolean flag = true;
            //普通票
            int priceId = 35;
            //儿童票
            int childrenPriceId = 37;
            //优惠票
            int discountPriceId = 36;
            //老年票
            int olderPriceId = 38;
            Map<String, Integer> priceNameCountMap = new HashMap<>();
            for (String ids : nameIDMap.values()) {
                int ageForIdcard = getAgeForIdcard(ids);
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
            for (int i = 0; i < priceTicketPoolVOS.size(); i++) {
                JSONObject obj = priceTicketPoolVOS.getJSONObject(i);
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
            /*for (Map.Entry<String, Integer> priceNameCountEntry : priceNameCountMap.entrySet()) {
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
            }*/
            //余票充足
            if (ticketPoolNum>=doSnatchInfo.getIdNameMap().size()) {
                log.info("获取到余票数据:{},抢票人数量:{}",ticketPoolNum,doSnatchInfo.getIdNameMap().size());
                //几个人添加几次
                for (Map.Entry<String, String> entry : nameIDMap.entrySet()) {
                    HttpEntity addEntity = new HttpEntity<>(buildAddParam(entry.getKey(),entry.getValue(), userId), headers);
                    //restTemplate.exchange(addUrl, HttpMethod.POST, addEntity, String.class);
                    JSONObject response = TemplateUtil.getResponse(restTemplate, addUrl, HttpMethod.POST, addEntity);
                    if(ObjectUtils.isEmpty(response)||response.getIntValue("code")!=200){
                        if(!msgCache.containsKey(doSnatchInfo.getTaskId())) {
                            WebSocketServer.sendInfo(socketMsg("抢票异常", "账号:" + doSnatchInfo.getAccount() + "登录态异常", 0), doSnatchInfo.getCreator());
                            SendMessageUtil.send(ChannelEnum.CSTM.getDesc(),DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"),"账号：",doSnatchInfo.getAccount(),"登录态异常");
                            List<Long> taskDetailIds = doSnatchInfo.getTaskDetailIds();
                            for (Long taskDetailId : taskDetailIds) {
                                TaskDetailEntity taskDetailEntity=new TaskDetailEntity();
                                taskDetailEntity.setId(taskDetailId);
                                taskDetailEntity.setExt(response.getString("msg"));
                                taskDetailDao.updateTaskDetail(taskDetailEntity);
                            }
                        }
                        msgCache.put(doSnatchInfo.getTaskId(),true);
                        runTaskCache.remove(taskId);
                        return;
                    }
                }
                //ResponseEntity<JSONObject> getCheckImagRes = restTemplate.exchange(getCheckImagUrl, HttpMethod.GET, entity, JSONObject.class);
                //JSONObject getCheckImageJson = getCheckImagRes.getBody();
                JSONObject getCheckImageJson = TemplateUtil.getResponse(restTemplate,getCheckImagUrl,HttpMethod.GET,entity);
                if (!ObjectUtils.isEmpty(getCheckImageJson)&&getCheckImageJson.getIntValue("code")==200) {
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
                    JSONObject param = new JSONObject();
                    param.put("x", x);
                    param.put("y", 5);
                    String point = EncDecUtil.doAES(JSON.toJSONString(param), secretKey);
                    Integer childrenTicketNum = priceNameCountMap.get("childrenTicket");
                    HttpEntity shoppingCartUrlEntity = new HttpEntity<>(buildParam(token, childrenTicketNum==null?0:childrenTicketNum, point, doSnatchInfo.getSession(), doSnatchInfo.getUseDate(), priceId, childrenPriceId, discountPriceId, olderPriceId, phone, nameIDMap), headers);
                    //ResponseEntity<String> exchange = restTemplate.exchange(shoppingCartUrl, HttpMethod.POST, shoppingCartUrlEntity, String.class);
                    //log.info(exchange.getBody());
                    //String body = exchange.getBody();
                    //JSONObject bodyJson = JSON.parseObject(body);
                    JSONObject bodyJson = TemplateUtil.getResponse(restTemplate,shoppingCartUrl,HttpMethod.POST,shoppingCartUrlEntity);
                    if (!ObjectUtils.isEmpty(bodyJson) && (bodyJson.getIntValue("code") == 550 || bodyJson.getIntValue("code") == 503)) {
                        if(!msgCache.containsKey(doSnatchInfo.getTaskId())) {
                            //WebSocketServer.sendInfo(socketMsg("抢票异常", "账号:"+doSnatchInfo.getAccount()+","+bodyJson.getString("msg"), 0), doSnatchInfo.getCreator());
                            List<Long> taskDetailIds = doSnatchInfo.getTaskDetailIds();
                            for (Long taskDetailId : taskDetailIds) {
                                TaskDetailEntity taskDetailEntity=new TaskDetailEntity();
                                taskDetailEntity.setId(taskDetailId);
                                taskDetailEntity.setExt("购票账号："+doSnatchInfo.getAccount()+"。"+bodyJson.getString("msg"));
                                taskDetailDao.updateTaskDetail(taskDetailEntity);
                            }
                        }
                        msgCache.put(doSnatchInfo.getTaskId(),true);
                        runTaskCache.remove(taskId);
                        try {
                            Files.delete(Paths.get(sliderImageName));
                            Files.delete(Paths.get(backImageName));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    //WebSocketServer.sendInfo("余票不足","web");
                    if (!ObjectUtils.isEmpty(bodyJson) && bodyJson.getIntValue("code") == 200) {
                        msgCache.remove(doSnatchInfo.getTaskId());
                        log.info("提交订单结果：{}", bodyJson);
                        /*//doneList.addAll(nameIDMap.values());
                        HttpEntity placeOrderEntity = new HttpEntity<>(buildPlaceOrderParam(priceNameCountMap.get("childrenTicket"), useDate, phone, bodyJson.getJSONArray("data").toJavaList(Long.class)), headers);
                        ResponseEntity<String> placeOrderRes = restTemplate.exchange(placeOrderUrl, HttpMethod.POST, placeOrderEntity, String.class);
                        String placeOrderBody = placeOrderRes.getBody();
                        log.info("下单结果：{}",placeOrderBody);
                        JSONObject placeOrderJson = JSON.parseObject(placeOrderBody);
                        if(placeOrderJson==null||placeOrderJson.getIntValue("code")!=200){
                            try {
                                Files.delete(Paths.get(sliderImageName));
                                Files.delete(Paths.get(backImageName));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }*/
                        //查询个人订单
                        headers.set("Referer", "https://pcticket.cstm.org.cn/personal/car");
                        HttpEntity searchEntity = new HttpEntity(headers);
                        ResponseEntity<String> searchResEntity = restTemplate.exchange(getShoppingCart, HttpMethod.GET, searchEntity, String.class);
                        String searchResBody = searchResEntity.getBody();
                        JSONObject searchBodyJson = JSON.parseObject(searchResBody);
                        if (searchBodyJson == null || searchBodyJson.getIntValue("code") != 200) {
                            log.info("查询个人订单失败：{}", searchBodyJson);
                            runTaskCache.remove(taskId);
                            try {
                                Files.delete(Paths.get(sliderImageName));
                                Files.delete(Paths.get(backImageName));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        JSONArray dataArr = searchBodyJson.getJSONArray("data");
                        List<TaskDetailEntity> taskDetailEntities = new ArrayList<>();
                        if (!ObjectUtils.isEmpty(dataArr)) {
                            for (int i = 0; i < dataArr.size(); i++) {
                                JSONObject item = dataArr.getJSONObject(i);
                                String certificateInfo = item.getString("certificateInfo");
                                nameIDMap.forEach((key, val) -> {
                                    if (ObjectUtils.nullSafeEquals(key, certificateInfo)) {
                                        TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
                                        taskDetailEntity.setTaskId(doSnatchInfo.getTaskId());
                                        taskDetailEntity.setIDCard(key);
                                        taskDetailEntity.setUpdateDate(new Date());
                                        taskDetailEntity.setChildrenTicket(item.getIntValue("isChildFreeTicket") == 1);
                                        taskDetailEntity.setTicketId(item.getLongValue("id"));
                                        taskDetailEntity.setDone(true);
                                        taskDetailEntity.setPrice(item.getIntValue("sourcePrice"));
                                        taskDetailEntity.setExt(null);
                                        taskDetailEntities.add(taskDetailEntity);
                                    }
                                });
                            }
                        }
                        taskDetailDao.updateTaskDetailBath(taskDetailEntities);
                        SendMessageUtil.send(ChannelEnum.CSTM.getDesc(),DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"),"主场馆",doSnatchInfo.getAccount(),String.join(",",doSnatchInfo.getIdNameMap().values()));
                        WebSocketServer.sendInfo(socketMsg("抢票成功", String.valueOf(nameIDMap.values()), 5000), doSnatchInfo.getCreator());
                    }
                    /*if (!ObjectUtils.isEmpty(bodyJson) && bodyJson.getIntValue("code") == 550) {
                        if(bodyJson.getString("msg").contains("已有订单")){
                            doneList.forEach(idCard->{
                                if(!bodyJson.getString("msg").contains(idCard)){
                                    WebSocketServer.sendInfo(socketMsg("抢票失败", bodyJson.getString("msg"), 0), null);
                                }
                            });
                        }
                    }*/
                    runTaskCache.remove(taskId);
                    try {
                        Files.delete(Paths.get(sliderImageName));
                        Files.delete(Paths.get(backImageName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                        if(!msgCache.containsKey(doSnatchInfo.getTaskId())) {
                            //WebSocketServer.sendInfo(socketMsg("抢票异常", "账号:" + doSnatchInfo.getAccount() + "."+getCheckImageJson.getString("msg"), 0), null);
                        }
                        msgCache.put(doSnatchInfo.getTaskId(),true);
                }
            }
        } catch (Exception e) {
            runTaskCache.remove(taskId);
            log.info("科技馆抢票异常:{}",e);
        }
        runTaskCache.remove(taskId);
    }


    @Override
    public ServiceResponse<String> pay(PlaceOrderInfo placeOrderInfo) {
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
        if (ObjectUtils.isEmpty(placeOrderInfo.getOrderId())) {
            JSONObject placeOrderRes = new JSONObject();
            JSONObject param = new JSONObject();
            param.put("childTicketNum", placeOrderInfo.getChildTicketNum());
            param.put("date", DateUtil.format(placeOrderInfo.getDate(), "yyyy-MM-dd"));
            param.put("phone", placeOrderInfo.getLoginPhone());
            param.put("platform", 1);
            param.put("poolFlag", 1);
            param.put("realNameFlag", 1);
            param.put("saleMode", 1);
            param.put("ticketInfoList", placeOrderInfo.getTicketInfoList());
            param.put("ticketNum", placeOrderInfo.getTicketInfoList().size());
            param.put("useTicketType", 1);
            HttpEntity entity = new HttpEntity<>(param, headers);
            ResponseEntity<JSONObject> exchange = restTemplate.exchange(placeOrderUrl, HttpMethod.POST, entity, JSONObject.class);
            log.info("提交订单结果:{}", exchange.getBody());
            placeOrderRes = exchange.getBody();
            if (!ObjectUtils.isEmpty(placeOrderRes)) {
                if(placeOrderRes.getIntValue("code")!=200){
                    return ServiceResponse.createByErrorMessage(placeOrderRes.getString("msg"));
                }
                JSONObject orderData = placeOrderRes.getJSONObject("data");
                long orderId = orderData.getLongValue("orderId");
                String orderNumber = orderData.getString("orderNumber");
                Integer needChargeCode = orderData.getInteger("needChargeCode");
                List<TaskDetailEntity> updates = new ArrayList<>();
                placeOrderInfo.getTaskDetailIds().forEach(o -> {
                    TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
                    taskDetailEntity.setId(o);
                    taskDetailEntity.setUpdateDate(new Date());
                    taskDetailEntity.setOrderId(orderId);
                    taskDetailEntity.setPayment(needChargeCode != 1);
                    updates.add(taskDetailEntity);
                });
                taskDetailDao.updateTaskDetailBath(updates);
                if (needChargeCode != 1) {
                    return null;
                }
                JSONObject payParam = new JSONObject();
                payParam.put("id", orderId);
                payParam.put("payType", 0);
                HttpEntity payEntity = new HttpEntity<>(payParam, headers);
                ResponseEntity<JSONObject> payResEntity = restTemplate.exchange(wxPayForPcUrl, HttpMethod.POST, payEntity, JSONObject.class);
                JSONObject payRes = payResEntity.getBody();
                log.info("获取支付url结果:{}", payRes);
                if (!ObjectUtils.isEmpty(payRes) && payRes.getIntValue("code") == 200) {
                    return ServiceResponse.createBySuccess(payRes.getString("data"));
                }
            }
        }else {
            JSONObject payParam = new JSONObject();
            payParam.put("id",placeOrderInfo.getOrderId());
            payParam.put("payType", 0);
            HttpEntity payEntity = new HttpEntity<>(payParam, headers);
            ResponseEntity<JSONObject> payResEntity = restTemplate.exchange(wxPayForPcUrl, HttpMethod.POST, payEntity, JSONObject.class);
            JSONObject payRes = payResEntity.getBody();
            log.info("获取支付url结果:{}", payRes);
            if (!ObjectUtils.isEmpty(payRes) && payRes.getIntValue("code") == 200) {
                return ServiceResponse.createBySuccess(payRes.getString("data"));
            }else{
                return ServiceResponse.createByErrorMessage(payRes.getString("msg"));
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
    public Object buildParam(String captchaToken, Integer childTicketNum, String point, String hallScheduleId, Date useDate, Integer priceId, Integer childrenPriceId, Integer discountPriceId, Integer olderTicketPriceId, String phone, Map<String, String> iDMap) {
        JSONObject param = new JSONObject();
        param.put("captchaToken", captchaToken);
        param.put("childTicketNum", childTicketNum);
        param.put("date", DateUtil.format(useDate, "yyyy-MM-dd"));
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
            int ageForIdCard = getAgeForIdcard(entry.getKey());
            JSONObject ticketInfo = new JSONObject();
            ticketInfo.put("certificate", 1);
            ticketInfo.put("certificateInfo", entry.getKey());
            ticketInfo.put("cinemaFlag", 0);
            ticketInfo.put("hallId", 1);
            ticketInfo.put("hallScheduleId", Integer.valueOf(hallScheduleId));
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
            ticketInfo.put("useDate", DateUtil.format(useDate, "yyyy-MM-dd HH:mm:ss"));
            ticketInfo.put("userName", entry.getValue());
            ticketInfoList.add(ticketInfo);
        }
        param.put("ticketInfoList", ticketInfoList);
        return param;
    }

    private JSONObject buildPlaceOrderParam(Integer childTicketNum, String useDate, String phone, List<Long> ticketList) {
        JSONObject param = new JSONObject();
        param.put("childTicketNum", childTicketNum);
        param.put("date", useDate);
        param.put("phone", phone);
        param.put("platform", 1);
        param.put("poolFlag", 1);
        param.put("realNameFlag", 1);
        param.put("saleMode", 1);
        JSONArray ticketInfoList = new JSONArray();
        for (Long ticketId : ticketList) {
            JSONObject ticketInfo = new JSONObject();
            ticketInfo.put("id", ticketId);
            ticketInfoList.add(ticketInfo);
        }
        param.put("ticketInfoList", ticketInfoList);
        param.put("ticketNum", ticketList.size());
        param.put("useTicketType", 1);
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


    private HttpHeaders getHeader(String auth){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authority", "pcticket.cstm.org.cn");
        headers.set("accept", "application/json");
        headers.set("authorization", auth);
        headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        return headers;
    }
}
