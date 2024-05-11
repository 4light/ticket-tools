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
            Thread.sleep(RandomUtil.randomInt(3000,3500));
            JSONObject responseJson = TemplateUtil.getResponse(restTemplate, formatQueryImperialPalaceTicketsUrl, HttpMethod.GET, entity);
            if (ObjectUtils.isEmpty(responseJson)||responseJson.getIntValue("code")!=200) {
                log.info("responseJson:{}",responseJson);
                return;
            }
            String decCalendarTicketsStr = EncDecUtil.decData(responseJson.getString("privateKey"), responseJson.getString("data"));
            responseJson=JSON.parseObject(decCalendarTicketsStr);
            JSONArray data = responseJson.getJSONArray("data");
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
            Thread.sleep(RandomUtil.randomInt(3000,3500));
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
            Thread.sleep(RandomUtil.randomInt(3000,3500));
            JSONObject response = TemplateUtil.getResponse(restTemplate, addTicketUrl, HttpMethod.GET, new HttpEntity<>(headers));
            log.info("add_ticket:{}",response);
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            String bodyFormat = MessageFormat.format("queryParam={0}&merchantId=2655&merchantInfoId=2655", ticketReserveList);
            //需要设置content_type application/x-www-form-urlencoded
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            URLEncoder.encode(bodyFormat, "utf-8");
            headers.set("Content-Length", String.valueOf(customURLEncode(bodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
            HttpEntity getReserveListEntity = new HttpEntity<>(bodyFormat, headers);
            Thread.sleep(RandomUtil.randomInt(2500,3500));
            JSONObject reserveListJson = TemplateUtil.getResponse(restTemplate, getReserveListUrl, HttpMethod.POST, getReserveListEntity);
            if (ObjectUtils.isEmpty(reserveListJson)||reserveListJson.getIntValue("code")!=200) {
                return;
            }
            String decReserveListStr = EncDecUtil.decData(responseJson.getString("privateKey"), responseJson.getString("data"));
            reserveListJson=JSON.parseObject(decReserveListStr);
            JSONArray reserveList = reserveListJson.getJSONArray("data");
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
            Thread.sleep(RandomUtil.randomInt(3000,3500));
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
            Thread.sleep(RandomUtil.randomInt(2000,3500));
            JSONObject createRes = TemplateUtil.getResponse(restTemplate, formatCreateUrl, HttpMethod.POST, addTicketQueryEntity);
            log.info("请求结果{}", createRes);
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

    public static void main(String[] args) {
        String hexStr="6a2aaaac7be538728542e0c4794c9d729bd1878d81fb23db8947eb41d4d53206c05d4b73f3cea11a4bd20f1a8f5e9799b1c1b417513f7ca2bf09db6ac7d0e8f91c09fd0537f7eb6770fef4d623d5313b0ffafcb735b1486e0453657fe46f863ee9c62bb55c05e9634eeff809f8fef45dd1f5f5e41168f43979f0c32aaf93b10566266971547094ed7ba259cdd139c7130d43a3a92a66393888b32e727e4361d41a6818b98a7ba93454c2a8b400c5f30ac20237515369bb1d3c60e2f25df23f27a95439aa8c8fdd324745b34daa934fbc194a32d97add5b3e59640dd0f36ef8daa662538d20334485d3b0c20c2cdafc62a5a0d38ebb95b66adaf6e8e579d745528a7eabe5b4acc63099d679543a867c473ab8e9534a448fb4bcba6634a9a8aa57c73821b2ac9b64f091d3a5e53b1c1f58dec2fc914d355f338f691b1db880758fc74580f6a0fe58139bedbc0e44d32a94489d6115108b1f4127109e7a3ca71cb302ed720dc4b7c5bd53b0bf596e281350a272de63c55590130dbe6a9eb75a92d5163c954d0e7f29d6ba254f4a6b89bbb7e4b6d1cb3c1b5e2d680015bcf64fe856984c893b8bc96120e2c3158bf164f7e677d42919c96e0f57a11e0ac8e7e27d23f25901088634dbbace36a2c4e949335999a7cf92f27d78562892296404d7f2661bd161eb5bceba275b0353f60dc8a5a05397c9f1328ca5b148b1d142ee5e30239cb5bc3f6d97f7bbb5f9013d46a246b189484ac21adbe3f329c79993a2f7bae9bf1ddff7aac5d0e8604e6e25d8fd692e35547dd5c238ecc879a1121b36da1ed3d80aff429eb272dcf1145d343ab76f46aecf3a747db70b6e50d237300235ceed5fe24264e7be974b498a3da492b4c6d66d15fa98be2b8e43c23600941e3245d30585e9e9159aa758a54d6b39c53d2b7438753bb1e071229ef6b30ebe0918b4bc9dcdee5eeaa535938c0f7a22102c02605c57ad212640e5a653d26331cdb7fdcaa405155acc4f97b74600512fba88b63bd7dfaaad15580683fb519835ad67a97bf9ad58df9f71e93498dc4322864c1e0040a379693c4735c5efb34e71e95933912f057b39094ee3c209de318e788e639cbeaa67b714da7662cafdbb0c97afbab4b7e4b3109c62981487689caab38ac4b5c40eddf4c57dd30f8f460a872563eb10933468c601c8afdca646d30352c1a049bf89c07dd5dc27af0b41942a61491f207614ba016b5ee1b67b654bc23b8b1bd4538faf46ddbe3be7c5551b248fac95412ba865a7eaed956d746e248055259f8c15fe13ca3b157d42f3c19f25876a5984324e5501e66fec136652d75b5582accec7c99938da5b50faaf85d519d87c970d5be6b88fb92c2bf724cf1c0e223469ecf2dbee79d2434abf69e6a481b7e1102550ac1279cc366a0857b3181d9d042c5f4d503f858fa1a613381d8832792e01e9ea59cf797675f202fda7dfe0e37f821f81a977775158dde695c004298ca6b962dd9abc19ef1d0ad6318ff38ed41cd4c01846ff16a842ab3dae065a99123c58892f8034d98a640ff39665c4aac0581df590c1cbc4a5a5f75bc861790a1744762518631c4c3c38df4483a7eee5577ee48c5b89284b903785d51582f144ef9c6b8f812c9c396c254cb0e126c0fc335bf95680d18e86f924777a4bc86adfc1597471b17b030db56d51f820d190232f426b0a92fff80432f8754ceb66bec3b4f9ce3870ba6fbc431f5311d895d9858c651055246bbbf34bf43c4207a50be9207a929a311e5ceb01df65f4c44f5e680148f0e16dea2c4079a997698a79b7af6c743106dbdcdfa7a5831e49cb907eb3ea78a8f675b5ff3ea2c7d4e1874d77f71f6702a6c4befb599cff9687327baad1bfc4ce8daa46c5fb1355c799b4b753b94e4708164512788a240781203e09a9f23ac3f1c6e65981679ad7593715ecb990779b3a6aaac1676215dc116b46e6f931ad8332704bc50f5c637677898e56704087678a7d25549660291fadb5047b967db8d5290b34615a9eee7bf5cddc98261fdcba86e7140eabbdf7ef5086073e35d87532561b02d749c18fa4e0ff04a88e96f81f3ed5e5eb3a67ff851a7ce0a599a70604b47becd3e94f2d85e1315878a140b1d81aa9a96ae053b72db4a0fd6ab024f1c121ec26657fd86e0fb5ca0afea088f5a05d2e3b6c082346f769d0919140f8114ef09299dfb6a6c0496fc3cdd3fa629f84d1d2486480af321fb396c5b49de5fe617a6aca5441fb0e5e9eb33563931c8c940af1d3ff4f9fcedd46851d755a158012b860d30c40e7fba516a6af868b6e7989020cafcb9a67b1cc7d8fa681382184e09d8658f4ca4ef38466e5621e7af072e48ddc1d79976dbc20c5ea95d963cc47698675c0391fe9f136f33c7c67b6da915d30b447cebf515b6b29db150d18cebd9c1e6aaee380f9aec90d0ca21fc716e319283e5f9dc0b8891ded227d1558ccf316edae5334977dff637b91ddc42871cc5108687440146001c6bcd87af296a2e38102d09636d302e20dbb246235479d3e91e5155e87f9cb08dc8944174f211269a17bd917d53af6ae2f8c5f3d9a77d91485a3f9fcf35cb64df6d12148b4535f78d3cc8911300d9fa8afafb24fb80475946039accd9c8c4d1dde10f202bbd2a629c1655a0afb8fc3885edc681afe06f573f5dc175399026cffd2881d8da23d65a6e6249ba114263be42c58e4f8a372bc480903259966bb25b5fd1abef2a66ae2da0a578e6635a67274a8b5dc27cae4bdf9e20e49a3d4bdc71064aae2de302686f7185793373810c9776bef8ec85f87a411550d74be3280c7d3b96e46e09b5865f822d0dc5eafa5a789be8eb3f9930ffb700ef43b95df0dfc67b7bb862ddf62ce774009899315172ceac8db7704ac7d5fbe2bdad044d485e42d8a2f518fc74d1d95e5f70b609578cbcff3704fe0ff32a6bd5dd82e1d63c78bf7282e7ff99e41695388a94c2d1173ad57649e4e3ad3ac6350402c3da3dfa844195bbfdb12b6e466b87c96853c4c8292100c6dc869b80fa640b320ca38c72173d38083ab9bccd89efe73c343b65050f66620b38c332881ab6d0f038879fa4b8adebb3723221b458d137e9e82f94bd3110edab266717996ff677f45fcc0341032f63eca67283b2ca8065f90503cb3c2a694c941ce8f7b811b7107c1603da2c835484e61e8601abd1529bcf097f4e3e34cb99de17d9cd48a34ada8e895eb5315b111ee8376c6eb7154808b66df6c0a6875e91859acc2a04770b2b9176ddc39991f267d09423520dceb2faf0812654ee0810d4c0a5fc04a8c831cf8e35bad50528118f7a12f6e38af4d094118d9342e139ec6cb273a9e4de35b6763e56ad10ff71ed7716c815b76c90e9295d0ddfe1ff33ca977a23ee8bc131733def435e886cc55f8f034a286527e568b274abb7c7324bb4a28da35c3340917d701aa4c9e8e3db7afe7c5fa2491cf09576c668bffd610514737af67f525d8248a6d8419b51768a3814c7bcfdbc24b1aa6aa67c3d83fa5b5a151cdbeb6cf7913a50850c9a7a59f88707c99836d756be96eec126d492b583c16ab3d03fd6e9c278a3ec41f477708bb6325d1b4c591797bc5a2ea79d956ffe6679fa5fbc7481658fbe6b4c265908be679e8ba58132a741cf87e8813756192f215b32c8a03e2a5ca865c8dad27205d34f8170f562328997efcff60b22262c20265751b4de5d555c85420acc960cfc59b3f5c6141a6e0a959d9944d4ce02980d86bfb5c2710a27a2d93b6ffc06cdf1145d572ec0b244a728c89c7e2f58e9a378e25270641f99bfb786259a814a9eb49b11b515bbaa355c0d5e4d5eecde73d726dbde419a7a74d00311a164da994aefb56b6a8c6b055adcac88c3fecfefdb8ea324ea2fe35771b747639168744ee8c74a942d6ce24c6f885adda0a892788143188c678442bff8455fa5a4572d46124d58365ed85fbdea32090daad29fa0119e46febbab8e2b03f1bdd952bf9d74940b673cc11d9e95be7fbb5c9238d1c911fc3f78af8129bbe7fadde75d635b71788e033af227770a33b811736d4c65c8ded30fdfb5e1ada9fdff0ca3a156423955f39564dda1fb9ac68b2e25a6adb98cf35eeaab622beb604fbfdce690ceb56c6a796dfb3957b90c7b938bdfd968c416313770e74c65c6a704d13cf28ba6ff8eed9455ec1114584899af142213b0686b66dfc5f6b28b34576637336cdfb3ab3ff26ac97133ce0fc114cdaabf982480b0f8e360d0cf1c37d892bdba892e48009679dc003f656e32e6d7947fdbd97190ce7e273dff3b57ce39415c7806bdc12e17d805186f14c51d4f89cb535504b44cb2c26a9ba04abb6bf6f3b2bab9b42ca549b973bc5ffcbed4204853e9d55c44060755094a8ce41baa7d3a695826c6625d7333f55316297d232b8edfde58cc489f1193d6982ac4489587f9567927f1e9d33fd12616c53d3cb1314b07cf111c384ec785355afbc136905618cca4e426804c34011c9fd1cff3bff19c60aad109b94adec00f5cae086d0eed411db08583f3eac1be686e913c9fa47b56bd90556d7f73d90cd8b18a3aee278c5326280222f9c5a6bf1dfeb35e11442b044a977b9f4ccb0ec86dc17fa55e5057fbeeace2d1ac62c972309c2f80297d51287d5cba8297eb0b89ee2b1446fb8a64a11c3c3e779c89e3a2045c2aae969e403cc67cd512c10f5aebbe6fe5ad5b2a2719036cf8bb493f7c20ff6c4517489740667e26e899c1b5f53f41ae7ba2ce9842864f6e274d9e42933f65d365acc2769a07dc439fae54b1e3b853bb46cfb4cd8180ba34f0a7664c107843831ecf39089ce79c6add21b58ac741561c099ac643645fe9c1359addda6a4fa681ea62bc6bcff6445e1b94188c4629e18369c9be9ab2406b9e5a3ebf954e1b0b37dbb20bc236ae71de2b654c8d3ae079a6cc03861fb7f5d6f5b29b89307dd31307fc0de2380217b939154e5ed9f90b8eba584e6b5aad8d437ce67052b4d7fa7fc96b27de52344d0fcbdfae5b8723a761d197c296ac0504d33cc5f8aed7dccf452c935f62dfc666de8ae4f2959bbd467fda1b8f38fc698ade4fa82a4eda36960d7a81ff54ef4142baa7280189a583c972da52350469ca5da76657b5bdeefef35e20a8f5b206a8803760b63c701bcc9a149c9723e678756502f7497fccb3b7e20c15fc328d49aea2097d901ed11c657c0c1124d1775ff8ea58f418bcad76f9f0c718478df800b9384076786fb35adf75d3dca504c5fa650035bb435984136414f64051b03c77d34a5694a375334a4fb4cddc35a9d457a17d153a1a09305bd59c74314f2ed8fc9d17617ccdf23e3406ee2020dccda134503028b8bbe64c387ec0c73dc35cf1739b99756840673c00be007c5fec869d7d0e1eef0052d0d6049506fa99260e733894348b289795f01599bd02baeb6611c827437f71cad6137082a15cce5916bfbc08ec0013df58aa97d568224ba51aa58e251a4178799215a8f28de59526594293d728441380fcbd9e0a26dcc060f152a5bd7bbb0d3d924d3f27b84ed75dc3dd9a44385bcfa2d1036d821f9ae1f43beb411e25a4a2177139ea1275f08bbe49bdee44529167700491da5028ce850a38093d63551ed07b159c7c9128d25fb54dde4fdc621ba93b71e44745a4d91ad1a90a4afbd0cd05f2df5722ccd6e6a191394ea6805c934b3b35a69ecf448184f94caed8bb27c24070afcf2d3d00dcca2efdbeb12e91cc02e4d5f4c3b8d90030ba0b36e9ba697bec96c75c8294525d9a530d07628d56a2669b56ec9e79b363bd9d560018649656eaf1f947a321c62ef067845cf36ce22d74c2fa6bd9dc559c0cf33bf1b10bc902294b427604cdfff2d9eeb08e9bbac7b70df230cd20144eb5b53693e32f146d8d35f13ac8c54813f2d6ec4134de2eefe7a12f616ff6bfb9e4bc55f9457b6e8f0ad44b95f6ebf50b2dbfa5342bb9f7adef0af682195a41e1841e6e2ebf51305b6bbe5098d2177cd15738148de910d40456a53f27490987cc85baaa7f7276d8c83d55fb29c2c1449b635d8de9aeab82155198e39202e204c42c68596075ee4d810ea3c9261800163af18c53c52eefdd5b563859fd185f5b9e2fadef7608c7d20259413d4c88a01c0aa006970f3c603a4ff7171b9bbf71b6cbc3abc565b1010abe6b9d70412bbaa22140feb50b5eb1e61c25cd9655c1b19de33d3a430e9ad1d10af25ee56b7e898582c360bff98f13a4bb8b17771106f12d03cb31a3c08bbac9f2a3ac5a844e898866c4536e27b474a726650412a021914a2a6df84c4950dce7dbdec0a4ae6f261bf2e78590ec7b8f2bdff0f4d2140228475061b106a84352cef90c39f9484e122f7ce0a45c2d6cfb1523453d326c7f7ac173e3dff6f36fa214f67d64c144c1a0d24a5a2a3cbff3fcc877868d0cfaa4cd8643e0e4196a314e7f1be868ea2950cdad627ae740dd6f621289e587e36ab74cc5449c15299eb7da9eda6c73881f22a54c80aae25cb06eb4701af8b2dc5d0a40d872f19905cbb7da069532e00793907ccc27e6166710d760ba62e29fa976d6cc4e3c115e902173f8fa4ec97be9a23a92eba9917c5517a0794930d2e8bebda87bdec68aef541679e20ff6354b7e3c435bb51139f7d265664162c388a82948c434b653aae03706b8fd588720ff0e8ddac4260540c2afa7d27a9e36eeeabc56c63b7c77b99479fed0c830b67f067b96d0982f61fd4e2c8ef5f06d70344057e8e892dbe219a6d934c9a5ea87b4a93c0609383991719465d734a123a7f6136859a8b6d41d425b366f90a7c1160c37ae87a59ea90cdb1dd49345bc8120fe53d4066099b682c9d472798a52cd3b411769d895ec1b7395e9ec783174d0c5314731f91d403c8f2e045d4b70f24a3dfe81a9dfda8b3c20e2118033d8b7733fe557fe76bcecf659bc66e96b8d4ec956525d4763192d020823903af2dee4aadb9aa7dc4932eface6aa5b97637baf4494291798af04dd96d077f4e1a477f212414d45ed941fd167e710edc9f46f3d0fcccc7454067837b63cbb3f0330c2ba163189e70807957111f5fa461032891985a53b0711786e4c2e0bff7b77b8485bf1f67347049cdff0e1319c066084b027627ae048b9b68ff7fd0055bbe1183414abf3c58bbac5e219bf8b28bc7c7e7936790a3edccae4af68172cc2dfd166894c851ecb8fa80b4d74535a5066fc18b555a73ad2b48bdef55178538dc05670522ce4d7551ba75edfe407056c72618bf743f07e6c01005e7d4f70e98e9a42f28a0e87588e5f19d8a7d3ae2be70317f161b0054a2ceb2b24f18c02eb58cfc9b9ed47153fceafd1cc86cfa869a856f5c3814a96026df40c19ff4a772d964500a7c9010ce3fb608b59147e9cffdaa91ce604cdb312363581092adba034a0cf44ec074ee6e244a85fcb23ea1485511c7ae6d390fe189ba0fdb6a6dd34687c76d08d3017f489857aa993155d05efa6a928d3da1af6c013137056aaec8328d03f24e3edaa37e8d2d582548d9035d658f3d11c9f5048a5f5bb8285fc220013139265f2c4996c2d7c94327de865be213993e7a574e94b24da39e07a94bbfeba869226060b90014202df5e0862be54dd89307dc84813764ebecc884469dc73f4efdc16f8d6589a5d042a9ec32c5d98067bbbc81c27972c81d4e8919b5c1d67ed04b016c52d900c049da28c9da9d89a6a4b7a1762ba1dad13170039194e2cfd1885e5908a43e52d49efea4e109c7c1cc7501aff1a4c5e5746495684b4c8aebf95962fb7a9214a4dfe56d60af035f961930ecb91f0c8f1552ca167eab6ee884b689cad8c1af1d3a28b88eb943bd5d36cbc034fc1669cee50b243565fd519182149345a4b7c6640d43ce481069e1b55cb40c7bffd280467d8cdfec12965314598d6777dd156a30cacca491c022ec5396ade739f8ced6bbdf38e65d6e41bde82eb121bdbb7f553cb5cffc273a2dba4be87ba500b96cfcf31a797f6ae78556e1f9603ea2c47cecb085f709c29887b157a71d1495a50c75b5caed0d4c3d70a23c690c19d2bb0a387ae63f6d7b840722a00cc70afac1d2a0cbca84d109c594409de601cdde8caf60718ea51a25e04e1a0e5310c4c2be067bca1b6405a85c619aae82da4e73dcb83df4ab24f79b40a13f9f3e015ac32440f3935b14c246b5576b0d952f5a2e04b80a256b314e4217ac78a251098097526527f743e752d1be9996137e162776fd0793524bb288b79ee4b72df8094d64a8b10c1da9c371bde15686157bc20f4e93b3c9f28991c82e4ff4196421a09dfc722fb0f814774ccb234d08efb3628dd8e15cbc9b6259a81d0234cbf5d3be45ed8b954c6926cd63047247cf090c90a789fe1acb6967a7b0a6d1d9f2c0d20509ae191c24cbaa11763a25e80a54e97ddf39b775ab71f36924520207fcdbe3155f2cdba15ce2ca2ae299372ca3eb0d927df17ba41d0640de9f7aab6bd4f8b4de9e87e984a7916a55b1ce096f2c70fe7e0ea11b5c151dbce23007d46221ae2d240a0ce3cf3c0852be89ca0e0a19af010724aef9ec8c897ef8687b81f08aab49f8d1e9578eddf9c6eeb4583f3b67acde969663092eb3eacb9c78307a94979b0b919689bad4b583da0e992e471dc81f32e4057face55dd93ecfdf040c935f615e9264ba23a36521661055d2d2daa7304cd03df00180a597774cb90727a00d86c9f07116a27e18f33bd88d7a451d911ff131e7c944acc534eabf98b7a39f89bc2759a0acc18607fe9a5c1425aaaeed58864c6edafd038065f4d117e30e5feea5c13472d6e3f89f63d42a3e58eb6e60b37382144eb21934c11b413fbd4b1aacdc89dd75023e785b83a8ad1d6b9bcaccb4942366c17d69573a7f8881762163ccc5cab4c162306df66881c4009cde08a86d5b83d2dc2f3c24c8b7fa7189ebabab76cdb2600b4134da19f04b5f35d116be360bd3eb456194fce6a8090a7f604f8c920cd7d90f898ae3585c18fb63b8eb3188fb8e21f6d16a9d2aff8b9ec6aec679ae3cb7b84bed42fde14e7ddbb479d18b04d1bdc5dac546d05acb6893a8892f4ae2132a80b5eb6bbe51b38274a885ce94543c5a1698e4e2a9e325da68a8ef54a3e792739fb4bf911f24b87e395b68e11631e8bcb0367880225c45fe33013e3f840afd46ef3eec6bc9699e58577a9c1894136ede1ed48e60dcd1d02362c6a381faa49272c4f872f7f245e41b49a47a900057b9474d90df874872db168170eb196eb3917f8e7b95ba2deadc60494a757d4e74d8694b20739e05014fe7a4e74c7c02d08f6b1ced4ee4603c79e33b8ba807e6bf0ff631b460c25f7d5e9754f59d541f5509569a584e6a6f763dba2c54a86d482c709c9aa95164aaab70ebd8b0ec9b6d6b6fa2f25a239a15e2b0d2b9ce9a6803ffd8f022e11fd71483cf5acbd58a0f420ae0d6e0d374ea9aefe32b1a7bbd1ac95a52d75d0f10fb7a72de24ffa52ca6289d6969b4bed77f41cb4d95269af8d640df954c980a673484c008785c1d1c8a9950538e4773d6b1b82dbec6a1a24c318befeb7955eda0fc715be2af36471cf563f0597dfedae5021fa87309966c0634738a14170cbdf2c6a1761557463f0be312ca8c70f162e19c4f1cc0d10f21acbde433cbe434cb5718378c828c12ad490c9774dd3d8b5a924c36aaa26a4a7d8e3668e1d8f5e83cd7d2415bc92f48a5167fb40448252571cc2fe0887f97f722e4da2b7adc62cb3939faaa020b883b24a46136cf0f98d1c7e26c1f2b9147ad0aae1a854dbe409b58c3839b4e7582b322beb3c2d882ca6d071a249aea31f77cbbcacce5f1df82d0fbcdfca88da037547aad971f438892762d3fd2aef515207b2370bc5c4ee2d9770f9ac21b349ea451613ac10f5f12f79c9a72599206c7ace3e07ccdae7334bf7f9e4f7f5b00cc4688af98daae0646d75afd57687b0d6543a8c3d7179bcd19832f0f7fdc75ed2053e0d773d1a75bd4c72da15f1d4a5a5ed110b6fc2c2988a3ad344cfa899a676f48ee46a00e0b1443eac2b390e1a0451e21924f7cb5665746f1421b217fc7e250bde5f12fbc9b7a6fe0d5ac6df3b692ff59b46f95b92dc8bd7420ec267dc2e6d1177acde844980c9c1a45c697d2a366a616a139045a063bcade4a7f029c51f39c7b729c9fb2d87fe12d29e2588dd91fe20ce803e71bd40ea0017111bacd6d6e0966bfbae0c668db87bea3425be1e24cad504e8c13d89c418a9a534866b87fb50b2d72e81a4a16f2157bca11ac66cab69c4004c774a2a78d096b771eb3f4f8de7c0eff1ed7abae2fdb549dbcaadf03c9a5d54904ed137c033aa58670b15a858dda4a648148050580f4d933b5327dfe230f77820b8ba604f6b2822f65637b7a69d5d902f4cfebe914ef0aad4b62be42b4890d72e857fc8133e97bd3fb2b5b2ac088cf02235eda4e53c7b96d0da7570d63e5e23978743c93c15b84c5a1ac1d29ce43bae45b7b4a6d4c801c7de3533aa80826441cd2650fd720b3c770224c528234e918f0386d6897ba5f52b4423017658d02bfe6843ed2644ec1a525268944636b343695569de9f6e8e82e36319d0d1455382dfa9244a58bc4cd6e2b8e0e5778c49960727e666aaa12af25a4698dd9261fa2d6754a99f987e4b13545216b3fdb9dfb9595c0b413299b17922c6d4337825fc86e73439ebae6413c1c29f63f4123ba2e67c4a0bbc4e62b344b34587e729562f90b61c3c1593249e0967ccebe6f9abd556655d674da84e90cc40766bfc0c612f8c1c576daa2278cd7a22f3ebd9774afcbd7adc7b5a156f828d30f27285c8635d8fc9b04de58622c29a29126c685c55ad5759b8fb1e8ae1234886364767f10c96c1edcf92c36892afcb617848c2ef193b4432aeef4aa7cfef0cabada5019727179ac8c56ec479e863b972c516d5908e8be6e3da0cebdd80aff573cc061911530d3db95c704f67a2cd77c89a945c6dde07d6348cb075c9a268bb82beb80a30b52572dd9b20e28a4175683d0fc8b251022e2b1b0006536a41d14ebb2ab6403ece3be0363e054ef673881576f745fa2b8773fbc9b4ff288c4248f5f235c154206d5b9b0b1e493e13c9504fd7696ffc56761b35f964bf6f0c9cc7c083e75c8a6b4ed4d6f90d7ee674e52a33f4fe237c629aac88f3ab29cecd0f7ba03863ffaf2779bfb135ad3f4e538bce127f82f4f1eadb664b1074dafb3d3b44d448427c73e2b4750a832bb23f4cf7caa13e6787d9ef327983c83c03493a9f7228a282504f05ca23142dbf63d94b985e72db68c962b394a1dccab7304f2ac850dcac3788fd6b3af807543bdca9e1b477765eaedc5513b5f14b0163185d89e0cee58b29f11ef95b8a3594da0c2390ff0d50fe0af79cebf3e4ea2795a3c507185a8c98452e999f870bb87b86c8298bcad95465a3286464fd0bf30fc273b73eee36087eaa219123b8681b9ecfefcfba9734b4970f8139e66c143f55aada5771511f6b451a1c7e10cda5a5238d8e088b6a2b3ced667293c7a7333ab8714193d0def2b4c1a90997f8ac2652357dca395399265f7ae80f3039eb41e2bb66e343e00ec652fc9d9f3f753bfca52fc271db37b7a04b091afb25bc797687b7575bd01f173054ba32a839f05c70da3b49333f61c010c8c36ebac303ca353d7a6f29d50e09f3a5ce6bb2494be7ceb2833179e6073cd0f12c4bd33df26853ffab4fbc6792ebf92e9c4d93d4fe76c2ed230a85a2eac2ce70718c68eaca0f699432a9d5c1d37ee711833fc830a1dbeb81b1154c2992f6c6c1a48cbf9d279671514cf176221e10245024b0a65b8bda3108dbfe1ce3b10331e3f9ba7a9893a7ea30e5f63036fd9860cc4b10f646c012ed2d5c9047dc9f3f7db73576ff1321226b622758d4082cc71e1529dd54114ac91016e1d96757381e0f55f5ca676341fc736fabe310f328fce8f31888535c2fac7103f45daee8617ac45945d55a60179141d790527a48efb52bf42a354f5d9be8af46e1eeb1dd1a01bfd3a68d3cc0e6795920f004bf5304368d3d5be2716a5f2b87d427e5e750dca918e7c199acce3011a89136f12b7295f724a8ac9563134a124855a8aa0272d8da4a62f14fccee76dea532ce4495c773d01d903d0ef687fbf420d88e4c44add2124058af6cb93d807236a5ac5cf4fc60e59c4e44ef9184b277afa2e2c6ecaccd905504122f7738cc756da3b63c41017e9efe2a5ac894f086327c966a67c7085e3aeb68b865c30855b141f65bb12bce06fc4e4e873d9f91609e99f915339aee8d41e2addc9c934a559a5cba3996dbd937023c68a5dce337fab5609849f4e3489c5358900f6056618798dc26d0da5796f5eeb6d14784e15aab277d0887dc49639646576d96285bb746589dc545a500735d95df84fd535e2699606dd99bfb1fb7ecfcc8c1419baf14e06b0aa28f4473d552e4cdf1e68fb5191c3de1b5195a53ceb1c1eada4b6a4752beaa70f124d640ba57d7e708b57f2c6af2bc9d6179b284000d2361b4b3461650ba90d305574be31e82011cee50518bf25ee6a9536378164198c4174628eec1a64d81d510298bc9bc2adb8dd045013b9b4acd9f706c03016ddddf16ca369f0ae4663c344be4d309b64d36790340eb151be68182fffc567790fd604cbaf185e863a96664fc830f538eef432a693c60ba82aa59128c70aa36ba608e69b92546aee2924d9c65515f0d7992e53765d0f6f6690c85873f6bd74e01e7c007cf27a08a5590076752ac301033ed4726f4241e010c1983969f332b17bf72924f2b40faedf3ffe593a7130df1e2e8125f0ce786a9c6c3e6a2f0b4d7e98722e710aee26c4130dbf6e315a89621917e4f1cd3da52c822987c872a16fc549313cf99c7ce82f647145134db492d9b1b61971b9176b50c70bf545cf22a7779a29c41fea86efd1b0157fed8beeb4543ee58e9a36a76987a7094634d4e9ac0e637c827ab6a784c75fdfccef91c8f83db12e68a6fb5bc4dc36848daee23cb03c14a08ed8e90da40f9af80e7fdf071c59671a81daa4eb286f806266292c5720f04f362abfd6239698fcbbdb35678f96a70ac47b5dee8c20c2bc17e56108035e4a4cf3e966755dc7fa332db1239c4eb9ac861316061c845245a201fe509ec722d5d44c88b96fefb1b972c6298ab30e28c4345ab05a4f6a1769a90d59f3df7832c13a666452d9a50b3083747de70a7a67988c51bd6ee8fbcdecb4d2825e80d7bd46cb83ffde1315c57c3d59fbfd7768320c8e3b2f20c3cab28759222bf04b129020b60adeef56ee2292f9769c3224d2bd46f87a05ef01d8f32aff2eb70193777ad30e8d3a29dd24135dd2f22b55b3866f56890851b281d38a3abaad5822a0b607d04bd5592879375972ff5dd7d863b6f36a281bab15e7a2c5f065002bed8fc504ca62f83363057e7e56d67571295bf099c89174d4b4338a5065b4b10746c9946670fab8211a61d637a9f79f5f0f077e4ce7a5443e2bb49d2b1c7f1506d945104c641fe0b33fdf571a359dd5bf06fd626b35bf9d28a01decef4d93dd4b653dcc1bd07e937b8eead28de9bb5bf4484810b8758a2a9de2843697cf0a94cb3b5b116b8c7110ecdb3ded3e71218e54602f68fadcf18591332227dd78c7e4aa71110bce1a3ddce1b34a63eb300573b17c679be9387eefa10c052c77ddee49c5399e6306856fb17c43b96c00e00ca9c710a3faf9c1a139120fffab550d9b0a74223c9e44d96c4ccac2f9378304dd624912a777581a349213e2d0de63e1f24ddfe86ed3e7dff732ae30f8e34d8228b129e42bdc606fd0972d3e1e810f7fecfff90aa6635541a33b668a725cd24276948782106241c61ee25bb3ed4d8d6f400e5325c886a29a8e7c3a1f7eb3a4f5d1d6a9f8d85d4cb32b50a9abadc04bca6095f11d5d19e4a590b450c0c498e20d1613d7307cf298201dce5f4feebf23b302b59be1c9e38d945d5517d5dbf5d73f9324f64adca4dd5c133864299828742190bf74852c167daa0e199626d5a566ec78a74af1c4ba7ccb7f7fe74456abc2fff6cbc1777f1d9441d8b9a15e7913e1c0ef617a77a6dbd302907a5299171f180200d8e9c22b4edf632f56b76a39f2126bbcf4e5e4de27682528278ed4922088048092907043c71575d46f2eba89dee692796b6608da9d8d9011e9c42f162bbc64169c50d08f2ba9d4c4a74fffd409af154171df661dcbf01f9e8e1137276a1ca61361d0e34d19916265601025439e93583f3467cdce6afce57144f54445b7ddeb9ede937b11bb4ed0b24358f6ef5834087778bd93b2df231b871874dd3d3833149d26102776537d62651307aea9a9e71de52442560b397686b327c719d09552c7ec969205f30fb17cc4af4c81de97e48d0a90ea1b110fc975b9bcabe8cee69ca864089d18f2810079e8897b777e3724df80366447283b4560f23df2fa2598bde3290eb0e57308e95893c95ff8075df7793c1ba5242c66ae10f447a404fad2848b8a41e9a6fc8fbf601ad725e1b557e2267e7c0492784c1517887c7b95dc3351b77eadc6b49aaadcfa63624e1178251c6e66fecb3e27a8fbab64504f11c015e8a74451ed5587e0e821a57da9906a814e2c6fc2aeccafa6537aedf84ded4da03ed1ab56d21c5a34863f16a6e6255165009b1310096f90c98d137a8e3d19519af29a11ec9f23ceb22c138c3fc05b5cda149ad88b268550d9c774f39c6c3fb508c7daec71cc7d39dbfb116bed3c8faa156a482fcec6c6f153b10989559fef336cd2e97510b0b50fd2104789fe0ba8f4d14122e910454937567bfd15d85c34cb881fabadf1f71bb6b53c0ab668319a7bc149b3b9d6a19f85c615f77e3ea7c42ded3585c82537870bc55c1a5ab384dfe01aa33c110ca3cf57f51843ff38189df95ee2f840319f51e92ffcb33e64880f570bb382a6f6fa2e1ad8b9012af8c69eed9bdbeb6a8ac6e1c412b68764f89fa1a6dc6221e3cae0fa528177b781fc5435ffef7b333de16f73a7992a839a7dd37a61504eb966049845d0834b5046e72bf053590be7382ff7f26c4df3801293663b0b9aa03f1b81de00c5d84255d7ad7d0c4e0028ac1fb488ef6c528a6e04724ce4dc68a246002b7d99eb142d52df55dd914d189aede0dd22fa609468dff0747908be8d5aa7b4be96b77eac840417038218938b8b7650ccd66ebf61daecdc024f0ad670d4a3a74fa7c06812e89564e76422df8d9da8ec80d19930e6613ac33f40c264e17fa6b87be3d64a55a69574333241807c4e06264b5ef8cb13248496808d12c7285a2a97d8645e71dc4679c89859bd9cc0613e2a82a22e30b01192f247d0952168f0f9cb2495ff5bd0f4309f4739b8d09163ec2f2b9578c878385b6035baaa3936ea82275d2a29cfbcc445554c8a2789f5d3945e7b7d49e2f4bf4223fcabfa335ed065ae813a337964821daa80c488a82554b070872e5ac5af10ae3f3e9189c2d3261ad8751429247fa83950e38281bf25cfcc20ca2c2c200002b93328cd6acd6fe61c27941ba5208a483ea502b0122056b159b32758c67bde6ce9a871669c0dcd2a7346c708b7a1a435b1aa3e53157a74283fdb09b878334f1ec143053ee2786a77248b53ec837c23f52565db3acfb0ca3510d1d6bf67b67bdd6c5712a6e90c8c02e5de43e70e1e3d620f94d53d464af0eebc959fdda5f35cb38c8566837c416e715e7f43a5a019a1440aa56e0219096a8a04e4da78726bfed9c44d1a4885c56267e94c788d9a5bb127bb92e98c1078f544d4812901f1078c9f0be18e90767875d47d00a5b4edc911f4441b3cc79d9e5dec973e3a281fd783d3ef83abc173f5d32ca48ab92c1a8034125e86ddec1feb979cd2583f8468fb84660a787fd7a02f7fa6537bb51ee380464bc7d75ba20f54fcd84586a2ace5b73938ec6991c2cb5fc57e95daca443dae1556017bc8282ed28aa1c24e8482ec7c06a6b1a75ea95e73a0b5428e813cc98fe2e893f653e98b0aa79efc64e44a50db8d9e22cdec93f331dc1b4f4df66d1c3c5bac50b338b56817bcb062b585b01e2995ea8f6ad8fc9cfc4364ffa3bb3ac77ed5c919ae72bdeb43df8b44d698ce3d887adfd0ada05e74cc776ffe3ecffd61d97f0d467891e908a8a54c4d5e9eb5fc8300187cc2bcb7762a15371f9c4f228a06bcc16ec611e97e2fb2b408604976bde41ca415d456d3ce8d547b1c6ea5cb5833673eeef66002ac7a8302d6ecb3e26893cefc7755d6671a58410363dcca209be7ac336ab4a251931f86a3a6be4ef690f67525bca0c5ffde290ebd131a02e689f8fc93ff9cc90fcc6b7ffe58bb928dfa0a24fdc60041f5f84c726288c209cb637c9c9ea4dd5b559eb59988380c8039462e6d701379c3ec7113060d98e04ffbbc87c9105c5557d499b0fdd4b83532923b44ad1a3ac968ced8858090f80617a92ac5b142afed6329b678d92e778ef111c340803c2d2daf153ab6de149effe865407206280c2010a298cdea83b39cc3634beddf7e4acac18de0b2a0a35eed03e7048ae3f85832a053ad6f7ba03c34b50d0c54572e6391e57a61f0d1d08fbb185012e67e59d498fb2aabea4956356172b9ae672e0af7e1c8f197b659575bb26b1c577c2a69a8b272f6502365997ef9046d315d35daedfb699d1fee4a346cd94367894fcb143daa46ae757682ab260f3bdea544b378661b6bc53a5e701766c9dd90b154517fe67f7ed2b250b399690a57dbaae310a15ed41cbbf79d65c4481bff2b39297110a8db7d787d18435cea41c37bbed68ef98589f621e6966fa4ac671ccd7ed2df8bf9947975bbb511bc45b948598e617400fa61670e747114b62346676a49db81bf53ea1af81fa5f5ddf97d488b0df01cef61ce5e99f9c4e1b180f1f46f1a96f717e94fb4323526df69f8013877680be7712aa2c21f2da3df7c614602afde3700a58f8ad7d36dc2197ec433ee6d998104e7238204f8ceb21c642ef61da932256a6ea395526e4c4616dcc2c959a062efb08c5380f50adf630bc98a8f515b7980424ff6b9baf190cd4d49755a6c7230fbe62f066a5efe1381ced42b22ab9700e84e767a8d61b5dc40499e292a9b3db64544a59829e3452daea4acd4161addf22fa766c558708ef331f122c797006d78fb2c713e766904615d80df3afeddf5280931daf5df7b2638c9332bdaa0d4c1fc29fe645ba348478991375e1a25bec31b0c66e89914e2864875605f293593e86fb1f6c37e30b774f7ed1e00f197b3de4f23a07c0b3bd9215220881666293a60a4a998c3d0a642db15cb017a9ad9bf42ab0dfb927512059729b5c74c7231b32228fcaa6ee5e6af7e22d8ff4625887fcd74ec39b9537d464e8a6c18f1c1e515a8243b23278a9b591266fc22c39c25ca35a7f0be40498e4b498cc49a9909a5b4efd23bc3281bd7b033a8b2d437afed9a5c701dfad676c7745e70faa94f2d15788e7249875741319a56e3ad501f895b6b198d2bf68ea7004da9aa7c76a24c3bad217fcf5cae19af8cb41807e38ab18437b9ba82cba7d2e24c142b87c17d4ad97ef380fbb1dc3285a7b8a7faaff7a9e69a48a9d345bcbaaaf1455a222a2b1ae9d1eb64f86343af6511046f2937275744388a3e299b47d3741ad6f131f9eb1cb465b86900fb8853d6b9f2754a4626bbe5ebf57777632e5fbaa03820ff9bcd308585b76e538c7d8a7b7726f331922d6721a80bb32309e3d91332d16366eda5c73bba291c2c810b6fc907384955049a1f8b2c2e2670341da281eb980ae7f7177cbcfe551c390cea682f9a063c6ce844b22ab9a6a87fba4b9a60df1c386";
        System.out.println(hexStr2Str(hexStr));
    }
    /**
     * 16进制直接转换成为字符串(无需Unicode解码)
     * @param hexStr  Byte字符串(Byte之间无分隔符
     * @author xxs
     * @return 对应的字符串
     */
    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF"; //16进制能用到的所有字符 0-15
        char[] hexs = hexStr.toCharArray();//toCharArray() 方法将字符串转换为字符数组。
        int length = (hexStr.length() / 2);//1个byte数值 -> 两个16进制字符
        byte[] bytes = new byte[length];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            int position = i * 2;//两个16进制字符 -> 1个byte数值
            n = str.indexOf(hexs[position]) * 16;
            n += str.indexOf(hexs[position + 1]);
            // 保持二进制补码的一致性 因为byte类型字符是8bit的  而int为32bit 会自动补齐高位1  所以与上0xFF之后可以保持高位一致性
            //当byte要转化为int的时候，高的24位必然会补1，这样，其二进制补码其实已经不一致了，&0xff可以将高的24位置为0，低8位保持原样，这样做的目的就是为了保证二进制数据的一致性。
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }
    /**
     * 字符串转换成为16进制(无需Unicode编码)
     * @param str 待转换的ASCII字符串
     * @author xxs
     * @return byte字符串 （每个Byte之间空格分隔）
     */
    public static String str2HexStr(String str) {
        char[] chars = "0123456789ABCDEF".toCharArray();//toCharArray() 方法将字符串转换为字符数组。
        StringBuilder sb = new StringBuilder(""); //StringBuilder是一个类，可以用来处理字符串,sb.append()字符串相加效率高
        byte[] bs = str.getBytes();//String的getBytes()方法是得到一个操作系统默认的编码格式的字节数组
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4; // 高4位, 与操作 1111 0000
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;  // 低四位, 与操作 0000 1111
            sb.append(chars[bit]);
            sb.append(' ');//每个Byte之间空格分隔
        }
        return sb.toString().trim();
    }
}
