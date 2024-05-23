package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.dao.TaskDetailDao;
import test.ticket.tickettools.dao.UserInfoDao;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.bo.ProxyInfo;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.domain.entity.UserInfoEntity;
import test.ticket.tickettools.service.DoSnatchTicketService;
import test.ticket.tickettools.utils.*;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static test.ticket.tickettools.utils.EncDecUtil.doAES;

@Slf4j
@Service
public class ChnMuseumTicketServiceImpl implements DoSnatchTicketService {
    //获取场馆信息
    private static String gainAllSystemConfigLoginUrl = "https://wxmini.chnmuseum.cn/prod-api/basesetting/HallSetting/gainAllSystemConfigLogin?channel=wxMini&requestTaskKey=gainAllSystemConfigLogin&ticketUseType=1&p=wxmini";
    //校验人员url
    private static String checkLeaderInfoUrl = "https://wxmini.chnmuseum.cn/prod-api/config/orderRule/checkLeaderInfo";
    //获取图片点选坐标
    private static String getPointUrl = "http://api.jfbym.com/api/YmServer/customApi";
    //下单url
    private static String placeOrderUrl="https://wxmini.chnmuseum.cn/prod-api/config/orderRule/placeOrder";

    private static Map<Long, Object> runTaskCache = new HashMap<>();

    @Resource
    TaskDao taskDao;
    @Resource
    TaskDetailDao taskDetailDao;
    @Resource
    UserInfoDao userInfoDao;


