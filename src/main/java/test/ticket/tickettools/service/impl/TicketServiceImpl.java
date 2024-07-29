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
import org.bytedeco.opencv.opencv_core.Size;
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
        //List<ProxyInfo> xieQuProxy = ProxyUtil.getXieQuProxy(taskEntities.size());
        for (int i = 0; i < taskEntities.size(); i++) {
            TaskEntity entity = taskEntities.get(i);
            //ProxyInfo proxyInfo = ObjectUtils.isEmpty(xieQuProxy) ? null : xieQuProxy.get(i);
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
            //doSnatchInfo.setIp(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getIp());
            //doSnatchInfo.setPort(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getPort());
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
        //List<ProxyInfo> xieQuProxy = ProxyUtil. getXieQuProxy(allUnDoneTasks.size());
        for (int i = 0; i < allUnDoneTasks.size(); i++) {
            TaskEntity entity = allUnDoneTasks.get(i);
            //ProxyInfo proxyInfo = ObjectUtils.isEmpty(xieQuProxy) ? null : xieQuProxy.get(i);
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
                //doSnatchInfo.setIp(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getIp());
                //doSnatchInfo.setPort(ObjectUtils.isEmpty(proxyInfo) ? null : proxyInfo.getPort());
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
                    if (!doneList.containsAll(doSnatchInfo.getIdNameMap().keySet())&&bodyJson.getString("msg").contains("已有订单")) {
                            SendMessageUtil.send(ChannelEnum.CSTM.getDesc(), DateUtil.format(doSnatchInfo.getUseDate(), "yyyy/MM/dd"), "主场馆", doSnatchInfo.getAccount(), doSnatchInfo.getIdNameMap().values() + bodyJson.getString("msg"));
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
        // 计算滑块需要滑动的实际距离，并保持浮点精度
        Double real = (maxLoc.x()-1) * 330.0 / 310;
        return real * 310.0 / 330.0;
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
        //List<ProxyInfo> xieQuProxy = ProxyUtil.getXieQuProxy(1);
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
            /*if (!ObjectUtils.isEmpty(xieQuProxy)) {
                restTemplate = TemplateUtil.xieQuTemp(doSnatchInfo.getIp(), doSnatchInfo.getPort());
            }*/
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
