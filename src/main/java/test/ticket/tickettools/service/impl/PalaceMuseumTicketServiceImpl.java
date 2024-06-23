package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.dao.TaskDetailDao;
import test.ticket.tickettools.dao.AccountInfoDao;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.bo.ProxyInfo;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.service.DoSnatchTicketService;
import test.ticket.tickettools.service.WebSocketServer;
import test.ticket.tickettools.utils.*;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class PalaceMuseumTicketServiceImpl implements DoSnatchTicketService {

    //获取余票信息
    private static final String getReserveListUrl = "https://lotswap.dpm.org.cn/lotsapi/order/api/batchTimeReserveList";
    //校验成员信息
    private static final String checkUserUrl = "https://lotswap.dpm.org.cn/dubboApi/trade-core/tradeCreateService/ticketVerificationCheck";
    //添加常用人
    private static final String addUserUrl = "https://lotswap.dpm.org.cn/lotsapi/up/api/user/contacts";
    //获取常用人列表
    private static final String getUsersUrl = "https://lotswap.dpm.org.cn/lotsapi/up/api/user/contacts/list?cipherText=0&merchantId=2655&merchantInfoId=2655";
    //删除常用用户
    private static final String delUserUrl = "https://lotswap.dpm.org.cn/lotsapi/up/api/user/contacts/";

    private static Map<Long, Object> runTaskCache = new ConcurrentHashMap<>();
    private static Map<Long,Object> initTaskCache=new ConcurrentHashMap<>();

    @Resource
    TaskDao taskDao;
    @Resource
    TaskDetailDao taskDetailDao;
    @Resource
    AccountInfoDao accountInfoDao;


    @Override
    public void initData(TaskEntity unDoneTask) {
        Long taskId = unDoneTask.getId();
        if(initTaskCache.containsKey(taskId)){
            return;
        }else{
            initTaskCache.put(taskId,true);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            if (!ObjectUtils.isEmpty(unDoneTask.getIp()) && !ObjectUtils.isEmpty(unDoneTask.getPort())) {
                initTaskCache.remove(taskId);
                return;
            }
            ProxyInfo proxy = ProxyUtil.getProxyList(1).get(0);
            unDoneTask.setIp(proxy.getIp());
            unDoneTask.setPort(proxy.getPort());
            taskDao.updateTask(unDoneTask);
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(unDoneTask.getUserInfoId());
            String headerStr = accountInfoEntity.getHeaders();
            JSONObject headerJson = JSON.parseObject(headerStr);
            for (Map.Entry<String, Object> headerEntry : headerJson.entrySet()) {
                headers.set(headerEntry.getKey(), headerEntry.getValue().toString());
            }
            headers.set("Accept-Encoding", "gzip,compress,deflate");
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            headers.setContentType(MediaType.APPLICATION_JSON);
            Integer integer = taskDao.updateTask(unDoneTask);
            if (integer > 0) {
                RestTemplate restTemplate = TemplateUtil.initSSLTemplateWithProxyAuth(proxy.getIp(), proxy.getPort());
                HttpEntity getUsersEntity = new HttpEntity<>(headers);
                JSONObject getUsersRes = TemplateUtil.getResponse(restTemplate, getUsersUrl, HttpMethod.GET, getUsersEntity);
                if (ObjectUtils.isEmpty(getUsersRes) && StrUtil.equals("success", getUsersRes.getString("message"))) {
                    log.info("获取常用成员失败:{}", getUsersRes);
                }
                JSONArray userList = getUsersRes.getJSONArray("data");
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                if (!ObjectUtils.isEmpty(userList)) {
                    String delUserBody = "merchantId=2655&merchantInfoId=2655";
                    headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
                    headers.set("Content-Length", String.valueOf(customURLEncode(delUserBody, "utf-8").getBytes(StandardCharsets.UTF_8).length));
                    HttpEntity delUserEntity = new HttpEntity<>(delUserBody, headers);
                    for (int i = 0; i < userList.size(); i++) {
                        JSONObject user = userList.getJSONObject(i);
                        String id = user.getString("id");
                        Thread.sleep(RandomUtil.randomInt(3000, 5000));
                        JSONObject response = TemplateUtil.getResponse(restTemplate, delUserUrl + id, HttpMethod.DELETE, delUserEntity);
                        log.info("删除成员结果:{}", response);
                        if (ObjectUtils.isEmpty(response) && StrUtil.equals("success", response.getString("message"))) {
                            log.info("删除成员失败:{}", response);
                        }
                    }
                }

                List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByTaskId(unDoneTask.getId());
                for (TaskDetailEntity taskDetailEntity : taskDetailEntities) {
                    headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
                    String bodyFormat = MessageFormat.format("id=&name={0}&idCard={1}&type=0&cipherText=0", URLEncoder.encode(taskDetailEntity.getUserName(), "utf-8"), taskDetailEntity.getIDCard());
                    headers.set("Content-Length", String.valueOf(customURLEncode(bodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
                    HttpEntity addUserEntity = new HttpEntity<>(bodyFormat, headers);
                    Thread.sleep(RandomUtil.randomInt(4000, 5000));
                    JSONObject response = TemplateUtil.getResponse(restTemplate, addUserUrl, HttpMethod.POST, addUserEntity);
                    log.info("添加成员结果:{}", response);
                    if (ObjectUtils.isEmpty(response) && StrUtil.equals("success", response.getString("message"))) {
                        log.info("添加成员失败:{}", response);
                    }
                }
            }
            initTaskCache.remove(taskId);
        } catch (Exception e) {
            initTaskCache.remove(taskId);
            log.info("初始化故宫数据失败:{}", e);
        }
    }

    @Override
    public List<TaskEntity> getAllUndoneTask() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.LOTS.getCode());
        List<TaskEntity> unDoneTasks = taskDao.getUnDoneTasks(taskEntity);
        return unDoneTasks;
    }

    @Override
    public List<DoSnatchInfo> getDoSnatchInfos() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.LOTS.getCode());
        List<TaskEntity> unDoneTasks = taskDao.getUnDoneTasks(taskEntity);
        List<DoSnatchInfo> doSnatchInfoList = new ArrayList<>();
        for (TaskEntity unDoneTask : unDoneTasks) {
            TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
            taskDetailEntity.setTaskId(unDoneTask.getId());
            taskDetailEntity.setDone(false);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(taskDetailEntity);
            if (ObjectUtils.isEmpty(taskDetailEntities)) {
                unDoneTask.setDone(true);
                taskDao.updateTask(unDoneTask);
                continue;
            }
            DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
            doSnatchInfo.setTaskId(unDoneTask.getId());
            doSnatchInfo.setUserInfoId(unDoneTask.getUserInfoId());
            AccountInfoEntity accountInfoEntity = accountInfoDao.selectById(unDoneTask.getUserInfoId());
            doSnatchInfo.setAccount(unDoneTask.getAccount());
            doSnatchInfo.setHeaders(accountInfoEntity.getHeaders());
            doSnatchInfo.setChannelUserId(accountInfoEntity.getChannelUserId());
            doSnatchInfo.setUseDate(unDoneTask.getUseDate());
            doSnatchInfo.setSession(unDoneTask.getSession());
            doSnatchInfo.setIp(unDoneTask.getIp());
            doSnatchInfo.setPort(unDoneTask.getPort());
            List<Long> taskDetailIds = new ArrayList<>();
            Map<String, String> idNameMap = new HashMap<>();
            for (TaskDetailEntity detailEntity : taskDetailEntities) {
                taskDetailIds.add(detailEntity.getId());
                idNameMap.put(detailEntity.getIDCard().trim(), detailEntity.getUserName().trim());
            }
            doSnatchInfo.setTaskDetailIds(taskDetailIds);
            doSnatchInfo.setIdNameMap(idNameMap);
            doSnatchInfoList.add(doSnatchInfo);
        }
        return doSnatchInfoList;
    }

    @Override
    public void doSnatchingTicket(DoSnatchInfo doSnatchInfo) {
        Long taskId = doSnatchInfo.getTaskId();
        if (runTaskCache.containsKey(taskId)) {
            return;
        } else {
            runTaskCache.put(taskId, true);
        }
        //查询余票
        String queryImperialPalaceTicketsUrl = "https://lotswap.dpm.org.cn/lotsapi/merchant/api/fsyy/calendar?parkId=11324&year=%s&month=%s&merchantId=2655&merchantInfoId=2655";
        //获取门票种类
        String getTicketGridUrl = "https://lotswap.dpm.org.cn/lotsapi/merchant/api/merchantParkTicketGridNew?date=%s&merchantParkInfoId=11324&currPage=1&pageSize=200&merchantInfoId=2655&playDate=%s&businessType=park";
        //提交订单
        String createUrl = "https://lotswap.dpm.org.cn/dubboApi/trade-core/tradeCreateService/create?sign=%s&timestamp=%s";
        try {
            JSONObject currentParkFsyyDetail = new JSONObject();
            RestTemplate restTemplate = ObjectUtils.isEmpty(doSnatchInfo.getIp()) ? TemplateUtil.initSSLTemplate() : TemplateUtil.initSSLTemplateWithProxyAuth(doSnatchInfo.getIp(), doSnatchInfo.getPort());
            //RestTemplate restTemplate=TemplateUtil.initSSLTemplate();
            HttpHeaders headers = new HttpHeaders();
            String headerStr = doSnatchInfo.getHeaders();
            JSONObject headerJson = JSON.parseObject(headerStr);
            LocalDate now = LocalDate.now();
            for (Map.Entry<String, Object> headerEntry : headerJson.entrySet()) {
                headers.set(headerEntry.getKey(), headerEntry.getValue().toString());
            }
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept-Encoding", "gzip,compress,deflate");
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            String mpOpenId = headerJson.getString("mpOpenId");
            HttpEntity entity = new HttpEntity<>(headers);
            //查询当月余票
            Date useDate = doSnatchInfo.getUseDate();
            String formatUseDate = DateUtils.dateToStr(useDate, "yyyy-MM-dd");
            LocalDate localDate = useDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int monthValue = localDate.getMonthValue();
            String month = monthValue > 10 ? String.valueOf(monthValue) : "0" + monthValue;
            String formatQueryImperialPalaceTicketsUrl = String.format(queryImperialPalaceTicketsUrl, now.getYear(), month);
            Thread.sleep(RandomUtil.randomInt(4000, 7000));
            JSONObject responseJson = TemplateUtil.getResponse(restTemplate, formatQueryImperialPalaceTicketsUrl, HttpMethod.GET, entity);
            if (ObjectUtils.isEmpty(responseJson) || responseJson.getIntValue("status") != 200) {
                log.info("responseJson:{}", responseJson);
                runTaskCache.remove(taskId);
                return;
            }
            String decCalendarTicketsStr = EncDecUtil.decData(responseJson.getString("privateKey"), responseJson.getString("data"));
            JSONArray data = JSON.parseArray(decCalendarTicketsStr);
            if (ObjectUtils.isEmpty(data)) {
                log.info("获取到的场次失败");
                runTaskCache.remove(taskId);
                return;
            }
            boolean haveTicket = false;
            String session = doSnatchInfo.getSession();
            outLoop:
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                if (StrUtil.equals("T", item.getString("saleStatus")) && item.getIntValue("stockNum") == 1) {
                    if (StrUtil.equals(DateUtils.dateToStr(useDate, "yyyy-MM-dd"), item.getString("occDate"))) {
                        JSONArray parkFsyyDetailDTOS = item.getJSONArray("parkFsyyDetailDTOS");
                        if (!ObjectUtils.isEmpty(parkFsyyDetailDTOS)) {
                            for (int j = 0; j < parkFsyyDetailDTOS.size(); j++) {
                                JSONObject parkFsyyDetailJson = parkFsyyDetailDTOS.getJSONObject(j);
                                if (parkFsyyDetailJson.getIntValue("stockNum") == 1 && parkFsyyDetailJson.getIntValue("totalNum") == 1) {
                                    //判断是否对上下午有要求
                                    if (ObjectUtils.isEmpty(session)) {
                                        haveTicket = true;
                                        currentParkFsyyDetail = parkFsyyDetailJson;
                                        break outLoop;
                                    } else {
                                        if(session.split(",").length>1){
                                            haveTicket = true;
                                            currentParkFsyyDetail = parkFsyyDetailJson;
                                            break outLoop;
                                        }
                                        if (doSnatchInfo.getSession().contains(parkFsyyDetailJson.getString("fsTimeName"))) {
                                                haveTicket = true;
                                                currentParkFsyyDetail = parkFsyyDetailJson;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //如果没有余票继续查询
            if (!haveTicket) {
                log.info("没有余票");
                runTaskCache.remove(taskId);
                return;
            }
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            HttpEntity getTicketEntity = new HttpEntity<>(headers);
            String formatGetTicketGridUrl = String.format(getTicketGridUrl, formatUseDate, formatUseDate);
            Thread.sleep(RandomUtil.randomInt(3000, 3500));
            JSONObject ticketGridJson = TemplateUtil.getResponse(restTemplate, formatGetTicketGridUrl, HttpMethod.GET, getTicketEntity);
            if (ObjectUtils.isEmpty(ticketGridJson)) {
                runTaskCache.remove(taskId);
                return;
            }
            JSONArray ticketGridDataArr = ticketGridJson.getJSONArray("data");
            JSONObject ticketGridItem = ObjectUtils.isEmpty(ticketGridDataArr) ? null : ticketGridDataArr.getJSONObject(0);
            JSONArray ticketList = ticketGridItem == null ? null : ticketGridItem.getJSONArray("ticketList");
            JSONArray ticketReserveList = new JSONArray();
            if (ObjectUtils.isEmpty(ticketList)) {
                runTaskCache.remove(taskId);
                return;
            }
            Map<String, JSONObject> typeTicketMap = new HashMap();
            Map<String, JSONObject> modelCodeTicketInfoMap = new HashMap();
            List<String> modelCodes = new ArrayList();
            for (int i = 0; i < ticketList.size(); i++) {
                JSONObject ticketInfo = ticketList.getJSONObject(i);
                String nickName = ticketInfo.getString("nickName");
                String modelCode = ticketInfo.getString("modelCode");
                modelCodes.add(modelCode);
                JSONObject tickCodeInfo = new JSONObject();
                tickCodeInfo.put("modelCode", modelCode);
                tickCodeInfo.put("externalCode", ticketInfo.getString("externalCode"));
                tickCodeInfo.put("startTime", formatUseDate);
                tickCodeInfo.put("endTime", formatUseDate);
                if (StrUtil.equals("标准票", nickName)) {
                    typeTicketMap.put("normal", tickCodeInfo);
                }
                if (StrUtil.equals("老年人票", nickName)) {
                    typeTicketMap.put("old", tickCodeInfo);
                }
                if (StrUtil.equals("未成年人免费票", nickName)) {
                    typeTicketMap.put("free", tickCodeInfo);
                }
                if (StrUtil.equals("学生票", nickName)) {
                    typeTicketMap.put("student", tickCodeInfo);
                }
                modelCodeTicketInfoMap.put(modelCode, ticketInfo);
                ticketReserveList.add(tickCodeInfo);
            }
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            String addTicketUrl = String.format("https://lotswap.dpm.org.cn/lotsapi/merchant/api/merchantParkInfo/add_ticket/query?modelCodes=%s&occDate=%s&merchantId=2655&merchantInfoId=2655", String.join(",", modelCodes), formatUseDate);
            Thread.sleep(RandomUtil.randomInt(1000, 3500));
            TemplateUtil.getResponse(restTemplate, addTicketUrl, HttpMethod.GET, new HttpEntity<>(headers));
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            String bodyFormat = MessageFormat.format("queryParam={0}&merchantId=2655&merchantInfoId=2655", ticketReserveList);
            //需要设置content_type application/x-www-form-urlencoded
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            URLEncoder.encode(bodyFormat, "utf-8");
            headers.set("Content-Length", String.valueOf(customURLEncode(bodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
            HttpEntity getReserveListEntity = new HttpEntity<>(bodyFormat, headers);
            Thread.sleep(RandomUtil.randomInt(1000, 3500));
            JSONObject reserveListJson = TemplateUtil.getResponse(restTemplate, getReserveListUrl, HttpMethod.POST, getReserveListEntity);
            if (ObjectUtils.isEmpty(reserveListJson) || reserveListJson.getIntValue("status") != 200) {
                runTaskCache.remove(taskId);
                return;
            }
            String decReserveListStr = EncDecUtil.decData(responseJson.getString("privateKey"), responseJson.getString("data"));
            JSONArray reserveList = JSON.parseArray(decReserveListStr);
            if (ObjectUtils.isEmpty(reserveList)) {
                log.info("批量获取余票数据失败batchTimeReserveList", reserveListJson);
                runTaskCache.remove(taskId);
                return;
            }
            //校验用户信息
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            headers.setContentType(MediaType.APPLICATION_JSON);
            String formatDate = DateUtils.dateToStr(doSnatchInfo.getUseDate(), "yyyy-MM-dd");
            JSONObject checkUserBody = buildCheckUserParam(doSnatchInfo.getIdNameMap(), formatDate, typeTicketMap);
            log.info("校验身份信息入参：{}", JSON.toJSONString(checkUserBody));
            HttpEntity checkUserEntity = new HttpEntity<>(checkUserBody, headers);
            Thread.sleep(RandomUtil.randomInt(4000, 5000));
            JSONObject checkUserBodyJson = TemplateUtil.getResponse(restTemplate, checkUserUrl, HttpMethod.POST, checkUserEntity);
            JSONObject checkUserData = checkUserBodyJson.getJSONObject("data");
            log.info("身份验证信息:{}", checkUserData);
            if (!ObjectUtils.isEmpty(checkUserData.getJSONArray("rejectCertAuthList"))) {
                log.info("身份验证失败:{}", checkUserBodyJson);
                runTaskCache.remove(taskId);
                return;
            }
            String accessToken = headerJson.getString("access-token");
            headers.set("Accept-Encoding", "gzip,compress,deflate");
            modelCodeTicketInfoMap.put("parkFsyyDetailDTO", currentParkFsyyDetail);
            long timestamp = System.currentTimeMillis();
            String ts = String.valueOf(timestamp).substring(0, 11);
            headers.set("ts", String.valueOf(timestamp / 1000));
            String signStr = "VDsdxfwljhy#@!94857access-token=" + accessToken + ts + "AAXY";
            String sign = DigestUtils.md5Hex(signStr);
            JSONObject jsonObject = buildCreateParam(mpOpenId, checkUserBody, doSnatchInfo, modelCodeTicketInfoMap);
            headers.setContentLength(JSON.toJSONString(jsonObject).getBytes(StandardCharsets.UTF_8).length);
            HttpEntity addTicketQueryEntity = new HttpEntity<>(jsonObject, headers);
            String formatCreateUrl = String.format(createUrl, sign, timestamp);
            Thread.sleep(RandomUtil.randomInt(3000, 5000));
            log.info("提交订单入参：{}", JSON.toJSONString(jsonObject));
            JSONObject createRes = TemplateUtil.getResponse(restTemplate, formatCreateUrl, HttpMethod.POST, addTicketQueryEntity);
            log.info("请求结果{}", createRes);
            if (createRes.getIntValue("code") == 200) {
                TaskEntity taskEntity = new TaskEntity();
                taskEntity.setId(doSnatchInfo.getTaskId());
                taskEntity.setDone(true);
                taskEntity.setUpdateDate(new Date());
                taskDao.updateTask(taskEntity);
                TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
                taskDetailEntity.setTaskId(doSnatchInfo.getTaskId());
                taskDetailEntity.setUpdateDate(new Date());
                taskDetailEntity.setDone(true);
                taskDetailEntity.setOrderNumber(createRes.getJSONObject("data").getString("orderCode"));
                taskDetailDao.updateEntityByTaskId(taskDetailEntity);
                SendMessageUtil.send(ChannelEnum.LOTS.getDesc(), formatDate, currentParkFsyyDetail.getString("fsTimeName"), doSnatchInfo.getAccount(), String.join(",", doSnatchInfo.getIdNameMap().values()));
                WebSocketServer.sendInfo(socketMsg("抢票成功", "账号:" + doSnatchInfo.getAccount() + "购票成功，请支付", 5000), null);
            }
            runTaskCache.remove(taskId);
        } catch (Exception e) {
            runTaskCache.remove(doSnatchInfo.getTaskId());
            log.info("doPalaceMuseumTicket异常:{}", e);
        }
    }

    private JSONObject buildCheckUserParam(Map<String, String> iDNameMap, String useDate, Map<String, JSONObject> typeTicketMap) {
        JSONObject param = new JSONObject();
        JSONObject normal = new JSONObject();
        JSONObject old = new JSONObject();
        JSONObject free = new JSONObject();
        //JSONObject student=new JSONObject();
        for (Map.Entry<String, String> nameIDMapEntry : iDNameMap.entrySet()) {
            String idCard = nameIDMapEntry.getKey();
            String name = nameIDMapEntry.getValue();
            //如果是护照之类的直接添加到成人
            if (idCard.length() < 17) {
                normal = buildItem(normal, idCard, name, "normal", typeTicketMap, true);
                continue;
            }
            Integer age = GetAgeForIdCardUtil.getAge(idCard);
            if (age >= 0 && age < 18) {
                free = buildItem(free, idCard, name, "free", typeTicketMap, false);
                continue;
            }
            if (age >= 60) {
                old = buildItem(old, idCard, name, "old", typeTicketMap, false);
                continue;
            }
            //学生后续优化
            normal = buildItem(normal, idCard, name, "normal", typeTicketMap, false);
        }
        List ticketVerificationDTOS = new ArrayList();
        if (!ObjectUtils.isEmpty(normal)) {
            ticketVerificationDTOS.add(normal);
        }
        if (!ObjectUtils.isEmpty(old)) {
            ticketVerificationDTOS.add(old);
        }
        if (!ObjectUtils.isEmpty(free)) {
            ticketVerificationDTOS.add(free);
        }
        param.put("ticketVerificationDTOS", ticketVerificationDTOS);
        param.put("merchantId", "2655");
        param.put("merchantInfoId", "2655");
        param.put("startDate", useDate);
        param.put("orderType", "park");
        param.put("chooseRuleProcessors", "");
        return param;
    }

    private JSONObject buildItem(JSONObject item, String idCard, String name, String type, Map<String, JSONObject> typeTicketMap, boolean isPassport) {
        if (item.get("certAuthDTOS") == null) {
            item.put("certAuthDTOS", Arrays.asList(new HashMap() {{
                put("cardType", isPassport ? 2 : 0);
                put("certNo", idCard);
                put("name", name);
            }}));
        } else {
            List certAuthDTOS = item.getJSONArray("certAuthDTOS").toJavaList(Object.class);
            JSONObject certAuthDTO = new JSONObject();
            certAuthDTO.put("cardType", isPassport ? 2 : 0);
            certAuthDTO.put("certNo", idCard);
            certAuthDTO.put("name", name);
            certAuthDTOS.add(certAuthDTO);
            item.put("certAuthDTOS", certAuthDTOS);
            return item;
        }
        if (item.get("modelCodesDTOS") == null) {
            String modelCode = typeTicketMap.get(type).getString("modelCode");
            item.put("modelCodesDTOS", Arrays.asList(new HashMap() {{
                put("modelCode", modelCode);
                put("parentModelCode", modelCode);
            }}));
        }
        return item;
    }

    private JSONObject buildCreateParam(String openId, JSONObject checkParam, DoSnatchInfo doSnatchInfo, Map<String, JSONObject> modelCodeTicketInfoMap) {
        JSONObject param = new JSONObject();
        Map<String, String> buyerMap = getBuyerMap(doSnatchInfo.getIdNameMap());
        param.put("buyer", new HashMap<String, Object>() {{
            put("id", doSnatchInfo.getChannelUserId());
            put("openId", openId);
            put("mobile", doSnatchInfo.getAccount());
            put("credentialNo", buyerMap.get("idCard"));
            put("credentialType", "0");
            put("nickName", buyerMap.get("name"));
        }});
        String dateStr = DateUtils.dateToStr(doSnatchInfo.getUseDate(), "yyyy-MM-dd");
        param.put("couponCode", "");
        param.put("startDate", dateStr);
        param.put("endDate", dateStr);
        param.put("addTickets", Collections.emptyList());
        List saveOrderList = new ArrayList();
        JSONArray ticketVerificationDTOS = checkParam.getJSONArray("ticketVerificationDTOS");
        for (int i = 0; i < ticketVerificationDTOS.size(); i++) {
            JSONObject ticketVerificationDTO = ticketVerificationDTOS.getJSONObject(i);
            JSONObject modelCodesDTO = ticketVerificationDTO.getJSONArray("modelCodesDTOS").getJSONObject(0);
            JSONObject ticketInfoJson = modelCodeTicketInfoMap.get(modelCodesDTO.getString("modelCode"));
            JSONObject parkFsyyDetailDTO = modelCodeTicketInfoMap.get("parkFsyyDetailDTO");
            JSONObject orderInfo = new JSONObject();
            orderInfo.put("ticketName", ticketInfoJson.getString("modelName"));
            orderInfo.put("price", ticketInfoJson.getDouble("price").intValue());
            orderInfo.put("amount", ticketVerificationDTO.getJSONArray("certAuthDTOS").size());
            orderInfo.put("modelCode", modelCodesDTO.getString("modelCode"));
            orderInfo.put("itemId", ticketInfoJson.getString("itemId"));
            orderInfo.put("wayType", "6");
            orderInfo.put("fsName", parkFsyyDetailDTO.getString("fsTimeName"));
            JSONArray certAuthDTOS = ticketVerificationDTO.getJSONArray("certAuthDTOS");
            List certAuthList = new ArrayList();
            for (int j = 0; j < certAuthDTOS.size(); j++) {
                JSONObject certAuthDTO = certAuthDTOS.getJSONObject(j);
                certAuthList.add(new HashMap<String, Object>() {{
                    put("realName", certAuthDTO.getString("name"));
                    put("certType", certAuthDTO.getIntValue("certType"));
                    put("certNo", certAuthDTO.getString("certNo"));
                }});
            }
            orderInfo.put("orderCertAuthList", certAuthList);
            orderInfo.put("needConfirm", "F");
            String fsTimeRange = parkFsyyDetailDTO.getString("fsTimeRange");
            orderInfo.put("spiltStartTime", fsTimeRange.substring(0, fsTimeRange.indexOf(" ~ ")));
            orderInfo.put("spiltEndTime", fsTimeRange.substring(fsTimeRange.indexOf(" ~ ") + 3));
            orderInfo.put("wayType", "6");
            saveOrderList.add(orderInfo);
        }
        param.put("saveOrders", saveOrderList);
        param.put("orderType", "park");
        param.put("wayType", "6");
        param.put("merchantInfoId", "2655");

        return param;
    }

    private static Map<String, String> getBuyerMap(Map<String, String> iDNameMap) {
        Map<String, String> normalMap = new HashMap();
        Map<String, String> oldMap = new HashMap();
        for (Map.Entry<String, String> nameIDMapEntry : iDNameMap.entrySet()) {
            String idCard = nameIDMapEntry.getKey();
            String name = nameIDMapEntry.getValue();
            if (idCard.length() < 17) {
                normalMap.put("name", name);
                normalMap.put("idCard", idCard);
                break;
            }
            Integer age = GetAgeForIdCardUtil.getAge(idCard);
            if (!ObjectUtils.isEmpty(age)) {
                if (age > 18 && age < 60) {
                    normalMap.put("name", name);
                    normalMap.put("idCard", idCard);
                    break;
                }
                if (age >= 60) {
                    oldMap.put("name", name);
                    oldMap.put("idCard", idCard);
                }
            }
        }
        return ObjectUtils.isEmpty(normalMap) ? oldMap : normalMap;
    }


    private String customURLEncode(String s, String enc) {
        StringBuilder sb = new StringBuilder();
        String[] split = s.split("&");
        for (int i = 0; i < split.length; i++) {
            String[] paramSplit = split[i].split("=");
            for (int j = 0; j < paramSplit.length; j++) {
                String encode = null;
                try {
                    encode = URLEncoder.encode(String.valueOf(paramSplit[j]), enc);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                sb.append(encode);
                if (j != paramSplit.length - 1) {
                    sb.append("=");
                }
            }
            if (i != split.length - 1) {
                sb.append("&");
            }
        }
        return sb.toString();
    }
    private String socketMsg(String title, String msg, Integer time) {
        JSONObject res = new JSONObject();
        res.put("title", title);
        res.put("msg", msg);
        res.put("time", time);
        return JSON.toJSONString(res);
    }
}
