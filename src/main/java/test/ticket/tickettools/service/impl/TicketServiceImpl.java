package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import org.bytedeco.javacpp.BytePointer;
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
    RedisService redisService1;


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
        /*if (ObjectUtils.isEmpty(userInfoId)) {
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
        }*/
        if (!ObjectUtils.isEmpty(userInfoId)) {
            accountInfoEntity = accountInfoDao.selectById(userInfoId);
        }
        BeanUtils.copyProperties(taskInfo, taskEntity);
        if (!ObjectUtils.isEmpty(accountInfoEntity)) {
            if (accountInfoEntity.getStatus()) {
                return ServiceResponse.createByErrorMessage("购票账号未授权不能创建任务");
            }
        }
       /* if (accountInfoEntity != null) {
            taskEntity.setAccount(accountInfoEntity.getAccount());
            taskEntity.setPwd(accountInfoEntity.getPwd());
        }*/
        if (ObjectUtils.isEmpty(taskInfo.getId())) {
            List<String> checkRes = checkUserRepeat(taskInfo.getUseDate(), taskInfo.getUserList(), taskInfo.getChannel());
            if (!ObjectUtils.isEmpty(checkRes)) {
                return ServiceResponse.createByErrorMessage("以下用户已存在抢票任务:" + String.join(",", checkRes));
            }
            taskEntity.setCreateDate(new Date());
            taskEntity.setAuth(taskInfo.getAuth());
            taskEntity.setUserInfoId(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getId());
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
            taskEntity.setAccount(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getAccount());
            taskEntity.setPwd(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getPwd());
            taskEntity.setUserInfoId(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getId());
            Integer insert = taskDao.updateTask(taskEntity);
            //redisService.setData(RedisKeyEnum.TASK.getCode() + taskEntity.getId(), JSON.toJSONString(taskEntity));
            if (insert > 0) {
                List<TaskDetailEntity> all = taskDetailDao.selectByTaskId(taskEntity.getId());
                List<TaskDetailEntity> userList = taskInfo.getUserList();
                List<TaskDetailEntity> addList = userList.stream().filter(o -> o.getId() == null).collect(Collectors.toList());
                List<String> checkRes = checkUserRepeat(taskInfo.getUseDate(), addList, taskInfo.getChannel());
                if (!ObjectUtils.isEmpty(checkRes)) {
                    return ServiceResponse.createByErrorMessage("以下用户已存在抢票任务:" + String.join(",", checkRes));
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

                   /* List<String> list = redisService.getList(RedisKeyEnum.RELATION.getCode() + taskEntity.getId());
                    addList.forEach(o -> {
                        list.add(String.valueOf(o.getId()));
                        //redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + o.getId(), JSON.toJSONString(o));
                    });*/
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
        RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
        List<String> failTicket = new ArrayList<>();
        for (TaskDetailEntity taskDetailEntity : taskDetailEntityList) {
            HttpHeaders headers = getHeader(taskDetailEntity.getOrderCreatorAuth());
            HttpEntity entity = new HttpEntity(headers);
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
                    //redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + taskDetailEntity.getId(), JSON.toJSONString(taskDetailEntity));
                } else {
                    failTicket.add(taskDetailEntity.getUserName());
                }
            }
        }
        //redisService.setData(RedisKeyEnum.TASK.getCode() + initTaskParam.getTaskId(), JSON.toJSONString(targetTask));
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
            AccountInfoEntity accountInfoEntity = ObjectUtils.isEmpty(userInfoId) ? null : accountInfoDao.selectById(userInfoId);
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
                //taskInfoListResponse.setAccount(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getUserName());
                taskInfoListResponse.setId(taskDetailEntity.getId());
                taskInfoListResponse.setAuthorization(ObjectUtils.isEmpty(accountInfoEntity) ? taskDetailEntity.getOrderCreatorAuth() : accountInfoEntity.getHeaders());
                //使用名字好区分
                taskInfoListResponse.setAccountName(accountInfoEntity == null ? null : accountInfoEntity.getUserName());
                taskInfoListResponse.setTaskYn(taskEntity.getYn());
                taskInfoListResponse.setAccount(taskEntity.getChannel()==0?taskDetailEntity.getOrderCreatorAccount():taskEntity.getAccount());
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
                    //redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + o.getId(), JSON.toJSONString(o));
                });
                TaskEntity queryTask = taskDao.queryTask(taskEntity);
                //redisService.setData(RedisKeyEnum.TASK.getCode() + queryTask.getId(), JSON.toJSONString(queryTask));
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
        AccountInfoEntity queryAccount = new AccountInfoEntity();
        queryAccount.setChannel(ChannelEnum.CSTM.getCode());
        queryAccount.setYn(false);
        queryAccount.setCreator("system");
        List<AccountInfoEntity> accountInfoEntityList = accountInfoDao.selectByEntity(queryAccount);
        Collections.shuffle(accountInfoEntityList);
        List<ProxyInfo> xieQuProxy = ProxyUtil.getXieQuProxy(taskEntities.size());
        for (int i = 0; i < taskEntities.size(); i++) {
            TaskEntity entity = taskEntities.get(i);
            ProxyInfo proxyInfo = ObjectUtils.isEmpty(xieQuProxy) ? null : xieQuProxy.get(i);
            Long id = entity.getId();
            Long userInfoId = entity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = ObjectUtils.isEmpty(userInfoId) ? accountInfoEntityList.get(0) : accountInfoDao.selectById(userInfoId);
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
            DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
            List<Long> taskDetailIds = taskDetailEntities.stream()
                    .map(TaskDetailEntity::getId) // 提取每个对象的 ID
                    .collect(Collectors.toList());
            Map<String, String> idNameMap = taskDetailEntities.stream()
                    .collect(Collectors.toMap(TaskDetailEntity::getIDCard, TaskDetailEntity::getUserName));
            doSnatchInfo.setTaskId(id);
            doSnatchInfo.setIp(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getIp());
            doSnatchInfo.setPort(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getPort());
            doSnatchInfo.setCreator(entity.getCreator());
            //doSnatchInfo.setUserId(ObjectUtils.isEmpty(accountInfoEntity)?null:Long.valueOf(accountInfoEntity.getChannelUserId()));
            doSnatchInfo.setAccount(accountInfoEntity.getAccount());
            doSnatchInfo.setAuthorization(accountInfoEntity.getHeaders());
            doSnatchInfo.setSession(entity.getSession());
            doSnatchInfo.setUseDate(entity.getUseDate());
            doSnatchInfo.setTaskDetailIds(taskDetailIds);
            doSnatchInfo.setIdNameMap(idNameMap);
            result.add(doSnatchInfo);

            /*List<List<TaskDetailEntity>> partition = Lists.partition(taskDetailEntities, 5);
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
            }*/
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
        AccountInfoEntity queryAccount = new AccountInfoEntity();
        queryAccount.setChannel(ChannelEnum.CSTM.getCode());
        queryAccount.setYn(false);
        queryAccount.setCreator("system");
        List<AccountInfoEntity> accountInfoEntityList = accountInfoDao.selectByEntity(queryAccount);
        accountInfoEntityList = accountInfoEntityList.stream().filter(o -> !ObjectUtils.isEmpty(o.getHeaders())).collect(Collectors.toList());
        Collections.shuffle(accountInfoEntityList);
        List<ProxyInfo> xieQuProxy = ProxyUtil. getXieQuProxy(allUnDoneTasks.size());
        for (int i = 0; i < allUnDoneTasks.size(); i++) {
            TaskEntity entity = allUnDoneTasks.get(i);
            ProxyInfo proxyInfo = ObjectUtils.isEmpty(xieQuProxy) ? null : xieQuProxy.get(i);
            Long userInfoId = entity.getUserInfoId();
            AccountInfoEntity accountInfoEntity = ObjectUtils.isEmpty(userInfoId) ? accountInfoEntityList.get(0) : accountInfoDao.selectById(userInfoId);
            TaskDetailEntity query = new TaskDetailEntity();
            query.setTaskId(entity.getId());
            query.setDone(false);
            query.setYn(false);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(query);
            if (ObjectUtils.isEmpty(taskDetailEntities)) {
                entity.setDone(true);
                taskDao.updateTask(entity);
            }
            Collections.shuffle(taskDetailEntities);
            for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
                DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
                doSnatchInfo.setIp(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getIp());
                doSnatchInfo.setPort(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getPort());
                doSnatchInfo.setCreator(entity.getCreator());
                doSnatchInfo.setTaskId(entity.getId());
                doSnatchInfo.setUserId(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getChannelUserId() == null ? null : Long.valueOf(accountInfoEntity.getChannelUserId()));
                doSnatchInfo.setAccount(accountInfoEntity.getAccount());
                doSnatchInfo.setAuthorization(ObjectUtils.isEmpty(accountInfoEntity) ? null : accountInfoEntity.getHeaders());
                doSnatchInfo.setUseDate(entity.getUseDate());
                doSnatchInfo.setSession(entity.getSession());
                doSnatchInfo.setTaskDetailIds(Arrays.asList(taskDetailEntity.getId()));
                doSnatchInfo.setIdNameMap(new HashMap<String, String>() {{
                    put(taskDetailEntity.getIDCard(), taskDetailEntity.getUserName());
                }});
                result.add(doSnatchInfo);
                break;
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
            //redisService.setData(RedisKeyEnum.TASKDETAIL.getCode() + res.getId(), JSON.toJSONString(res));
        }
        return integer > 0;
    }

    @Override
    public void snatchingTicket(DoSnatchInfo doSnatchInfo) {
        Map<String, String> nameIDMap = doSnatchInfo.getIdNameMap();
        //RestTemplate currentRestTemp=ObjectUtils.isEmpty(doSnatchInfo.getIp())?TemplateUtil.initSSLTemplate():TemplateUtil.xieQuTemp(doSnatchInfo.getIp(),doSnatchInfo.getPort());
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
            JSONObject getCheckImageJson = getCheckImag(doSnatchInfo);
            if (!ObjectUtils.isEmpty(getCheckImageJson) && getCheckImageJson.getIntValue("code") == 200) {
                long l = System.currentTimeMillis();
                JSONObject data = getCheckImageJson.getJSONObject("data");
                String jigsawImageBase64 = data == null ? null : data.getString("jigsawImageBase64");
                String originalImageBase64 = data == null ? null : data.getString("originalImageBase64");
                String secretKey = data == null ? null : data.getString("secretKey");
                String token = data == null ? null : data.getString("token");
                /*String imageUuid = UUID.randomUUID().toString();
                String sliderImageName = "." + File.separator + imageUuid + "_" + "slider.png";
                String backImageName = "." + File.separator + imageUuid + "_" + "back.png";
                ImageUtils.imagCreate(jigsawImageBase64, sliderImageName, 155, 47);
                ImageUtils.imagCreate(originalImageBase64, backImageName, 155, 310);*/
                //图片验证码处理
                Double x = getPoint(originalImageBase64,jigsawImageBase64);
                JSONObject param = new JSONObject();
                param.put("x", x);
                param.put("y", 5);
                String point = EncDecUtil.doAES(JSON.toJSONString(param), secretKey);
                log.info("验证码处理完毕，处理时长:{}", System.currentTimeMillis() - l);
                Integer childrenTicketNum = priceNameCountMap.get("childrenTicket");
                HttpEntity shoppingCartUrlEntity = new HttpEntity<>(buildParam(token, childrenTicketNum == null ? 0 : childrenTicketNum, point, doSnatchInfo.getSession(), doSnatchInfo.getUseDate(), priceId, childrenPriceId, discountPriceId, olderPriceId, phone, nameIDMap), headers);
                JSONObject bodyJson = TemplateUtil.getResponse(TemplateUtil.kuaiDaiLiTemp(), shoppingCartUrl, HttpMethod.POST, shoppingCartUrlEntity);
                log.info("账号：{}下游客：{},提交订单结果：{}", doSnatchInfo.getAccount(), doSnatchInfo.getIdNameMap().values(), bodyJson);
                if (!ObjectUtils.isEmpty(bodyJson) && (bodyJson.getIntValue("code") == 550 || bodyJson.getIntValue("code") == 503)) {
                    if (!doneList.containsAll(doSnatchInfo.getIdNameMap().keySet())) {
                            SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"), "主场馆", doSnatchInfo.getAccount(), "任务失败：" + bodyJson.getString("msg"));
                    }
                    /*try {
                       Files.delete(Paths.get(sliderImageName));
                       Files.delete(Paths.get(backImageName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
                    return;
                }
                if (!ObjectUtils.isEmpty(bodyJson) && bodyJson.getIntValue("code") == 200) {
                    doneList.addAll(doSnatchInfo.getIdNameMap().keySet());
                    SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"), "主场馆", doSnatchInfo.getAccount(), String.join(",", doSnatchInfo.getIdNameMap().values()));
                    msgCache.remove(doSnatchInfo.getTaskId());
                    //查询个人订单
                    headers.set("Referer", "https://pcticket.cstm.org.cn/personal/car");
                    HttpEntity searchEntity = new HttpEntity(headers);
                    JSONObject searchBodyJson = getOrderDetail(searchEntity);
                    if (searchBodyJson == null || searchBodyJson.getIntValue("code") != 200) {
                        SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"), "主场馆", doSnatchInfo.getAccount(), "任务成功，更新任务数据失败请求检查" + String.join(",", doSnatchInfo.getIdNameMap().values()));
                        log.info("查询个人订单失败：{}", searchBodyJson);
                        /*try {
                            Files.delete(Paths.get(sliderImageName));
                            Files.delete(Paths.get(backImageName));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
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
                                    taskDetailEntity.setOrderCreatorAuth(doSnatchInfo.getAuthorization());
                                    taskDetailEntity.setOrderCreatorAccount(doSnatchInfo.getAccount());
                                    //taskDetailEntity.setExt(null);
                                    taskDetailEntities.add(taskDetailEntity);
                                }
                            });
                        }
                        taskDetailDao.updateTaskDetailBath(taskDetailEntities);
                    }
                    WebSocketServer.sendInfo(socketMsg("抢票成功", String.valueOf(nameIDMap.values()), 5000), doSnatchInfo.getCreator());
                }
                /*try {
                    Files.delete(Paths.get(sliderImageName));
                    Files.delete(Paths.get(backImageName));
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
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
            if (ObjectUtils.isEmpty(placeOrderInfo.getOrderId())||StrUtil.equals(placeOrderInfo.getOrderId(),"0")) {
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
                System.out.println(JSON.toJSONString(param));
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

    @Override
    public void initAccountPool(Integer num) {
        for (int i = 0; i < num; i++) {
            String phoneNo = VirtualPhoneUtil.getPhoneNo();
            AccountInfoEntity account = new AccountInfoEntity();
            account.setUserName("三方号" + phoneNo);
            account.setAccount(phoneNo);
            account.setChannel(ChannelEnum.CSTM.getCode());
            account.setCreator("system");
            account.setYn(false);
            account.setStatus(false);
            account.setCreateDate(new Date());
            accountInfoDao.insertOrUpdate(account);
        }
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

    public static Double getPoint(String backImageBase64, String sliderImageBase64) {
        // 解码Base64字符串为字节数组
        byte[] backImageBytes = Base64.getDecoder().decode(backImageBase64);
        byte[] sliderImageBytes = Base64.getDecoder().decode(sliderImageBase64);
        // 将字节数组转换为Mat对象
        Mat backImageMat = imdecode(new Mat(new BytePointer(backImageBytes)), opencv_imgcodecs.IMREAD_GRAYSCALE);
        Mat sliderImageMat = imdecode(new Mat(new BytePointer(sliderImageBytes)), opencv_imgcodecs.IMREAD_GRAYSCALE);

        // 对图像进行二值化处理
        opencv_imgproc.threshold(backImageMat, backImageMat, 215, 255, opencv_imgproc.THRESH_BINARY);
        opencv_imgproc.threshold(sliderImageMat, sliderImageMat, 215, 255, opencv_imgproc.THRESH_BINARY);

        // 模板匹配
        Mat result = new Mat();
        opencv_imgproc.matchTemplate(sliderImageMat, backImageMat, result, opencv_imgproc.TM_CCORR_NORMED);
        opencv_core.normalize(result, result, 1, 0, opencv_core.NORM_MINMAX, -1, new Mat());

        DoublePointer doublePointer = new DoublePointer(new double[2]);
        Point maxLoc = new Point();
        opencv_core.minMaxLoc(result, null, doublePointer, null, maxLoc, null);

        // 在滑块图像上绘制矩形
        opencv_imgproc.rectangle(sliderImageMat, maxLoc, new Point(maxLoc.x() + backImageMat.cols(), maxLoc.y() + backImageMat.rows()), new Scalar(0, 255, 0, 1));

        int real = maxLoc.x() * 330 / 310;
        return real * 310 / 330.0;
    }

    // 辅助方法：将字节数组解码为Mat对象
    private static Mat imdecode(Mat buf, int flags) {
        return opencv_imgcodecs.imdecode(buf, flags);
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
        headers.set("Referer", "https://pcticket.cstm.org.cn/personal/check_info?name=%E4%B8%BB%E5%B1%95%E5%8E%85");
        headers.set("Sec-Ch-Ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"");
        headers.set("Sec-Ch-Ua-Mobile", "?0");
        headers.set("Sec-Ch-Ua-Platform", "\"macOS\"");
        headers.set("Sec-Fetch-Dest", "empty");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        return headers;
    }

    private JSONObject getCheckImag(DoSnatchInfo doSnatchInfo) {
        JSONObject response;
        List<ProxyInfo> xieQuProxy = ProxyUtil.getXieQuProxy(1);
        //RestTemplate restTemplate = ObjectUtils.isEmpty(doSnatchInfo.getIp()) ? TemplateUtil.initSSLTemplate() : TemplateUtil.xieQuTemp(doSnatchInfo.getIp(), doSnatchInfo.getPort());
        try {
            //Thread.sleep(1000);
            HttpEntity entity = new HttpEntity(getHeader(doSnatchInfo.getAuthorization()));
            response = TemplateUtil.getResponse(TemplateUtil.kuaiDaiLiTemp(), getCheckImagUrl, HttpMethod.GET, entity);
            if (!ObjectUtils.isEmpty(response) && response.getIntValue("code") == 200) {
                log.info("账号:{}获取到提单验证码成功", doSnatchInfo.getAccount());
                return response;
            }
            log.info("账号:{}获取提单验证码失败{}，重试中", doSnatchInfo.getAccount(), response);
        } catch (Exception e) {
            e.printStackTrace();
            if (!ObjectUtils.isEmpty(xieQuProxy)) {
                restTemplate = TemplateUtil.xieQuTemp(doSnatchInfo.getIp(), doSnatchInfo.getPort());
            }
            log.info("账号:{}获取提单验证码异常，涉及游客", String.join(",", doSnatchInfo.getIdNameMap().values()));
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
     *
     * @param date
     * @param taskDetailEntities
     * @return
     */
    private List<String> checkUserRepeat(Date date, List<TaskDetailEntity> taskDetailEntities, Integer channel) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setYn(false);
        taskEntity.setUseDate(date);
        taskEntity.setChannel(ChannelEnum.CSTM.getCode());
        List<TaskEntity> taskEntityList = taskDao.fuzzyQuery(taskEntity);
        taskEntityList = taskEntityList.stream().filter(o -> o.getChannel() == channel).collect(Collectors.toList());
        List<String> ids = new ArrayList<>();
        taskEntityList.forEach(entity -> {
            List<TaskDetailEntity> taskDetailEntityList = taskDetailDao.selectByTaskId(entity.getId());
            List<String> collect = taskDetailEntityList.stream().map(TaskDetailEntity::getIDCard).collect(Collectors.toList());
            ids.addAll(collect);
        });
        List<String> res = new ArrayList<>();
        for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
            if (ids.contains(taskDetailEntity.getIDCard())) {
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
                //SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), null, null, phoneNum, "获取渠道短信验证码异常!");
                return;
            }
            sourceParam.setVerificationCode(msgCode);
            ServiceResponse login = loginService.login(sourceParam);
            if (login.getStatus() != 0) {
                SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), null, null, phoneNum, "登录异常:"+login.getMsg());
                return;
            }
            log.info("账号{}登录成功成功", sourceParam.getPhone());
        } catch (Exception e) {
            log.info("获取手机号异常:{}", e);
        }

    }


    public static void main(String[] args) {
        String jigsawImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAC8AAACbCAYAAADyfMLPAAADmklEQVR42u2Y30tUQRTH/Wt6iqCoiOpJiIgeXMSCJaKywMgilyhCwYUMMSySCHswbPFhpVIyskKK1X6Q1IMU/UET38FzO0137s5ddpa7+P3AYa6uW59zZu7MubenZydiFF0nvv6zYaOrkhDxT7++JCOi8AnoirtR6ASyxN0Euk5eL59Cy3/c+mBDrrtCXhIQeZ1EoeV1AmnyhV7zOgGffOH3el39tR/vbEgCXXNQudXvmjYBolLxwsibHKTJ56Wt4o9mx0391RPzpvHCvP5cN0tr82Z1c8W8/b5qR1njeq2n3bh665T4+vvbP9F2+ZmZqpWH+OL7ORuQltDiSMiVdxPQSUBYX0erPORr2/K4FnnIyEw0q7xvBqIcZm7lMbqVh6CegZDK+xKIJv+8sWBqS48T+ZXNlzah5fW6vR98lU/redJ6/yjys08nEmmMSATiyxsLNiGI6/Xvk/fNQLQeCP/YXG3KPFt8YMUR2G1QcYjjGpWXmWhVPkorIcsG8gjZbSAcss+H3sDR5O9Nj/4nn/afuN1lnvUfTT7PKSif4+b17flZCTWTj/5WQpaPyMsMNLsfsnohEdaHWZQkRF7OAC3dLIE0If2o6bYWUfoh2T51f58WaQm5NLY2kkACGKOeDfrQckV9P2c1dwidBKRlzJTPOzX4W7dh08smNLKWl54Jr5tslaE3CD6XllmaNVfanRH3Z3dWshLIrLzIo7O8P383MwH5TM4B9/ByZWU71SG/T/uer/qZ8jeuXk4SQPiYrU97dwtfElpaQr7fLIEg+SuXBs3E2C3bJkAeiUgyGNH7oOcJPWDSKi4RknjwMzI+HL5YTqqPQCJyjYQgLzMScjPnkW/1pE++NFgumduVa2ZyqmLFEQ/v/JWXGzp0J0L3KTdzqHzLe/bQ2dO28tWbIzYgjxHJTFbH7XUeedlKOyJ/ZqBklw6qP3r9nA1cQ1qSynMW6OrrLTWKfN+xI+b8wEm7fDALI0MXEmmMSKQyNhy85vEgA3k3kETb5Y8e3GsDSZRLx20C2IEgD3FcYww9xPAgIxFdvvfwfnNgzy6bQP+JXpvAqf4+OwOQllGfxF7x7edeVN8NJBCl8pA/tG93Io9ZwL0gS0iWEXahZvJp0vIWoiNPUnnfN8rvIdmxd5UxXti2q3slhBBCCCGEEEIIIYQQQgghhBBCCCGEEEIIIYQQQgghhBBCCCGEEEIIIYQQQgghhBBCCCEd4w/eO7kNXGxRggAAAABJRU5ErkJggg==";
        String originalImageBase64 = "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";
        System.out.println(ImageUtils.mergeBase64Images(jigsawImageBase64, originalImageBase64));
        String imageUuid = UUID.randomUUID().toString();
        String sliderImageName = "." + File.separator + imageUuid + "_" + "slider.png";
        String backImageName = "." + File.separator + imageUuid + "_" + "back.png";
        ImageUtils.imagCreate(jigsawImageBase64, sliderImageName, 155, 47);
        ImageUtils.imagCreate(originalImageBase64, backImageName, 155, 310);
        //图片验证码处理
        Double x = getPoint(originalImageBase64,jigsawImageBase64);
        System.out.println(x);
    }
}