    @Override
    public void initData(TaskEntity entity) {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.CHNMU.getCode());
        List<TaskEntity> unDoneTasks = taskDao.getUnDoneTasks(taskEntity);
        for (TaskEntity unDoneTask : unDoneTasks) {
            if (!ObjectUtils.isEmpty(unDoneTask.getIp()) && !ObjectUtils.isEmpty(unDoneTask.getPort())) {
                return;
            }
            ProxyInfo proxy = ProxyUtil.getProxy();
            unDoneTask.setIp(proxy.getIp());
            unDoneTask.setPort(proxy.getPort());
            taskDao.updateTask(unDoneTask);
        }
    }

    @Override
    public List<TaskEntity> getAllUndoneTask() {
        return null;
    }

    @Override
    public List<DoSnatchInfo> getDoSnatchInfos() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.CHNMU.getCode());
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
        Long taskId = doSnatchInfo.getTaskId();
        if (runTaskCache.containsKey(taskId)) {
            return;
        } else {
            runTaskCache.put(taskId, true);
        }
        int hallId = 1;
        int hallScheduleId = 1;
        int priceId = 8;
        try {
            Thread.sleep(RandomUtil.randomInt(2000, 6000));
            boolean hasTicket = false;
            String getPriceByScheduleIdUrl = "https://wxmini.chnmuseum.cn/prod-api/pool/ingore/getPriceByScheduleId?hallId=%s&openPerson=1&queryDate=%s&saleMode=1&scheduleId=%s&p=wxmini";
            String getBlockUrl = "https://wxmini.chnmuseum.cn/prod-api/pool/getBlock?nonce=%s&platform=2&docType=1&p=wxmini";
            Date useDate = doSnatchInfo.getUseDate();
            String formatDate = DateUtils.dateToStr(useDate, "yyyy-MM-dd");
            Map<String, String> idNameMap = doSnatchInfo.getIdNameMap();
            Integer session = doSnatchInfo.getSession();
            //获取所有信息
            RestTemplate restTemplate =ObjectUtils.isEmpty(doSnatchInfo.getIp())?TemplateUtil.initSSLTemplate():TemplateUtil.initSSLTemplateWithProxyAuth(doSnatchInfo.getIp(), doSnatchInfo.getPort());
            HttpHeaders headers = new HttpHeaders();
            String headerStr = doSnatchInfo.getHeaders();
            JSONObject headerJson = JSON.parseObject(headerStr);
            for (Map.Entry<String, Object> headerEntry : headerJson.entrySet()) {
                headers.set(headerEntry.getKey(), headerEntry.getValue().toString());
            }
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.remove("Content-Length");
/*
            if(ObjectUtils.isEmpty(doSnatchInfo.getSession())) {
*/
                HttpEntity httpEntity = new HttpEntity(headers);
                //Thread.sleep(RandomUtil.randomInt(4000, 8000));
                JSONObject getAllConfigRes = TemplateUtil.getResponse(restTemplate, gainAllSystemConfigLoginUrl, HttpMethod.GET, httpEntity);
                if (ObjectUtils.isEmpty(getAllConfigRes) || !StrUtil.equals("操作成功", getAllConfigRes.getString("msg"))) {
                    log.info("获取配置信息失败:{}", getAllConfigRes);
                    runTaskCache.remove(taskId);
                    return;
                }
                log.info("获取配置信息:{}", getAllConfigRes);
                JSONObject getAllConfigData = getAllConfigRes.getJSONObject("data");
                JSONArray calendarTicketPoolsByDate = getAllConfigData == null ? null : getAllConfigData.getJSONArray("calendarTicketPoolsByDate");
                if (ObjectUtils.isEmpty(calendarTicketPoolsByDate)) {
                    log.info("获取日期下余票信息失败:{}", getAllConfigRes);
                    runTaskCache.remove(taskId);
                    return;
                }
                for (int i = 0; i < calendarTicketPoolsByDate.size(); i++) {
                    JSONObject calendarTicketInfo = calendarTicketPoolsByDate.getJSONObject(i);
                    if (StrUtil.equals(formatDate, calendarTicketInfo.getString("currentDate"))) {
                        if (calendarTicketInfo.getIntValue("status") == 4 && calendarTicketInfo.getIntValue("ticketPool") >= idNameMap.size()) {
                            JSONArray hallTicketPoolVOS = calendarTicketInfo.getJSONArray("hallTicketPoolVOS");
                            JSONArray scheduleTicketPoolVOS = hallTicketPoolVOS.getJSONObject(0).getJSONArray("scheduleTicketPoolVOS");
                            for (int j = 0; j < scheduleTicketPoolVOS.size(); j++) {
                                JSONObject scheduleTicketJson = scheduleTicketPoolVOS.getJSONObject(j);
                                String scheduleName = scheduleTicketJson.getString("scheduleName");
                                if (ObjectUtils.isEmpty(session)) {
                                    hallId = scheduleTicketJson.getIntValue("hallId");
                                    hallScheduleId = scheduleTicketJson.getIntValue("hallScheduleId");
                                    break;
                                } else {
                                    if (session == 1 && StrUtil.equals("09:00-11:00", scheduleName)) {
                                        hallId = scheduleTicketJson.getIntValue("hallId");
                                        hallScheduleId = scheduleTicketJson.getIntValue("hallScheduleId");
                                    }
                                    if (session == 2 && StrUtil.equals("11:00-13:30", scheduleName)) {
                                        hallId = scheduleTicketJson.getIntValue("hallId");
                                        hallScheduleId = scheduleTicketJson.getIntValue("hallScheduleId");
                                    }
                                    if (session == 3 && StrUtil.equals("13:30-16:00", scheduleName)) {
                                        hallId = scheduleTicketJson.getIntValue("hallId");
                                        hallScheduleId = scheduleTicketJson.getIntValue("hallScheduleId");
                                    }
                                }
                            }
                            hasTicket = true;
                            String formatUrl = String.format(getPriceByScheduleIdUrl, hallId, formatDate, hallScheduleId);
                            JSONObject getPriceByScheduleIdRes = TemplateUtil.getResponse(restTemplate, formatUrl, HttpMethod.GET, httpEntity);
                            if (ObjectUtils.isEmpty(getPriceByScheduleIdRes) || !StrUtil.equals("操作成功", getPriceByScheduleIdRes.getString("msg"))) {
                                log.info("获取场次票价信息失败:{}", getAllConfigRes);
                                runTaskCache.remove(taskId);
                                return;
                            }
                            JSONArray data = getPriceByScheduleIdRes.getJSONArray("data");
                            if (!ObjectUtils.isEmpty(data)) {
                                priceId = data.getJSONObject(0).getIntValue("priceId");
                            }
                            break;
                        }
                    }
                }
            /*}else{
                hallScheduleId=doSnatchInfo.getSession();
                hasTicket=true;
            }*/
            if (hasTicket) {
                JSONObject checkLeaderInfoParam = getCheckLeaderInfoParam(idNameMap, formatDate, hallId, hallScheduleId, priceId);
                headers.setContentLength(JSON.toJSONString(checkLeaderInfoParam).getBytes(StandardCharsets.UTF_8).length);
                HttpEntity checkLeaderInfoEntity = new HttpEntity(checkLeaderInfoParam, headers);
                JSONObject getCheckLeaderInfoRes = TemplateUtil.getResponse(restTemplate, checkLeaderInfoUrl, HttpMethod.POST, checkLeaderInfoEntity);
                if (ObjectUtils.isEmpty(getCheckLeaderInfoRes) || !StrUtil.equals("操作成功", getCheckLeaderInfoRes.getString("msg"))) {
                    log.info("CheckLeaderInfo信息失败:{}", getCheckLeaderInfoRes);
                    runTaskCache.remove(taskId);
                    return;
                }
                log.info("CheckLeaderInfo结果:{}", getCheckLeaderInfoRes);
                headers.remove("Content-Length");
                String checkTime = getCheckTime(headerJson.getString("User-Agent"),doSnatchInfo.getIp(),doSnatchInfo.getPort());
                log.info("getCheckTime:{}", checkTime);
                String ip = checkTime.split("\"")[9];
                String data = doSnatchInfo.getChannelUserId() + ":" + checkTime.substring(26, 36) + "000" + ":" + DateUtils.dateToStr(doSnatchInfo.getUseDate(), "yyyy/MM/dd") + ":" + hallId + ":" + hallScheduleId + ":2";
                String nonce = doAES(data, "AyrKJRXPO3nR5Abc");
                String formatUrl = String.format(getBlockUrl,nonce);
                log.info("formatUrl:{}",formatUrl);
                HttpEntity getBlockEntity=new HttpEntity(headers);
                log.info("header:{}",headers);
                JSONObject getBlockRes = TemplateUtil.getResponse(restTemplate, formatUrl, HttpMethod.GET, getBlockEntity);
                if (ObjectUtils.isEmpty(getBlockRes) || !StrUtil.equals("操作成功", getBlockRes.getString("msg"))) {
                    log.info("获取验证码失败:{}", getBlockRes);
                    runTaskCache.remove(taskId);
                    return;
                }
                JSONObject getBlockData = getBlockRes.getJSONObject("data");
                String originalImageBase64 = getBlockData.getString("originalImageBase64");
                String jigsawImageBase64 = getBlockData.getString("jigsawImageBase64");
                String secretKey = getBlockData.getString("secretKey");
                String token = getBlockData.getString("token");
                HttpHeaders getPointHeaders = new HttpHeaders();
                getPointHeaders.setContentType(MediaType.APPLICATION_JSON);
                getPointHeaders.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
                JSONObject getPointBody = new JSONObject();
                getPointBody.put("image", ImageUtils.mergeBase64Images(originalImageBase64, jigsawImageBase64));
                getPointBody.put("direction", "bottom");
                getPointBody.put("click_num", "");
                getPointBody.put("token", "2J3UHYaDJTbELG55unhlt9JkNLKoLpcY9gsEOvbZ2Uc");
                getPointBody.put("type", "30228");
                HttpEntity getPointEntity = new HttpEntity(getPointBody, getPointHeaders);
                JSONObject getPointRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getPointUrl, HttpMethod.POST, getPointEntity);
                if (ObjectUtils.isEmpty(getPointRes) || getPointRes.getIntValue("code") != 10000) {
                    log.info("获取图片坐标失败：{}", getPointRes);
                    runTaskCache.remove(taskId);
                    return;
                }
                log.info("获取图片坐标：{}", getPointRes);

                JSONObject getPointData = getPointRes.getJSONObject("data");
                String pointStr = getPointData.getString("data");
                String[] split = pointStr.split(",");
                JSONObject coordinate = new JSONObject();
                coordinate.put("x", split[0]);
                coordinate.put("y", split[1]);
                String pointJson = EncDecUtil.doAES(JSON.toJSONString(coordinate), secretKey);
                checkLeaderInfoParam.put("pointJson", pointJson);
                checkLeaderInfoParam.put("captchaToken", token);
                checkLeaderInfoParam.put("scanToken", null);
                headers.setContentLength(JSON.toJSONString(checkLeaderInfoParam).getBytes(StandardCharsets.UTF_8).length);
                headers.set("Host-Ip", EncDecUtil.doAES(doSnatchInfo.getIp(), "AyrKJRXPO3nR5Abc"));
                HttpEntity placeOrderEntity = new HttpEntity(checkLeaderInfoParam, headers);
                JSONObject placeOrderRes = TemplateUtil.getResponse(restTemplate, placeOrderUrl, HttpMethod.POST, placeOrderEntity);
                if (ObjectUtils.isEmpty(placeOrderRes) || !StrUtil.equals("操作成功", getBlockRes.getString("msg"))) {
                    log.info("订票失败:{}", placeOrderRes);
                    runTaskCache.remove(taskId);
                    return;
                }
                log.info("订票结果:{}", placeOrderRes);
                if (placeOrderRes.getIntValue("code") == 200) {
                    JSONObject placeOrderData = placeOrderRes.getJSONObject("data");
                    TaskEntity taskEntity = new TaskEntity();
                    taskEntity.setId(doSnatchInfo.getTaskId());
                    taskEntity.setDone(true);
                    taskEntity.setUpdateDate(new Date());
                    taskDao.updateTask(taskEntity);
                    TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
                    taskDetailEntity.setTaskId(doSnatchInfo.getTaskId());
                    taskDetailEntity.setUpdateDate(new Date());
                    taskDetailEntity.setDone(true);
                    taskDetailEntity.setOrderId(placeOrderData.getLongValue("orderId"));
                    taskDetailEntity.setOrderNumber(placeOrderData.getString("orderNumber"));
                    taskDetailDao.updateEntityByTaskId(taskDetailEntity);
                    SendMessageUtil.send(ChannelEnum.CHNMU.getDesc(), formatDate, placeOrderData.getString("schduleDate"), doSnatchInfo.getAccount(), String.join(",", doSnatchInfo.getIdNameMap().values()));
                }
            }
            runTaskCache.remove(taskId);
        }catch (Exception e){
            log.info("国博抢票任务出错:{}",e);
            runTaskCache.remove(taskId);
        }
    }

    private JSONObject getCheckLeaderInfoParam(Map<String, String> idNameMap, String useDate, Integer hallId, Integer hallScheduleId, Integer ticketPriceId) {
        JSONObject checkLeaderInfoParam = new JSONObject();
        checkLeaderInfoParam.put("useTicketType", 1);
        checkLeaderInfoParam.put("poolFlag", 1);
        checkLeaderInfoParam.put("realNameFlag", 1);
        checkLeaderInfoParam.put("platform", 2);
        checkLeaderInfoParam.put("ticketNum", idNameMap.size());
        checkLeaderInfoParam.put("date", useDate);
        checkLeaderInfoParam.put("childTicketNum", 0);
        checkLeaderInfoParam.put("saleMode", 1);
        JSONArray ticketInfoList = new JSONArray();
        for (Map.Entry<String, String> idNameEntry : idNameMap.entrySet()) {
            JSONObject ticketInfo = new JSONObject();
            ticketInfo.put("status", 0);
            ticketInfo.put("saleMode", 1);
            ticketInfo.put("platform", 2);
            ticketInfo.put("hallId", hallId);
            ticketInfo.put("hallScheduleId", hallScheduleId);
            ticketInfo.put("cinemaFlag", 0);
            ticketInfo.put("ticketPriceId", ticketPriceId);
            ticketInfo.put("certificate", 1);
            ticketInfo.put("certificateInfo", idNameEntry.getKey());
            ticketInfo.put("userName", idNameEntry.getValue());
            ticketInfo.put("userName", idNameEntry.getValue());
            ticketInfo.put("useDate", useDate + " 00:00:00");
            ticketInfo.put("isChildFreeTicket", 0);
            ticketInfo.put("realNameFlag", 1);
            ticketInfoList.add(ticketInfo);
        }
        checkLeaderInfoParam.put("ticketInfoList",ticketInfoList);
        checkLeaderInfoParam.put("p","wxmini");
        return checkLeaderInfoParam;
    }
    public static String getCheckTime(String userAgent,String ip,Integer port){
        HttpHeaders httpHeaders=new HttpHeaders();
        httpHeaders.set("Host","vv.video.qq.com");
        httpHeaders.set("Connection","keep-alive");
        httpHeaders.set("xweb_xhr","1");
        httpHeaders.set("User-Agent",userAgent);
        httpHeaders.set("Content-Type","application/json");
        httpHeaders.set("Accept-Type","*/*");
        httpHeaders.set("Sec-Fetch-Site","cross-site");
        httpHeaders.set("Sec-Fetch-Mode","cors");
        httpHeaders.set("Sec-Fetch-Dest","empty");
        httpHeaders.set("Referer","https://servicewechat.com/wx9e2927dd595b0473/73/page-frame.html");
        httpHeaders.set("Accept-Encoding","gzip, deflate, br");
        httpHeaders.set("Accept-Language","zh-CN,zh;q=0.9");
        RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
        HttpEntity httpEntity=new HttpEntity(httpHeaders);
        ResponseEntity getCheckTimeRes = restTemplate.exchange("http://vv.video.qq.com/checktime?otype=json", HttpMethod.GET, httpEntity, String.class);
        return getCheckTimeRes.getBody().toString();
    }
}
