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
        String jigsawImageBase64 = "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";
        String originalImageBase64 = "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";
        //System.out.println(ImageUtils.mergeBase64Images(jigsawImageBase64, originalImageBase64));
        String imageUuid = UUID.randomUUID().toString();
        String sliderImageName = "." + File.separator + imageUuid + "_" + "slider.png";
        String backImageName = "." + File.separator + imageUuid + "_" + "back.png";
        //ImageUtils.imagCreate(jigsawImageBase64, sliderImageName, 155, 47);
        //ImageUtils.imagCreate(originalImageBase64, backImageName, 155, 310);
        //图片验证码处理
        Double x = getPoint(originalImageBase64,jigsawImageBase64);
        System.out.println(x);
    }
}
