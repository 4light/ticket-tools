package test.ticket.tickettools.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
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
import test.ticket.tickettools.dao.UserInfoDao;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.domain.entity.UserInfoEntity;
import test.ticket.tickettools.service.PalaceMuseumTicketService;
import test.ticket.tickettools.utils.*;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;


@Slf4j
@Service
public class PalaceMuseumTicketServiceImpl implements PalaceMuseumTicketService {

    //获取余票信息
    private static final String getReserveListUrl = "https://lotswap.dpm.org.cn/lotsapi/order/api/batchTimeReserveList";
    //校验成员信息
    private static final String checkUserUrl = "https://lotswap.dpm.org.cn/dubboApi/trade-core/tradeCreateService/ticketVerificationCheck";

    @Resource
    TaskDao taskDao;
    @Resource
    TaskDetailDao taskDetailDao;
    @Resource
    UserInfoDao userInfoDao;


    private String getPalaceMuUserInfoUrl ="https://lotswap.dpm.org.cn/lotsapi/leaguer/api/userLeaguer/manage/leaguerInfo?cipherText=0&merchantId=2655&merchantInfoId=2655";


    @Override
    public void test() {
        UserInfoEntity userInfoEntity = userInfoDao.selectById(16L);
        //故宫获取用户信息
        HttpHeaders headers=new HttpHeaders();
        String proxyHeadersStr = userInfoEntity.getHeaders();
        JSONObject proxyHeadersJson = JSON.parseObject(proxyHeadersStr);
        proxyHeadersJson.entrySet().forEach(o->{
            if(StrUtil.equals(o.getKey(),"Accept-Encoding")){
                headers.set("Accept-Encoding","gzip, deflate");
            }else{
                headers.set(o.getKey(),o.getValue().toString());
            }
        });
        HttpEntity entity=new HttpEntity(headers);
        JSONObject response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getPalaceMuUserInfoUrl, HttpMethod.GET, entity);
        if(response==null||response.getIntValue("status")!=200){
            log.info("请求用户信息异常：{}",response);
        }
        log.info("获取到的用户信息:{}",response);
    }

    @Override
    public void initData() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.LOTS.getCode());
        List<TaskEntity> unDoneTasks = taskDao.getUnDoneTasks(taskEntity);
        for (TaskEntity unDoneTask : unDoneTasks) {
            if(!ObjectUtils.isEmpty(unDoneTask.getIp())&&!ObjectUtils.isEmpty(unDoneTask.getPort())){
                return;
            }
            JSONObject proxy = ProxyUtil.getProxy();
            unDoneTask.setIp(proxy.getString("ip"));
            unDoneTask.setPort(proxy.getInteger("port"));
            taskDao.updateTask(unDoneTask);
        }
    }
    @Override
    public List<DoSnatchInfo> snatchingTicket() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.LOTS.getCode());
        List<TaskEntity> unDoneTasks = taskDao.getUnDoneTasks(taskEntity);
        List<DoSnatchInfo> doSnatchInfoList = new ArrayList<>();
        for (TaskEntity unDoneTask : unDoneTasks) {
            TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
            taskDetailEntity.setTaskId(unDoneTask.getId());
            taskDetailEntity.setDone(false);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(taskDetailEntity);
            if(ObjectUtils.isEmpty(taskDetailEntities)){
                unDoneTask.setDone(true);
                taskDao.updateTask(unDoneTask);
                continue;
            }
            DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
            doSnatchInfo.setTaskId(unDoneTask.getId());
            doSnatchInfo.setUserInfoId(unDoneTask.getUserInfoId());
            UserInfoEntity userInfoEntity = userInfoDao.selectById(unDoneTask.getUserInfoId());
            doSnatchInfo.setAccount(unDoneTask.getAccount());
            doSnatchInfo.setHeaders(userInfoEntity.getHeaders());
            doSnatchInfo.setChannelUserId(userInfoEntity.getChannelUserId());
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
        //查询余票
        String queryImperialPalaceTicketsUrl = "https://lotswap.dpm.org.cn/lotsapi/merchant/api/fsyy/calendar?parkId=11324&year=%s&month=%s&merchantId=2655&merchantInfoId=2655";
        //获取门票种类
        String getTicketGridUrl = "https://lotswap.dpm.org.cn/lotsapi/merchant/api/merchantParkTicketGridNew?date=%s&merchantParkInfoId=11324&currPage=1&pageSize=200&merchantInfoId=2655&playDate=%s&businessType=park";
        //提交订单
        String createUrl = "https://lotswap.dpm.org.cn/dubboApi/trade-core/tradeCreateService/create?sign=%s&timestamp=%s";
        //记录有票的具体日期
        JSONArray parkFsyyDetailDTOs = new JSONArray();
        try {
            JSONObject currentParkFsyyDetail=new JSONObject();
            RestTemplate restTemplate = TemplateUtil.initSSLTemplateWithProxy(doSnatchInfo.getIp(),doSnatchInfo.getPort());
            //RestTemplate restTemplate=TemplateUtil.initSSLTemplate();
            HttpHeaders headers=new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String headerStr = doSnatchInfo.getHeaders();
            //String headerStr = FileUtil.readString("/Users/devin.zhang/Desktop/record", Charset.defaultCharset());
            JSONObject headerJson = JSON.parseObject(headerStr);
            LocalDate now = LocalDate.now();
            for (Map.Entry<String, Object> headerEntry : headerJson.entrySet()) {
                headers.set(headerEntry.getKey(), headerEntry.getValue().toString());
            }
            headers.set("Accept-Encoding", "gzip,compress,deflate");
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            String mpOpenId = headerJson.getString("mpOpenId");
            HttpEntity entity = new HttpEntity<>(headers);
            //查询当月余票
            Date useDate = doSnatchInfo.getUseDate();
            String formatUseDate=DateUtils.dateToStr(useDate,"yyyy-MM-dd");
            LocalDate localDate = useDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int monthValue = localDate.getMonthValue();
            String month = monthValue > 10 ? String.valueOf(monthValue) : "0" + monthValue;
            String formatQueryImperialPalaceTicketsUrl = String.format(queryImperialPalaceTicketsUrl, now.getYear(), month);
            Thread.sleep(RandomUtil.randomInt(2000,6000));
            JSONObject responseJson = TemplateUtil.getResponse(restTemplate, formatQueryImperialPalaceTicketsUrl, HttpMethod.GET, entity);
            if (ObjectUtils.isEmpty(responseJson)||responseJson.getIntValue("status")!=200) {
                log.info("responseJson:{}",responseJson);
                return;
            }
            String decCalendarTicketsStr = EncDecUtil.decData(responseJson.getString("privateKey"), responseJson.getString("data"));
            JSONArray data = JSON.parseArray(decCalendarTicketsStr);
            if (ObjectUtils.isEmpty(data)) {
                log.info("获取到的场次失败");
                return;
            }
            boolean haveTicket = false;
            outLoop:
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                if (StrUtil.equals("T", item.getString("saleStatus")) && item.getIntValue("stockNum") == 1) {
                    if (StrUtil.equals(DateUtils.dateToStr(useDate,"yyyy-MM-dd"), item.getString("occDate"))) {
                        JSONArray parkFsyyDetailDTOS = item.getJSONArray("parkFsyyDetailDTOS");
                        if (!ObjectUtils.isEmpty(parkFsyyDetailDTOS)) {
                            for (int j = 0; j < parkFsyyDetailDTOS.size(); j++) {
                                JSONObject parkFsyyDetailJson = parkFsyyDetailDTOS.getJSONObject(j);
                                if (parkFsyyDetailJson.getIntValue("stockNum") == 1 && parkFsyyDetailJson.getIntValue("totalNum") == 1) {
                                    Integer session = doSnatchInfo.getSession();
                                    //判断是否对上下午有要求
                                    if(ObjectUtils.isEmpty(session)){
                                        haveTicket = true;
                                        parkFsyyDetailDTOs.add(parkFsyyDetailJson);
                                        currentParkFsyyDetail=parkFsyyDetailJson;
                                        break outLoop;
                                    }else{
                                        //上午票
                                        if(doSnatchInfo.getSession()==0){
                                            if(StrUtil.equals("上午",parkFsyyDetailJson.getString("fsTimeName"))){
                                                haveTicket = true;
                                                parkFsyyDetailDTOs.add(parkFsyyDetailJson);
                                                currentParkFsyyDetail=parkFsyyDetailJson;
                                                break outLoop;
                                            }
                                        }
                                        //下午票
                                        if(doSnatchInfo.getSession()==1){
                                            if(StrUtil.equals("下午",parkFsyyDetailJson.getString("fsTimeName"))){
                                                haveTicket = true;
                                                parkFsyyDetailDTOs.add(parkFsyyDetailJson);
                                                currentParkFsyyDetail=parkFsyyDetailJson;
                                                break outLoop;
                                            }
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
                return;
            }
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            HttpEntity getTicketEntity = new HttpEntity<>(headers);
            String formatGetTicketGridUrl = String.format(getTicketGridUrl, formatUseDate, formatUseDate);
            Thread.sleep(RandomUtil.randomInt(2000,6000));
            JSONObject ticketGridJson = TemplateUtil.getResponse(restTemplate, formatGetTicketGridUrl, HttpMethod.GET, getTicketEntity);
            if (ObjectUtils.isEmpty(ticketGridJson)) {
                return;
            }
            JSONArray ticketGridDataArr = ticketGridJson.getJSONArray("data");
            JSONObject ticketGridItem = ObjectUtils.isEmpty(ticketGridDataArr) ? null : ticketGridDataArr.getJSONObject(0);
            JSONArray ticketList = ticketGridItem == null ? null : ticketGridItem.getJSONArray("ticketList");
            JSONArray ticketReserveList = new JSONArray();
            if (ObjectUtils.isEmpty(ticketList)) {
                return;
            }
            Map<String, JSONObject> typeTicketMap = new HashMap();
            Map<String, JSONObject> modelCodeTicketInfoMap = new HashMap();
            List<String> modelCodes=new ArrayList();
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
            String addTicketUrl=String.format("https://lotswap.dpm.org.cn/lotsapi/merchant/api/merchantParkInfo/add_ticket/query?modelCodes=%s&occDate=%s&merchantId=2655&merchantInfoId=2655",String.join(",",modelCodes),formatUseDate);
            Thread.sleep(RandomUtil.randomInt(2000,6000));
            JSONObject response = TemplateUtil.getResponse(restTemplate, addTicketUrl, HttpMethod.GET, new HttpEntity<>(headers));
            log.info("add_ticket:{}",response);
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            String bodyFormat = MessageFormat.format("queryParam={0}&merchantId=2655&merchantInfoId=2655", ticketReserveList);
            //需要设置content_type application/x-www-form-urlencoded
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            URLEncoder.encode(bodyFormat, "utf-8");
            headers.set("Content-Length", String.valueOf(customURLEncode(bodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
            HttpEntity getReserveListEntity = new HttpEntity<>(bodyFormat, headers);
            Thread.sleep(RandomUtil.randomInt(2000,6000));
            JSONObject reserveListJson = TemplateUtil.getResponse(restTemplate, getReserveListUrl, HttpMethod.POST, getReserveListEntity);
            if (ObjectUtils.isEmpty(reserveListJson)||reserveListJson.getIntValue("status")!=200) {
                return;
            }
            String decReserveListStr = EncDecUtil.decData(responseJson.getString("privateKey"), responseJson.getString("data"));
            JSONArray reserveList = JSON.parseArray(decReserveListStr);
            if (ObjectUtils.isEmpty(reserveList)) {
                log.info("批量获取余票数据失败batchTimeReserveList", reserveListJson);
                return;
            }
            //校验用户信息
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            headers.setContentType(MediaType.APPLICATION_JSON);
            String formatDate = DateUtils.dateToStr(doSnatchInfo.getUseDate(), "yyyy-MM-dd");
            JSONObject checkUserBody = buildCheckUserParam(doSnatchInfo.getIdNameMap(),formatDate,typeTicketMap);
            log.info("校验身份信息入参：{}", JSON.toJSONString(checkUserBody));
            HttpEntity checkUserEntity = new HttpEntity<>(checkUserBody, headers);
            Thread.sleep(RandomUtil.randomInt(2000,6000));
            JSONObject checkUserBodyJson = TemplateUtil.getResponse(restTemplate, checkUserUrl, HttpMethod.POST, checkUserEntity);
            JSONObject checkUserData = checkUserBodyJson.getJSONObject("data");
            log.info("身份验证信息:{}", checkUserData);
            if (!ObjectUtils.isEmpty(checkUserData.getJSONArray("rejectCertAuthList"))) {
                log.info("身份验证失败:{}", checkUserBodyJson);
                return;
            }
            String accessToken = headerJson.getString("access-token");
            headers.set("Accept-Encoding","gzip,compress,deflate");
            modelCodeTicketInfoMap.put("parkFsyyDetailDTO", currentParkFsyyDetail);
            long timestamp = System.currentTimeMillis();
            String ts = String.valueOf(timestamp).substring(0, 11);
            headers.set("ts", String.valueOf(timestamp/1000));
            String signStr = "VDsdxfwljhy#@!94857access-token=" + accessToken + ts + "AAXY";
            String sign = DigestUtils.md5Hex(signStr);
            JSONObject jsonObject = buildCreateParam(mpOpenId, checkUserBody,doSnatchInfo,modelCodeTicketInfoMap);
            headers.setContentLength(JSON.toJSONString(jsonObject).getBytes(StandardCharsets.UTF_8).length);
            HttpEntity addTicketQueryEntity = new HttpEntity<>(jsonObject, headers);
            String formatCreateUrl = String.format(createUrl, sign, timestamp);
            Thread.sleep(RandomUtil.randomInt(2000,6000));
            log.info("提交订单入参：{}", JSON.toJSONString(jsonObject));
            JSONObject createRes = TemplateUtil.getResponse(restTemplate, formatCreateUrl, HttpMethod.POST, addTicketQueryEntity);
            log.info("请求结果{}", createRes);
            if(ObjectUtils.isEmpty(createRes)){
                JSONObject proxy = ProxyUtil.getProxy();
                TaskEntity taskEntity=new TaskEntity();
                taskEntity.setId(doSnatchInfo.getTaskId());
                taskEntity.setIp(proxy.getString("ip"));
                taskEntity.setPort(proxy.getInteger("port"));
                taskDao.updateTask(taskEntity);
            }
            if(createRes.getIntValue("code")==200){
                TaskEntity taskEntity=new TaskEntity();
                taskEntity.setId(doSnatchInfo.getTaskId());
                taskEntity.setDone(true);
                taskEntity.setUpdateDate(new Date());
                taskDao.updateTask(taskEntity);
                TaskDetailEntity taskDetailEntity=new TaskDetailEntity();
                taskDetailEntity.setTaskId(doSnatchInfo.getTaskId());
                taskDetailEntity.setUpdateDate(new Date());
                taskDetailEntity.setDone(true);
                taskDetailEntity.setOrderNumber(createRes.getJSONObject("data").getString("orderCode"));
                taskDetailDao.updateEntityByTaskId(taskDetailEntity);
                SendMessageUtil.send(ChannelEnum.LOTS.getDesc(),formatDate,currentParkFsyyDetail.getString("fsTimeName"),doSnatchInfo.getAccount(),String.join(",",doSnatchInfo.getIdNameMap().values()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject buildCheckUserParam(Map<String,String> iDNameMap,String useDate,Map<String, JSONObject> typeTicketMap) {
        JSONObject param = new JSONObject();
        JSONObject normal = new JSONObject();
        JSONObject old = new JSONObject();
        JSONObject free = new JSONObject();
        //JSONObject student=new JSONObject();
        for (Map.Entry<String, String> nameIDMapEntry : iDNameMap.entrySet()) {
            String idCard = nameIDMapEntry.getKey();
            String name = nameIDMapEntry.getValue();
            //如果是护照之类的直接添加到成人
            if(idCard.length()<17){
                normal = buildItem(normal, idCard, name, "normal",typeTicketMap,true);
                continue;
            }
            Integer age = GetAgeForIdCardUtil.getAge(idCard);
            if (age >= 0 && age < 18) {
                free = buildItem(free, idCard, name, "free",typeTicketMap,false);
                continue;
            }
            if (age >= 60) {
                old = buildItem(old, idCard, name, "old",typeTicketMap,false);
                continue;
            }
            //学生后续优化
            normal = buildItem(normal, idCard, name, "normal",typeTicketMap,false);
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

    private JSONObject buildItem(JSONObject item, String idCard, String name, String type,Map<String, JSONObject> typeTicketMap,boolean isPassport) {
        if (item.get("certAuthDTOS") == null) {
            item.put("certAuthDTOS", Arrays.asList(new HashMap() {{
                put("cardType", isPassport?2:0);
                put("certNo", idCard);
                put("name", name);
            }}));
        } else {
            List certAuthDTOS = item.getJSONArray("certAuthDTOS").toJavaList(Object.class);
            JSONObject certAuthDTO = new JSONObject();
            certAuthDTO.put("cardType", isPassport?2:0);
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

    private JSONObject buildCreateParam(String openId, JSONObject checkParam,DoSnatchInfo doSnatchInfo,Map<String, JSONObject> modelCodeTicketInfoMap) {
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

    private  static Map<String,String> getBuyerMap(Map<String,String> iDNameMap){
        Map<String,String> normalMap=new HashMap();
        Map<String,String> oldMap=new HashMap();
        for (Map.Entry<String, String> nameIDMapEntry : iDNameMap.entrySet()) {
            String idCard = nameIDMapEntry.getKey();
            String name = nameIDMapEntry.getValue();
            if(idCard.length()<17){
                normalMap.put("name",name);
                normalMap.put("idCard",idCard);
                break;
            }
            Integer age = GetAgeForIdCardUtil.getAge(idCard);
            if(!ObjectUtils.isEmpty(age)){
                if(age>18&&age<60){
                    normalMap.put("name",name);
                    normalMap.put("idCard",idCard);
                    break;
                }
                if(age>=60){
                    oldMap.put("name",name);
                    oldMap.put("idCard",idCard);
                }
            }
        }
        return ObjectUtils.isEmpty(normalMap)?oldMap:normalMap;
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
}
