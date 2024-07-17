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
        AccountInfoEntity accountInfoEntity = null;
        if (ObjectUtils.isEmpty(userInfoId)) {
            String phoneNo = VirtualPhoneUtil.getPhoneNo();
            AccountInfoEntity account = new AccountInfoEntity();
            account.setUserName("三方号" + phoneNo);
            account.setAccount(phoneNo);
            account.setChannel(ChannelEnum.CSTM.getCode());
            account.setCreator(taskInfo.getCreator());
            account.setYn(false);
            account.setStatus(false);
            account.setCreateDate(new Date());
            Integer integer = accountInfoDao.insertOrUpdate(account);
            if (integer > 0) {
                accountInfoEntity = account;
                taskEntity.setUserInfoId(account.getId());
                taskEntity.setAccount(phoneNo);
                //CompletableFuture.runAsync(() -> updateVerPhoneAuth(phoneNo));
            } else {
                return ServiceResponse.createByErrorMessage("保存购票账号异常");
            }
        } else {
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
            List<String> checkRes = checkUserRepeat(taskInfo.getUseDate(), taskInfo.getUserList());
            if(!ObjectUtils.isEmpty(checkRes)){
                return ServiceResponse.createByErrorMessage("以下用户已存在抢票任务:"+String.join(",",checkRes));
            }
            taskEntity.setCreateDate(new Date());
            taskEntity.setAuth(taskInfo.getAuth());
            taskEntity.setUserInfoId(accountInfoEntity.getId());
            taskEntity.setTaskName(taskInfo.getTaskName());
            Integer insert = taskDao.insert(taskEntity);
            if (insert > 0) {
                List<TaskDetailEntity> userList = taskInfo.getUserList();
                if (ObjectUtils.isEmpty(userList)) {
                    return ServiceResponse.createByErrorMessage("详情数据不能为空");
                }
                TaskEntity query = new TaskEntity();
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
            //redisService.setData(RedisKeyEnum.TASK.getCode() + taskEntity.getId(), JSON.toJSONString(taskEntity));
            if (insert > 0) {
                List<TaskDetailEntity> all = taskDetailDao.selectByTaskId(taskEntity.getId());
                List<TaskDetailEntity> userList = taskInfo.getUserList();
                List<TaskDetailEntity> addList = userList.stream().filter(o -> o.getId() == null).collect(Collectors.toList());
                List<String> checkRes = checkUserRepeat(taskInfo.getUseDate(),addList);
                if(!ObjectUtils.isEmpty(checkRes)){
                    return ServiceResponse.createByErrorMessage("以下用户已存在抢票任务:"+String.join(",",checkRes));
                }
                List<TaskDetailEntity> updateList = userList.stream().filter(o -> o.getId() != null).collect(Collectors.toList());
                List<TaskDetailEntity> deleteList = new ArrayList<>();
                List<Long> taskDetailIds = updateList.stream().map(TaskDetailEntity::getId).collect(Collectors.toList());
                if (updateList.size() != all.size()) {
                    List<String> taskDetailIdList = new ArrayList<>();
                    all.forEach(allEntity -> {
                        //redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + allEntity.getId(), JSON.toJSONString(allEntity));
                        taskDetailIdList.add(String.valueOf(allEntity.getId()));
                        if (!taskDetailIds.contains(allEntity.getId())) {
                            deleteList.add(allEntity);
                            //redisService.deleteKey(RedisKeyEnum.TASKDETAIL.getCode() + allEntity.getId());
                            //redisService.setData(RedisKeyEnum.RELATION.getCode() + taskEntity.getId(), String.valueOf(allEntity.getId()));
                        }
                    });
                    //redisService.saveList(RedisKeyEnum.RELATION.getCode() + taskEntity.getId(), taskDetailIdList);
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
                    addList.forEach(o -> {
                        list.add(String.valueOf(o.getId()));
                        //redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + o.getId(), JSON.toJSONString(o));
                    });
                    //redisService.saveList(RedisKeyEnum.RELATION.getCode() + taskEntity.getId(), list);
                }
                if (!ObjectUtils.isEmpty(updateList)) {
                    taskDetailDao.updateTaskDetailBath(updateList);
                    /*updateList.forEach(o -> {
                        redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + o.getId(), JSON.toJSONString(o));
                    });*/
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
                    redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + taskDetailEntity.getId(), JSON.toJSONString(taskDetailEntity));
                } else {
                    failTicket.add(taskDetailEntity.getUserName());
                }
            }
        }
        redisService.setData(RedisKeyEnum.TASK.getCode() + initTaskParam.getTaskId(), JSON.toJSONString(targetTask));
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
                taskInfoListResponse.setAuthorization(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getHeaders());
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
            TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
            taskDetailEntity.setYn(yn);
            taskDetailEntity.setTaskId(taskId);
            Integer res = taskDetailDao.updateEntityByTaskId(taskDetailEntity);
            if (res > 0) {
                List<TaskDetailEntity> taskDetailEntityList = taskDetailDao.selectByTaskId(taskId);
                taskDetailEntityList.forEach(o -> {
                    redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + o.getId(), JSON.toJSONString(o));
                });
                TaskEntity queryTask = taskDao.queryTask(taskEntity);
                redisService.setData(RedisKeyEnum.TASK.getCode() + queryTask.getId(), JSON.toJSONString(queryTask));
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
            TaskEntity entity = taskEntities.get(i);
            ProxyInfo proxyInfo = ObjectUtils.isEmpty(xieQuProxy) ? null : xieQuProxy.get(i);
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
                doSnatchInfo.setIp(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getIp());
                doSnatchInfo.setPort(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getPort());
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
            if (taskEntity.getChannel() == ChannelEnum.CSTM.getCode()
                    && ObjectUtil.equals(DateUtils.localDateToDate(snatchDate), taskEntity.getUseDate())
                    && !taskEntity.getDone()
                    && !taskEntity.getYn()) {
                List<String> taskDetailIds = redisService.getList(RedisKeyEnum.RELATION.getCode() + taskEntity.getId());
                List<List<String>> partition = Lists.partition(taskDetailIds, 5);
                for (List<String> item : partition) {
                    DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
                    Map<String, String> idNameMap = new HashMap<>();
                    List<Long> detailIds = new ArrayList<>();
                    for (String o : item) {
                        String taskDetailStr = redisService.getData(RedisKeyEnum.TASKDETAIL.getCode() + o);
                        TaskDetailEntity taskDetailEntity = JSON.parseObject(taskDetailStr, TaskDetailEntity.class);
                        if (!taskDetailEntity.getDone() || taskDetailEntity.getYn()) {
                            continue;
                        }
                        detailIds.add(Long.valueOf(o));
                        idNameMap.put(taskDetailEntity.getIDCard(), taskDetailEntity.getUserName());
                    }
                    if (ObjectUtils.isEmpty(detailIds)) {
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
            TaskEntity entity = allUnDoneTasks.get(i);
            ProxyInfo proxyInfo = ObjectUtils.isEmpty(xieQuProxy) ? null : xieQuProxy.get(i);
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
                doSnatchInfo.setIp(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getIp());
                doSnatchInfo.setPort(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getPort());
                doSnatchInfo.setCreator(entity.getCreator());
                doSnatchInfo.setTaskId(entity.getId());
                doSnatchInfo.setUserId(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getChannelUserId() == null ? null : Long.valueOf(accountInfoEntity.getChannelUserId()));
                doSnatchInfo.setAccount(entity.getAccount());
                doSnatchInfo.setAuthorization(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getHeaders());
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
        if (integer > 0) {
            TaskDetailEntity res = taskDetailDao.selectByTaskDetailId(taskDetailEntity.getId());
            redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + res.getId(), JSON.toJSONString(res));
        }
        return integer > 0;
    }

    @Override
    public void snatchingTicket(DoSnatchInfo doSnatchInfo) {
        Map<String, String> nameIDMap = doSnatchInfo.getIdNameMap();
        RestTemplate restTemplate = TemplateUtil.xieQuTunnelTemp();
        //RestTemplate restTemplate = TemplateUtil.initSSLTemplateWithProxyTunnelAuth();
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
                log.info("账号：{}下游客：{},提交订单结果：{}", doSnatchInfo.getAccount(), doSnatchInfo.getIdNameMap().values(), bodyJson);
                if (!ObjectUtils.isEmpty(bodyJson) && (bodyJson.getIntValue("code") == 550 || bodyJson.getIntValue("code") == 503)) {
                    log.info("提交订单异常！账号：{}下游客：{},提交订单结果：{}", doSnatchInfo.getAccount(), doSnatchInfo.getIdNameMap().values(), bodyJson);
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
                    //查询个人订单
                    headers.set("Referer", "https://pcticket.cstm.org.cn/personal/car");
                    HttpEntity searchEntity = new HttpEntity(headers);
                    JSONObject searchBodyJson = getOrderDetail(searchEntity);
                    if (searchBodyJson == null || searchBodyJson.getIntValue("code") != 200) {
                        SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"), "主场馆", doSnatchInfo.getAccount(), "任务成功，更新任务数据失败请求检查" + String.join(",", doSnatchInfo.getIdNameMap().values()));
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
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResponse.createByErrorMessage("获取二维码异常，请重新支付");
        }
        return null;
    }

    @Override
    public void updateAuth(String phone) {
        updateVerPhoneAuth(phone);
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
    public static Double getPoint(String backImagePath, String sliderImagePath, String uid) {
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

    private JSONObject getCheckImag(DoSnatchInfo doSnatchInfo) {
        int retryCount = 0;
        JSONObject response=new JSONObject();
        while (retryCount < 3) {
            try {
                HttpEntity entity = new HttpEntity(getHeader(doSnatchInfo.getAuthorization()));
                response = TemplateUtil.getResponse(TemplateUtil.xieQuTunnelTemp(), getCheckImagUrl, HttpMethod.GET, entity);
                //response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplateWithProxyTunnelAuth(), getCheckImagUrl, HttpMethod.GET, entity);
                if (!ObjectUtils.isEmpty(response) && response.getIntValue("code") == 200) {
                    log.info("账号:{}获取到提单验证码成功", doSnatchInfo.getAccount());
                    return response;
                }
                log.info("账号:{}获取提单验证码失败{}，重试中", doSnatchInfo.getAccount(), response);
            } catch (Exception e) {
                //e.printStackTrace();
                log.info("账号:{}获取提单验证码异常，涉及游客", String.join(",", doSnatchInfo.getIdNameMap().values()));
            }
            retryCount++;
        }
        return null;
    }

    private JSONObject getOrderDetail(HttpEntity searchEntity) {
        int retryCount = 0;
        while (retryCount < 10) {
            try {
                ResponseEntity<String> searchResEntity = restTemplate.exchange(getShoppingCart, HttpMethod.GET, searchEntity, String.class);
                JSONObject searchBodyJson = JSON.parseObject(searchResEntity.getBody());
                return searchBodyJson;
            } catch (Exception e) {
                log.info("获取购物车数据异常，重试次数: {}", (retryCount + 1));
            }
            retryCount++;
        }
        return null;
    }

    /**
     * 检查同一天内用户是否重复
     * @param date
     * @param taskDetailEntities
     * @return
     */
    private List<String> checkUserRepeat(Date date,List<TaskDetailEntity> taskDetailEntities){
        TaskEntity taskEntity=new TaskEntity();
        taskEntity.setYn(false);
        taskEntity.setUseDate(date);
        taskEntity.setChannel(ChannelEnum.CSTM.getCode());
        List<TaskEntity> taskEntityList = taskDao.fuzzyQuery(taskEntity);
        List<String> ids=new ArrayList<>();
        taskEntityList.forEach(entity->{
            List<TaskDetailEntity> taskDetailEntityList = taskDetailDao.selectByTaskId(entity.getId());
            List<String> collect = taskDetailEntityList.stream().map(TaskDetailEntity::getIDCard).collect(Collectors.toList());
            ids.addAll(collect);
        });
        List<String> res=new ArrayList<>();
        for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
            if(ids.contains(taskDetailEntity.getIDCard())){
                res.add(taskDetailEntity.getUserName());
            }
        }
        return res;
    }

    private void updateVerPhoneAuth(String phoneNum) {
        try {
            LogInCSTMParam sourceParam = new LogInCSTMParam();
            for (int i = 0; i < 10; i++) {
                ServiceResponse<LogInCSTMParam> captchaImage = loginService.getCaptchaImage();
                if (captchaImage.getStatus() == 0) {
                    log.info("获取渠道图片验证码成功");
                    LogInCSTMParam data = captchaImage.getData();
                    data.setPhone(phoneNum);
                    String captchaImageBase64 = data.getCaptchaImageBase64();
                    String code = null;
                    //重试3次
                    for (int j = 0; j < 3; j++) {
                        String verCode = ImageUtils.getVerCode(captchaImageBase64);
                        if (!ObjectUtils.isEmpty(verCode)) {
                            code = verCode;
                            break;
                        }
                    }
                    if (ObjectUtils.isEmpty(code)) {
                        continue;
                    }
                    data.setCaptchaImage(code);
                    ServiceResponse sendMsgCodeRes = loginService.sendMessageCode(data);
                    if (sendMsgCodeRes.getStatus() == 0) {
                        log.info("发送渠道短信验证码成功");
                        sourceParam = data;
                        break;
                    }
                }
                log.info("第{}重试发送短信验证码", i);
            }
            String msgCode = null;
            //等待100秒
            for (int i = 0; i < 10; i++) {
                Thread.sleep(10000);
                String verificationCode = VirtualPhoneUtil.getVerificationCode(phoneNum);
                if (!ObjectUtils.isEmpty(verificationCode)) {
                    msgCode = verificationCode;
                    break;
                }
            }
            if (ObjectUtils.isEmpty(msgCode)) {
                SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), null, null, phoneNum, "获取渠道短信验证码异常!");
                return;
            }
            sourceParam.setVerificationCode(msgCode);
            ServiceResponse login = loginService.login(sourceParam);
            if (login.getStatus() != 0) {
                SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), null, null, phoneNum, "登录异常");
                return;
            }
            log.info("账号{}登录成功成功", sourceParam.getPhone());
        } catch (Exception e) {
            log.info("获取手机号异常:{}", e);
        }

    }


    public static void main(String[] args) {
        String jigsawImageBase64 = "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";
        String originalImageBase64 = "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";
        System.out.println(ImageUtils.mergeBase64Images(jigsawImageBase64,originalImageBase64));
        String imageUuid = UUID.randomUUID().toString();
        String sliderImageName = "." + File.separator + imageUuid + "_" + "slider.png";
        String backImageName = "." + File.separator + imageUuid + "_" + "back.png";
        ImageUtils.imagCreate(jigsawImageBase64, sliderImageName, 155, 47);
        ImageUtils.imagCreate(originalImageBase64, backImageName, 155, 310);
        //图片验证码处理
        Double x = getPoint(sliderImageName, backImageName, imageUuid);
        System.out.println(x);
    }
}
