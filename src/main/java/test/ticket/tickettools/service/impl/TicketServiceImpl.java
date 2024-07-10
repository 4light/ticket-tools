package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
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
import test.ticket.tickettools.domain.constant.RedisKeyEnum;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.domain.entity.UserEntity;
import test.ticket.tickettools.service.*;
import test.ticket.tickettools.utils.*;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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


    private String getPlaceMuUserInfoUrl = "https://lotswap.dpm.org.cn/lotsapi/leaguer/api/userLeaguer/manage/leaguerInfo?cipherText=0&merchantId=2655&merchantInfoId=2655";
    private String getChnMuUserInfoUrl = "https://uu.chnmuseum.cn/prod-api/getUserInfoToIndividual2Mini?p=wxmini";


    private static List<String> doneList = new ArrayList<>();
    private static Map<Long, Object> runTaskCache = new ConcurrentHashMap<>();
    private static Map<Long, Object> msgCache = new ConcurrentHashMap<>();

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
    @Resource
    LoginService loginService;
    @Resource
    RedisService redisService;


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
        Long userInfoId = taskInfo.getUserInfoId();
        AccountInfoEntity accountInfoEntity=null;
        if(ObjectUtils.isEmpty(userInfoId)){
            String phoneNo = VirtualPhoneUtil.getPhoneNo();
            AccountInfoEntity account=new AccountInfoEntity();
            account.setUserName("三方号"+phoneNo);
            account.setAccount(phoneNo);
            account.setChannel(ChannelEnum.CSTM.getCode());
            account.setCreator(taskInfo.getCreator());
            account.setYn(false);
            account.setStatus(false);
            account.setCreateDate(new Date());
            accountInfoDao.insertOrUpdate(account);
            accountInfoEntity=account;
            taskEntity.setUserInfoId(account.getId());
            taskEntity.setAccount(phoneNo);
            CompletableFuture.runAsync(()->updateVerPhoneAuth(phoneNo));
        }else{
            accountInfoEntity = accountInfoDao.selectById(userInfoId);
        }
        BeanUtils.copyProperties(taskInfo, taskEntity);
        if (ObjectUtils.isEmpty(accountInfoEntity) || accountInfoEntity.getStatus()) {
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
                TaskEntity query=new TaskEntity();
                query.setId(taskEntity.getId());
                //redisService.setData(RedisKeyEnum.TASK.getCode()+taskEntity.getId(),JSON.toJSONString(taskDao.queryTask(query)));
                userList.forEach(o -> {
                    o.setTaskId(taskEntity.getId());
                    o.setPayment(false);
                    o.setCreateDate(new Date());
                });
                Integer res = taskDetailDao.insertBatch(userList);
                if (res == userList.size()) {
                    /*List<TaskDetailEntity> taskDetailEntityList = taskDetailDao.selectByTaskId(taskEntity.getId());
                    List<String> taskDetailIds=new ArrayList<>();
                    for (TaskDetailEntity taskDetailEntity : taskDetailEntityList) {
                        redisService.setData(RedisKeyEnum.TASKDETAIL.getCode()+taskDetailEntity.getId(),JSON.toJSONString(taskDetailEntity));
                        taskDetailIds.add(String.valueOf(taskDetailEntity.getId()));
                    }
                    redisService.saveList(RedisKeyEnum.RELATION.getCode()+taskEntity.getId(),taskDetailIds);*/
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
            taskEntity.setUserInfoId(accountInfoEntity.getId());
            Integer insert = taskDao.updateTask(taskEntity);
            redisService.setData(RedisKeyEnum.TASK.getCode()+taskEntity.getId(),JSON.toJSONString(taskEntity));
            if (insert > 0) {
                List<TaskDetailEntity> all = taskDetailDao.selectByTaskId(taskEntity.getId());
                List<TaskDetailEntity> userList = taskInfo.getUserList();
                List<TaskDetailEntity> addList = userList.stream().filter(o -> o.getId() == null).collect(Collectors.toList());
                List<TaskDetailEntity> updateList = userList.stream().filter(o -> o.getId() != null).collect(Collectors.toList());
                List<TaskDetailEntity> deleteList = new ArrayList<>();
                List<Long> taskDetailIds = updateList.stream().map(TaskDetailEntity::getId).collect(Collectors.toList());
                if (updateList.size() != all.size()) {
                    List<String> taskDetailIdList=new ArrayList<>();
                    all.forEach(allEntity -> {
                        redisService.setData(RedisKeyEnum.TASKDETAIL.getCode()+allEntity.getId(),JSON.toJSONString(allEntity));
                        taskDetailIdList.add(String.valueOf(allEntity.getId()));
                        if (!taskDetailIds.contains(allEntity.getId())) {
                            deleteList.add(allEntity);
                            redisService.deleteKey(RedisKeyEnum.TASKDETAIL.getCode()+allEntity.getId());
                            redisService.setData(RedisKeyEnum.RELATION.getCode()+taskEntity.getId(),String.valueOf(allEntity.getId()));
                        }
                    });
                    redisService.saveList(RedisKeyEnum.RELATION.getCode()+taskEntity.getId(), taskDetailIdList);
                    if (deleteList.size() > 0) {
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
                    List<String> list = redisService.getList(RedisKeyEnum.RELATION.getCode() + taskEntity.getId());
                    addList.forEach(o->{
                        list.add(String.valueOf(o.getId()));
                        redisService.setData(RedisKeyEnum.TASKDETAIL.getCode()+o.getId(),JSON.toJSONString(o));
                    });
                    redisService.saveList(RedisKeyEnum.RELATION.getCode() + taskEntity.getId(),list);
                }
                if (!ObjectUtils.isEmpty(updateList)) {
                    taskDetailDao.updateTaskDetailBath(updateList);
                    updateList.forEach(o->{
                        redisService.setData(RedisKeyEnum.TASKDETAIL.getCode()+o.getId(),JSON.toJSONString(o));
                    });
                }
                return ServiceResponse.createBySuccess();
            }
        }
        return ServiceResponse.createByErrorMessage("保存任务异常");
    }

    @Override
    public ServiceResponse initTask(InitTaskParam initTaskParam) {
        String cancelTicketUrl = "https://pcticket.cstm.org.cn/prod-api/order/ticketInfo/removeForShopping/";
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setDone(false);
        taskEntity.setId(initTaskParam.getTaskId());
        taskEntity.setUpdateDate(new Date());
        taskDao.updateTask(taskEntity);
        TaskEntity targetTask = taskDao.selectByPrimaryKey(initTaskParam.getTaskId());
        List<TaskDetailEntity> taskDetailEntityList = initTaskParam.getTaskDetailEntityList();
        TaskEntity queryEntity = new TaskEntity();
        queryEntity.setId(initTaskParam.getTaskId());
        TaskEntity currentTask = taskDao.queryTask(queryEntity);
        AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(currentTask.getUserInfoId());
        HttpHeaders headers = getHeader(accountInfoEntity.getHeaders());
        RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
        HttpEntity entity = new HttpEntity(headers);
        List<String> failTicket = new ArrayList<>();
        for (TaskDetailEntity taskDetailEntity : taskDetailEntityList) {
            Long ticketId = taskDetailEntity.getTicketId();
            Long id = taskDetailEntity.getId();
            if (!ObjectUtils.isEmpty(ticketId) && ticketId != 0 && !ObjectUtils.isEmpty(id)) {
                JSONObject response = TemplateUtil.getResponse(restTemplate, cancelTicketUrl + ticketId, HttpMethod.DELETE, entity);
                if (!ObjectUtils.isEmpty(response) && response.getIntValue("code") == 200) {
                    log.info("删除购物车订单结果:{}", response);
                    taskDetailEntity.setPrice(null);
                    taskDetailEntity.setDone(false);
                    //successEntities.add(taskDetailEntity);
                    taskDetailDao.updateTaskDetail(taskDetailEntity);
                    redisService.setData(RedisKeyEnum.TASKDETAIL.getCode()+taskDetailEntity.getId(), JSON.toJSONString(taskDetailEntity));
                } else {
                    failTicket.add(taskDetailEntity.getUserName());
                }
            }
        }
        redisService.setData(RedisKeyEnum.TASK.getCode()+initTaskParam.getTaskId(), JSON.toJSONString(targetTask));
        if (ObjectUtils.isEmpty(failTicket)) {
            return ServiceResponse.createBySuccessMessgge("重置成功");
        }
        return ServiceResponse.createByErrorMessage("以下人员重置失败:" + String.join(",", failTicket));
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
        if (!StrUtil.equals("admin", byUsername.getRole())) {
            query.setCreator(queryTaskInfo.getCreator());
        }
        List<TaskEntity> taskEntities = taskDao.fuzzyQuery(query);
        List<TaskInfoListResponse> list = new ArrayList<>();
        for (TaskEntity taskEntity : taskEntities) {
            Long id = taskEntity.getId();
            Long userInfoId = taskEntity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
            TaskDetailEntity queryEntity = new TaskDetailEntity();
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
                taskInfoListResponse.setAuthorization(ObjectUtils.isEmpty(accountInfoEntity) ? null:accountInfoEntity.getHeaders());
                //使用名字好区分
                taskInfoListResponse.setAccountName(accountInfoEntity == null ? null : accountInfoEntity.getUserName());
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
    public ServiceResponse<TaskInfo> getTask(Long taskId, Boolean yn) {
        TaskInfo taskInfo = new TaskInfo();
        TaskEntity query = new TaskEntity();
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
    public ServiceResponse delete(Long taskId, Boolean yn) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(taskId);
        taskEntity.setYn(yn);
        Integer integer = taskDao.updateTask(taskEntity);
        if (integer > 0) {
            TaskDetailEntity taskDetailEntity=new TaskDetailEntity();
            taskDetailEntity.setYn(yn);
            taskDetailEntity.setTaskId(taskId);
            Integer res = taskDetailDao.updateEntityByTaskId(taskDetailEntity);
            if (res > 0) {
                List<TaskDetailEntity> taskDetailEntityList = taskDetailDao.selectByTaskId(taskId);
                taskDetailEntityList.forEach(o->{
                    redisService.setData(RedisKeyEnum.TASKDETAIL.getCode()+o.getId(),JSON.toJSONString(o));
                });
                TaskEntity queryTask = taskDao.queryTask(taskEntity);
                redisService.setData(RedisKeyEnum.TASK.getCode()+queryTask.getId(),JSON.toJSONString(queryTask));
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
    public List<DoSnatchInfo> getTaskForRun() {
        List<DoSnatchInfo> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate snatchDate = now.plusDays(7L);
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.CSTM.getCode());
        taskEntity.setUseDate(DateUtils.localDateToDate(snatchDate));
        List<TaskEntity> taskEntities = taskDao.getUnDoneTasks(taskEntity);
        if (ObjectUtils.isEmpty(taskEntities)) {
            return result;
        }
        List<ProxyInfo> xieQuProxy = ProxyUtil.getXieQuProxy(taskEntities.size());
        for (int i = 0; i < taskEntities.size(); i++) {
            TaskEntity entity=taskEntities.get(i);
            ProxyInfo proxyInfo = ObjectUtils.isEmpty(xieQuProxy)?null:xieQuProxy.get(i);
            Long id = entity.getId();
            Long userInfoId = entity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
            TaskDetailEntity query = new TaskDetailEntity();
            query.setTaskId(entity.getId());
            query.setDone(false);
            query.setYn(false);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(query);
            if (ObjectUtils.isEmpty(taskDetailEntities)) {
                entity.setDone(true);
                taskDao.updateTask(entity);
                continue;
            }
            List<List<TaskDetailEntity>> partition = Lists.partition(taskDetailEntities, 5);
            for (List<TaskDetailEntity> taskDetailEntityList : partition) {
                DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
                List<Long> taskDetailIds = taskDetailEntityList.stream()
                        .map(TaskDetailEntity::getId) // 提取每个对象的 ID
                        .collect(Collectors.toList());
                Map<String, String> idNameMap = taskDetailEntityList.stream()
                        .collect(Collectors.toMap(TaskDetailEntity::getIDCard, TaskDetailEntity::getUserName));
                doSnatchInfo.setTaskId(id);
                doSnatchInfo.setIp(ObjectUtils.isEmpty(proxyInfo)?null:proxyInfo.getIp());
                doSnatchInfo.setPort(ObjectUtils.isEmpty(proxyInfo)?null:proxyInfo.getPort());
                doSnatchInfo.setCreator(entity.getCreator());
                doSnatchInfo.setUserId(Long.valueOf(accountInfoEntity.getChannelUserId()));
                doSnatchInfo.setAccount(entity.getAccount());
                doSnatchInfo.setAuthorization(accountInfoEntity.getHeaders());
                doSnatchInfo.setSession(entity.getSession());
                doSnatchInfo.setUseDate(entity.getUseDate());
                doSnatchInfo.setTaskDetailIds(taskDetailIds);
                doSnatchInfo.setIdNameMap(idNameMap);
                result.add(doSnatchInfo);
            }
        }
        return result;
    }

    @Override
    public List<DoSnatchInfo> getTaskForRun1() {
        LocalDate now = LocalDate.now();
        LocalDate snatchDate = now.plusDays(7L);
        List<String> taskKeys = redisService.searchKey(RedisKeyEnum.TASK.getCode() + "[0-9]*");
        List<DoSnatchInfo> result = new ArrayList<>();
        for (String taskKey : taskKeys) {
            String taskStr = redisService.getData(taskKey);
            TaskEntity taskEntity = JSON.parseObject(taskStr, TaskEntity.class);
            String accountStr = redisService.getData(RedisKeyEnum.ACCOUNT.getCode() + taskEntity.getUserInfoId());
            AccountInfoEntity accountInfoEntity = JSON.parseObject(accountStr, AccountInfoEntity.class);
            if(taskEntity.getChannel()==ChannelEnum.CSTM.getCode()
                    && ObjectUtil.equals(DateUtils.localDateToDate(snatchDate),taskEntity.getUseDate())
                    &&!taskEntity.getDone()
                    &&!taskEntity.getYn()){
                List<String> taskDetailIds = redisService.getList(RedisKeyEnum.RELATION.getCode() + taskEntity.getId());
                List<List<String>> partition = Lists.partition(taskDetailIds, 5);
                for (List<String> item : partition) {
                    DoSnatchInfo doSnatchInfo=new DoSnatchInfo();
                    Map<String, String> idNameMap=new HashMap<>();
                    List<Long> detailIds=new ArrayList<>();
                    for (String o : item) {
                        String taskDetailStr = redisService.getData(RedisKeyEnum.TASKDETAIL.getCode() + o);
                        TaskDetailEntity taskDetailEntity = JSON.parseObject(taskDetailStr, TaskDetailEntity.class);
                        if (!taskDetailEntity.getDone()||taskDetailEntity.getYn()){
                            continue;
                        }
                        detailIds.add(Long.valueOf(o));
                        idNameMap.put(taskDetailEntity.getIDCard(),taskDetailEntity.getUserName());
                    }
                    if(ObjectUtils.isEmpty(detailIds)){
                        continue;
                    }
                    doSnatchInfo.setTaskId(taskEntity.getId());
                    doSnatchInfo.setCreator(taskEntity.getCreator());
                    doSnatchInfo.setUserId(Long.valueOf(accountInfoEntity.getChannelUserId()));
                    doSnatchInfo.setAccount(accountInfoEntity.getAccount());
                    doSnatchInfo.setAuthorization(accountInfoEntity.getHeaders());
                    doSnatchInfo.setSession(taskEntity.getSession());
                    doSnatchInfo.setUseDate(taskEntity.getUseDate());
                    doSnatchInfo.setTaskDetailIds(detailIds);
                    doSnatchInfo.setIdNameMap(idNameMap);
                    result.add(doSnatchInfo);
                }
            }
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
        List<ProxyInfo> xieQuProxy = ProxyUtil.getXieQuProxy(allUnDoneTasks.size());
        for (int i = 0; i < allUnDoneTasks.size(); i++) {
            TaskEntity entity=allUnDoneTasks.get(i);
            ProxyInfo proxyInfo =ObjectUtils.isEmpty(xieQuProxy)?null: xieQuProxy.get(i);
            Long userInfoId = entity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(userInfoId);
            TaskDetailEntity query = new TaskDetailEntity();
            query.setTaskId(entity.getId());
            query.setDone(false);
            query.setYn(false);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(query);
            if (ObjectUtils.isEmpty(taskDetailEntities)) {
                entity.setDone(true);
                taskDao.updateTask(entity);
            }
            for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
                DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
                doSnatchInfo.setIp(ObjectUtils.isEmpty(proxyInfo)?null:proxyInfo.getIp());
                doSnatchInfo.setPort(ObjectUtils.isEmpty(proxyInfo)?null:proxyInfo.getPort());
                doSnatchInfo.setCreator(entity.getCreator());
                doSnatchInfo.setTaskId(entity.getId());
                doSnatchInfo.setUserId(accountInfoEntity.getChannelUserId() == null ? null : Long.valueOf(accountInfoEntity.getChannelUserId()));
                doSnatchInfo.setAccount(entity.getAccount());
                doSnatchInfo.setAuthorization(accountInfoEntity.getHeaders());
                doSnatchInfo.setUseDate(entity.getUseDate());
                doSnatchInfo.setSession(entity.getSession());
                doSnatchInfo.setTaskDetailIds(Arrays.asList(taskDetailEntity.getId()));
                doSnatchInfo.setIdNameMap(new HashMap<String, String>() {{
                    put(taskDetailEntity.getIDCard(), taskDetailEntity.getUserName());
                }});
                result.add(doSnatchInfo);
            }
        }
        return result;
    }
    @Override
    public List<DoSnatchInfo> getAllTaskForRun1() {
        List<String> taskKeys = redisService.searchKey(RedisKeyEnum.TASK.getCode() + "[0-9]*");
        List<DoSnatchInfo> result = new ArrayList<>();
        for (String taskKey : taskKeys) {
            String taskStr = redisService.getData(taskKey);
            TaskEntity taskEntity = JSON.parseObject(taskStr, TaskEntity.class);
            String accountStr = redisService.getData(RedisKeyEnum.ACCOUNT.getCode() + taskEntity.getUserInfoId());
            AccountInfoEntity accountInfoEntity = JSON.parseObject(accountStr, AccountInfoEntity.class);
            if (taskEntity.getChannel() == ChannelEnum.CSTM.getCode() && !taskEntity.getDone() && !taskEntity.getYn()) {
                List<String> taskDetailIds = redisService.getList(RedisKeyEnum.RELATION.getCode() + taskEntity.getId());
                for (String taskDetailId : taskDetailIds) {
                    String taskDetailStr = redisService.getData(RedisKeyEnum.TASKDETAIL.getCode() + taskDetailId);
                    TaskDetailEntity taskDetailEntity = JSON.parseObject(taskDetailStr, TaskDetailEntity.class);
                    if (!taskDetailEntity.getDone() && !taskDetailEntity.getYn()) {
                        DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
                        doSnatchInfo.setCreator(taskEntity.getCreator());
                        doSnatchInfo.setTaskId(taskEntity.getId());
                        doSnatchInfo.setUserId(accountInfoEntity.getChannelUserId() == null ? null : Long.valueOf(accountInfoEntity.getChannelUserId()));
                        doSnatchInfo.setAccount(accountInfoEntity.getAccount());
                        doSnatchInfo.setAuthorization(accountInfoEntity.getHeaders());
                        doSnatchInfo.setUseDate(taskEntity.getUseDate());
                        doSnatchInfo.setSession(taskEntity.getSession());
                        doSnatchInfo.setTaskDetailIds(Arrays.asList(taskDetailEntity.getId()));
                        doSnatchInfo.setIdNameMap(new HashMap<String, String>() {{
                            put(taskDetailEntity.getIDCard(), taskDetailEntity.getUserName());
                        }});
                        result.add(doSnatchInfo);
                    }
                }
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
        if(integer>0){
            TaskDetailEntity res = taskDetailDao.selectByTaskDetailId(taskDetailEntity.getId());
            redisService.setData(RedisKeyEnum.TASKDETAIL.getCode()+res.getId(), JSON.toJSONString(res));
        }
        return integer > 0;
    }

    @Override
    public void snatchingTicket(DoSnatchInfo doSnatchInfo) {
        Map<String, String> nameIDMap = doSnatchInfo.getIdNameMap();
        RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
        try {
            HttpHeaders headers = getHeader(doSnatchInfo.getAuthorization());
            Long userId = doSnatchInfo.getUserId();
            String phone = doSnatchInfo.getAccount();
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
            /*for (Map.Entry<String, String> entry : nameIDMap.entrySet()) {
                HttpEntity addEntity = new HttpEntity<>(buildAddParam(entry.getKey(), entry.getValue(), userId), headers);
                //restTemplate.exchange(addUrl, HttpMethod.POST, addEntity, String.class);
                JSONObject response = TemplateUtil.getResponse(restTemplate, addUrl, HttpMethod.POST, addEntity);
                if (ObjectUtils.isEmpty(response) || response.getIntValue("code") != 200) {
                    List<Long> taskDetailIds = doSnatchInfo.getTaskDetailIds();
                    for (Long taskDetailId : taskDetailIds) {
                        TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
                        taskDetailEntity.setId(taskDetailId);
                        taskDetailEntity.setUpdateDate(new Date());
                        taskDetailEntity.setExt(ObjectUtils.isEmpty(response)?"添加用户异常":response.getString("msg"));
                        taskDetailDao.updateTaskDetail(taskDetailEntity);
                    }
                    if (!msgCache.containsKey(doSnatchInfo.getTaskId())) {
                        WebSocketServer.sendInfo(socketMsg("抢票异常", "账号:" + doSnatchInfo.getAccount() + response.getString("msg"), 0), doSnatchInfo.getCreator());
                        SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"), "账号：", doSnatchInfo.getAccount(), response.getString("msg"));
                    }
                    msgCache.put(doSnatchInfo.getTaskId(), true);
                    return;
                }
            }*/
            JSONObject getCheckImageJson = getCheckImag(doSnatchInfo);
            if (!ObjectUtils.isEmpty(getCheckImageJson) && getCheckImageJson.getIntValue("code") == 200) {
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
                HttpEntity shoppingCartUrlEntity = new HttpEntity<>(buildParam(token, childrenTicketNum == null ? 0 : childrenTicketNum, point, doSnatchInfo.getSession(), doSnatchInfo.getUseDate(), priceId, childrenPriceId, discountPriceId, olderPriceId, phone, nameIDMap), headers);
                JSONObject bodyJson = TemplateUtil.getResponse(restTemplate, shoppingCartUrl, HttpMethod.POST, shoppingCartUrlEntity);
                log.info("提交订单结果：{}", bodyJson);
                if (!ObjectUtils.isEmpty(bodyJson) && (bodyJson.getIntValue("code") == 550 || bodyJson.getIntValue("code") == 503)) {
                    log.info("提交订单异常！账号：{}下游客：{},提交订单结果：{}", doSnatchInfo.getAccount(),doSnatchInfo.getIdNameMap().values(),bodyJson);
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
                    /*List<Long> taskDetailIds = doSnatchInfo.getTaskDetailIds();
                    for (Long taskDetailId : taskDetailIds) {
                        String taskDetailStr = redisService.getData(RedisKeyEnum.TASKDETAIL.getCode() + taskDetailId);
                        TaskDetailEntity taskDetailEntity = JSON.parseObject(taskDetailStr, TaskDetailEntity.class);
                        taskDetailEntity.setDone(true);
                        redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + taskDetailId,JSON.toJSONString(taskDetailEntity));
                    }*/
                    SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"), "主场馆", doSnatchInfo.getAccount(), String.join(",", doSnatchInfo.getIdNameMap().values()));
                    msgCache.remove(doSnatchInfo.getTaskId());
                    log.info("账号：{}下游客：{},提交订单结果：{}", doSnatchInfo.getAccount(),doSnatchInfo.getIdNameMap().values(),bodyJson);
                    //查询个人订单
                    headers.set("Referer", "https://pcticket.cstm.org.cn/personal/car");
                    HttpEntity searchEntity = new HttpEntity(headers);
                    JSONObject searchBodyJson = getOrderDetail(searchEntity);
                    if (searchBodyJson == null || searchBodyJson.getIntValue("code") != 200) {
                        SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"), "主场馆", doSnatchInfo.getAccount(), "任务成功，更新任务数据失败请求检查"+String.join(",", doSnatchInfo.getIdNameMap().values()));
                        log.info("查询个人订单失败：{}", searchBodyJson);
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
                        taskDetailDao.updateTaskDetailBath(taskDetailEntities);
                    }
                    WebSocketServer.sendInfo(socketMsg("抢票成功", String.valueOf(nameIDMap.values()), 5000), doSnatchInfo.getCreator());
                }
                try {
                    Files.delete(Paths.get(sliderImageName));
                    Files.delete(Paths.get(backImageName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (!msgCache.containsKey(doSnatchInfo.getTaskId())) {
                    //WebSocketServer.sendInfo(socketMsg("抢票异常", "账号:" + doSnatchInfo.getAccount() + "."+getCheckImageJson.getString("msg"), 0), null);
                }
                msgCache.put(doSnatchInfo.getTaskId(), true);
            }
        } catch (Exception e) {
            log.info("科技馆抢票异常:{}", e);
        }
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
        try {
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
                log.info("购物车提交结果:{}", exchange.getBody());
                placeOrderRes = exchange.getBody();
                if (!ObjectUtils.isEmpty(placeOrderRes)) {
                    if (placeOrderRes.getIntValue("code") != 200) {
                        return ServiceResponse.createByErrorMessage(placeOrderRes.getString("msg"));
                    }
                    JSONObject orderData = placeOrderRes.getJSONObject("data");
                    long orderId = orderData.getLongValue("orderId");
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
                        return ServiceResponse.createBySuccess();
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
            } else {
                JSONObject payParam = new JSONObject();
                payParam.put("id", placeOrderInfo.getOrderId());
                payParam.put("payType", 0);
                HttpEntity payEntity = new HttpEntity<>(payParam, headers);
                ResponseEntity<JSONObject> payResEntity = restTemplate.exchange(wxPayForPcUrl, HttpMethod.POST, payEntity, JSONObject.class);
                JSONObject payRes = payResEntity.getBody();
                log.info("获取支付url结果:{}", payRes);
                if (!ObjectUtils.isEmpty(payRes) && payRes.getIntValue("code") == 200) {
                    return ServiceResponse.createBySuccess(payRes.getString("data"));
                } else {
                    return ServiceResponse.createByErrorMessage(payRes.getString("msg"));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return ServiceResponse.createByErrorMessage("获取二维码异常，请重新支付");
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


    private HttpHeaders getHeader(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authority", "pcticket.cstm.org.cn");
        headers.set("accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.set("authorization", auth);
        headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        return headers;
    }

    private JSONObject getCheckImag(DoSnatchInfo doSnatchInfo){
        int retryCount = 0;
        while (retryCount < 20) {
            try {
                HttpEntity entity=new HttpEntity(getHeader(doSnatchInfo.getAuthorization()));
                JSONObject response = TemplateUtil.getResponse(ObjectUtils.isEmpty(doSnatchInfo.getIp())?TemplateUtil.initSSLTemplate():TemplateUtil.xieQuTemp(doSnatchInfo.getIp(), doSnatchInfo.getPort()), getCheckImagUrl, HttpMethod.GET,entity);
                if (!ObjectUtils.isEmpty(response)&&response.getIntValue("code")==200) {
                    log.info("账号:{}获取到验证码成功",doSnatchInfo.getAccount());
                    return response;
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
            retryCount++;
        }
        return null;
    }

    private JSONObject getOrderDetail(HttpEntity searchEntity){
        int retryCount = 0;
        while (retryCount < 10) {
            try {
                ResponseEntity<String> searchResEntity = restTemplate.exchange(getShoppingCart, HttpMethod.GET, searchEntity, String.class);
                JSONObject searchBodyJson = JSON.parseObject(searchResEntity.getBody());
                return searchBodyJson;
            } catch (Exception e) {
                log.info("获取购物车数据异常，重试次数: {}" ,(retryCount + 1));
            }
            retryCount++;
        }
        return null;
    }

    private void updateVerPhoneAuth(String phoneNum){
        try{
            ServiceResponse<LogInCSTMParam> captchaImage = loginService.getCaptchaImage();
            if(captchaImage.getStatus()==0){
                LogInCSTMParam data = captchaImage.getData();
                data.setPhone(phoneNum);
                String captchaImageBase64 = data.getCaptchaImageBase64();
                String code=null;
                //重试3次
                for (int i = 0; i < 3; i++) {
                    String verCode = ImageUtils.getVerCode(captchaImageBase64);
                    if(!ObjectUtils.isEmpty(verCode)){
                        code=verCode;
                        break;
                    }
                }
                if(ObjectUtils.isEmpty(code)){
                    SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), null, null, phoneNum, "从三方获取图片验证码异常!");
                    return;
                }
                data.setCaptchaImage(code);
                ServiceResponse sendMsgCodeRes = loginService.sendMessageCode(data);
                if(sendMsgCodeRes.getStatus()!=0){
                    SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), null, null, phoneNum, "发送渠道短信验证码异常!");
                    return;
                }
                String msgCode=null;
                //等待100秒
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(10000);
                    String verificationCode = VirtualPhoneUtil.getVerificationCode(phoneNum);
                    if(!ObjectUtils.isEmpty(verificationCode)){
                        msgCode=verificationCode;
                        break;
                    }
                }
                if(ObjectUtils.isEmpty(msgCode)){
                    SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), null, null, phoneNum, "获取渠道短信验证码异常!");
                    return;

                }
                data.setVerificationCode(msgCode);
                ServiceResponse login = loginService.login(data);
                if(login.getStatus()!=0){
                    SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), null, null, phoneNum, "登录异常");
                    return;
                }
            }
        }catch (Exception e){
            log.info("获取手机号异常:{}",e);
        }

    }

    public static void main(String[] args) {
        HttpHeaders headers=new HttpHeaders();
        headers.set("authority", "pcticket.cstm.org.cn");
        headers.set("accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.set("authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6Ijk3NjY0ZDI4LWQ1OTQtNDRiMi1hZTYzLWU1OWJmMTY2NzMxNCJ9.Eg1P8iCWc5DPvZ3VVPRYF0xRLpfn1I-yUaQFGSyc1ZxPj3FXW3yHkOhv6p4OYolWoZbj720Tbiknktzeso3rsg");
        headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
       // for (int i = 0; i < 10; i++) {
            List<ProxyInfo> proxy = ProxyUtil.getProxyList(1);
            JSONObject response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplateWithProxyAuth(proxy.get(0).getIp(), proxy.get(0).getPort()), getCheckImagUrl, HttpMethod.GET, new HttpEntity(headers));
            System.out.println(response);
       // }
    }
}
