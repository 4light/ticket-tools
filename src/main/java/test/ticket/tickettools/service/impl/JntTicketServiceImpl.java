package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javafx.beans.binding.ObjectExpression;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.*;
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
import test.ticket.tickettools.service.JntTicketService;
import test.ticket.tickettools.utils.*;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class JntTicketServiceImpl implements JntTicketService {

    //获取csrf
    private static String getCsrfUrl = "https://jnt.mfu.com.cn/ajax?ugi=tg/account&action=forminit&bundleid=com.maiget.tickets&moduleid=6f77be86038c47269f1e00f7ddee9af4";
    //登录Url
    private static String loginUrl = "https://jnt.mfu.com.cn/ajax?ugi=tg/account&action=login&bundleid=com.maiget.tickets&moduleid=6f77be86038c47269f1e00f7ddee9af4";
    //获取场次url
    private static String bookingQueryUrl = "https://jnt.mfu.com.cn/ajax?ugi=bookingquery&action=getSessions&bundleid=com.maiget.tickets&moduleid=6f77be86038c47269f1e00f7ddee9af4";
    //获取文字点选坐标
    private static String getPointUrl = "http://api.jfbym.com/api/YmServer/customApi";
    //获取验证码接口
    private static String getCaptchaUrl = "https://jnt.mfu.com.cn/mmodule/mpwork.captcha/ajax/captcha/get";
    //提交订单
    private static String submitUrl = "https://jnt.mfu.com.cn/ajax?ugi=bookingorder&action=createGroupTicketOrder&bundleid=com.maiget.tickets&moduleid=6f77be86038c47269f1e00f7ddee9af4";
    //验证码校验
    private static String checkUrl = "https://jnt.mfu.com.cn/mmodule/mpwork.captcha/ajax/captcha/check";
    //校验姓名url
    private static String checkNameUrl = "https://jnt.mfu.com.cn/ajax?ugi=account&action=checkSensitiveWord&bundleid=com.maiget.tickets&moduleid=6f77be86038c47269f1e00f7ddee9af4";
    //校验身份证号
    private static String checkIdUrl = "https://jnt.mfu.com.cn/ajax?ugi=bookingorder&action=checkBookingUserV2&bundleid=com.maiget.tickets&moduleid=6f77be86038c47269f1e00f7ddee9af4";





    @Resource
    TaskDao taskDao;

    @Resource
    TaskDetailDao taskDetailDao;

    @Resource
    UserInfoDao userInfoDao;

    @Override
    public void doSnatchingJnt(DoSnatchInfo doSnatchInfo) {
        String authorization = doSnatchInfo.getAuthorization();
        if(ObjectUtils.isEmpty(authorization)){
            authorization= getCookie(doSnatchInfo.getAccount(), doSnatchInfo.getPwd(), doSnatchInfo.getIp(), doSnatchInfo.getPort());
        }
        Map<String, JSONObject> sessionMap = new HashMap();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept-Encoding", "gzip, deflate, br");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Origin", "https://jnt.mfu.com.cn");
        headers.set("Referer", "https://jnt.mfu.com.cn/page/tg/login");
        headers.set("Sec-Fetch-Mode", "cors");
        headers.set("Sec-Fetch-Site", "same-origin");
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
        headers.set("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        headers.set("sec-ch-ua-platform", "macOS");
        headers.set("cookie",authorization);
        //查询余票
        headers.set("Referer", "https://jnt.mfu.com.cn/page/tg");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        RestTemplate restTemplate=TemplateUtil.initSSLTemplateWithProxy(doSnatchInfo.getIp(), doSnatchInfo.getPort());
        String queryFormat = MessageFormat.format("fromtype={0}&siteid={1}", "GROUP", "7e97d18d179c4791bab189f8de87ee9d");
        headers.set("Content-Length", String.valueOf(customURLEncode(queryFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
        HttpEntity queryEntity = new HttpEntity<>(queryFormat, headers);
        JSONObject queryRes = TemplateUtil.getResponse(restTemplate, bookingQueryUrl, HttpMethod.POST, queryEntity);
        if (ObjectUtils.isEmpty(queryRes)) {
            log.info("查询余票数据失败:{}",queryRes);
            return;
        }
        if (StrUtil.equals(queryRes.getString("code"), "A00013")) {
            for (int i=0;i<5;i++){
                check(queryRes.getString("captcha_type"), restTemplate, headers);
                queryRes = TemplateUtil.getResponse(restTemplate, bookingQueryUrl, HttpMethod.POST, queryEntity);
                if (StrUtil.equals(queryRes.getString("code"), "A00006")) {
                    break;
                }
            }
        }
        Date useDate = doSnatchInfo.getUseDate();
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        String formatDate=simpleDateFormat.format(useDate);
        List<String> sessionList = new ArrayList();
        if (StrUtil.equals(queryRes.getString("code"), "A00006")) {
            JSONObject useDateTickInfo = queryRes.getJSONObject(formatDate);
            JSONArray sessions = useDateTickInfo.getJSONArray("sessions");
            for (int i = 0; i < sessions.size(); i++) {
                JSONObject session = sessions.getJSONObject(i);
                String eventsSessionId = session.getString("eventssessionid");
                if(session!=null&&session.getIntValue("remaining_check")==1) {
                    sessionList.add(0, eventsSessionId);
                    sessionMap.put(eventsSessionId, sessions.getJSONObject(i));
                }
            }
        }
        log.info("场馆信息:{}",sessionMap);
        String referer = "https://jnt.mfu.com.cn/page/tg/editorder/%s?date=%s&begintime=%s&endtime=%s&booking_including_self=0&maxnums=60&minnums=10";
        headers.set("M-Lang","zh");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        TaskEntity taskEntity=new TaskEntity();
        taskEntity.setId(doSnatchInfo.getTaskId());
        taskEntity.setDone(true);
        for (Map.Entry<String, JSONObject> sessionEntity : sessionMap.entrySet()) {
            try {
                Thread.sleep(RandomUtil.randomInt(2000,3000));
                log.info("当前session:{}", sessionEntity.getKey());
                String key = sessionEntity.getKey();
                JSONObject value = sessionEntity.getValue();
                String format = String.format(referer, key, formatDate, value.getString("begintime"), value.getString("endtime"));
                headers.set("Referer", format);
                //校验姓名和身份证号
                Map<String, String> currentIdNameMap = doSnatchInfo.getIdNameMap();
                for (String name : currentIdNameMap.values()) {
                    String reqParam = "str=" + name;
                    headers.set("Content-Length", String.valueOf(customURLEncode(reqParam, "utf-8").getBytes(StandardCharsets.UTF_8).length));
                    HttpEntity checkNameEntity = new HttpEntity<>(reqParam, headers);
                    JSONObject response = TemplateUtil.getResponse(restTemplate, checkNameUrl, HttpMethod.POST, checkNameEntity);
                    String code = response.getString("code");
                    if (!StrUtil.equals("A00006", code)&&!StrUtil.equals("A00004", code)) {
                        log.info("姓名-{}校验不通过:{}", name, response);
                        return;
                    }
                    if(StrUtil.equals("A00004", code)){
                        String cookie = getCookie(doSnatchInfo.getAccount(), doSnatchInfo.getPwd(), doSnatchInfo.getIp(), doSnatchInfo.getPort());
                        headers.set("cookie",cookie);
                        break;
                    }
                }
                String idNums = String.join(",", currentIdNameMap.keySet());
                String idParamForm = MessageFormat.format("eventssessionid={0}&usertype=tg&idnums={1}", key, idNums);
                headers.set("Content-Length", String.valueOf(customURLEncode(idParamForm, "utf-8").getBytes(StandardCharsets.UTF_8).length));
                HttpEntity checkIdEntity = new HttpEntity<>(idParamForm, headers);
                JSONObject checkIdRes = TemplateUtil.getResponse(restTemplate, checkIdUrl, HttpMethod.POST, checkIdEntity);
                String checkIdResCode = checkIdRes.getString("code");
                if (!StrUtil.equals("A00006", checkIdResCode)&&!StrUtil.equals("A00004", checkIdResCode)) {
                    log.info("身份证校验不通过:{}", checkIdRes);
                    break;
                }
                if(StrUtil.equals("A00004", checkIdResCode)){
                    String cookie = getCookie(doSnatchInfo.getAccount(), doSnatchInfo.getPwd(), doSnatchInfo.getIp(), doSnatchInfo.getPort());
                    headers.set("cookie",cookie);
                    continue;
                }
                //提交订单
                String submitBodyFormat = MessageFormat.format("usertype=tg&eventssessionid={0}&bookingdata={1}", key, customURLEncode(buildParam(doSnatchInfo.getIdNameMap()).toJSONString(), "utf-8"));
                headers.set("Content-Length", String.valueOf(submitBodyFormat.getBytes(StandardCharsets.UTF_8).length));
                headers.set("Accept-Language","zh-CN,zh-Hans;q=0.9");
                HttpEntity submitEntity = new HttpEntity<>(submitBodyFormat, headers);
                JSONObject submitRes = TemplateUtil.getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                log.info("请求结果:{}", submitRes);
                if (StrUtil.equals(submitRes.getString("code"), "A00006")) {
                    taskDao.updateTask(taskEntity);
                    taskDetailDao.updateByTaskId(doSnatchInfo.getTaskId());
                    SendMessageUtil.send(ChannelEnum.MFU.getDesc(),formatDate,value.getString("summary"),doSnatchInfo.getAccount(),String.join(",",doSnatchInfo.getIdNameMap().values()));
                    break;
                }
                if (StrUtil.equals(submitRes.getString("code"), "A00013")) {
                    check(submitRes.getString("captcha_type"), restTemplate, headers);
                    submitRes = TemplateUtil.getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                    log.info("请求结果:{}", submitRes);
                    if (StrUtil.equals(submitRes.getString("code"), "A00006")) {
                        taskDao.updateTask(taskEntity);
                        taskDetailDao.updateByTaskId(doSnatchInfo.getTaskId());
                        SendMessageUtil.send(ChannelEnum.MFU.getDesc(),formatDate,value.getString("summary"),doSnatchInfo.getAccount(),String.join(",",doSnatchInfo.getIdNameMap().values()));
                        break;
                    }
                    if(StrUtil.equals("A00004", submitRes.getString("code"))){
                        String cookie = getCookie(doSnatchInfo.getAccount(), doSnatchInfo.getPwd(), doSnatchInfo.getIp(), doSnatchInfo.getPort());
                        headers.set("cookie",cookie);
                        submitEntity = new HttpEntity<>(submitBodyFormat, headers);
                        submitRes = TemplateUtil.getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                        log.info("请求结果:{}", submitRes);
                        if (StrUtil.equals(submitRes.getString("code"), "A00006")) {
                            taskDao.updateTask(taskEntity);
                            taskDetailDao.updateByTaskId(doSnatchInfo.getTaskId());
                            SendMessageUtil.send(ChannelEnum.MFU.getDesc(),formatDate,value.getString("summary"),doSnatchInfo.getAccount(),String.join(",",doSnatchInfo.getIdNameMap().values()));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void initData() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.MFU.getCode());
        List<TaskEntity> unDoneTasks = taskDao.getUnDoneTasks(taskEntity);
        if(ObjectUtils.isEmpty(unDoneTasks)){
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(unDoneTasks.size());
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        for (TaskEntity unDoneTask : unDoneTasks) {
            executor.execute(() -> {
                if(unDoneTask.getUpdateDate()!=null&&unDoneTask.getIp()!=null&&unDoneTask.getPort()!=null){
                    return;
                }
                JSONObject proxy = ProxyUtil.getProxy();
                unDoneTask.setIp(proxy.getString("ip"));
                unDoneTask.setPort(proxy.getInteger("port"));
                UserInfoEntity userInfoEntity = new UserInfoEntity();
                if (ObjectUtils.isEmpty(unDoneTask.getUserInfoId())) {
                    UserInfoEntity userInfo = new UserInfoEntity();
                    userInfo.setChannel(ChannelEnum.MFU.getCode());
                    userInfo.setStatus(false);
                    List<UserInfoEntity> select = userInfoDao.select(userInfo);
                    userInfoEntity = select.get((int) (select.size() * Math.random()));
                } else {
                    userInfoEntity = userInfoDao.selectById(unDoneTask.getUserInfoId());
                }
                unDoneTask.setUserInfoId(userInfoEntity.getId());
                unDoneTask.setAccount(userInfoEntity.getAccount());
                unDoneTask.setPwd(userInfoEntity.getPwd());
                //获取cookie
                String cookie = getCookie(userInfoEntity.getAccount(), userInfoEntity.getPwd(), proxy.getString("ip"), proxy.getInteger("port"));
                if(ObjectUtils.isEmpty(cookie)){
                    return;
                }
                unDoneTask.setAuth(cookie);
                unDoneTask.setUpdateDate(new Date());
                taskDao.updateTask(unDoneTask);
            });
            // 提交完所有任务后，关闭线程池
            executor.shutdown();
            // 等待所有任务执行完毕
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public List<DoSnatchInfo> getDoSnatchInfos() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.MFU.getCode());
        List<TaskEntity> unDoneTasks = taskDao.getUnDoneTasks(taskEntity);
        List<DoSnatchInfo> doSnatchInfoList = new ArrayList<>();
        for (TaskEntity unDoneTask : unDoneTasks) {
            DoSnatchInfo doSnatchInfo = new DoSnatchInfo();
            doSnatchInfo.setTaskId(unDoneTask.getId());
            doSnatchInfo.setUserInfoId(unDoneTask.getUserInfoId());
            doSnatchInfo.setAccount(unDoneTask.getAccount());
            doSnatchInfo.setPwd(unDoneTask.getPwd());
            doSnatchInfo.setAuthorization(unDoneTask.getAuth());
            doSnatchInfo.setIp(unDoneTask.getIp());
            doSnatchInfo.setPort(unDoneTask.getPort());
            doSnatchInfo.setUseDate(unDoneTask.getUseDate());
            TaskDetailEntity taskDetailEntity = new TaskDetailEntity();
            taskDetailEntity.setTaskId(unDoneTask.getId());
            taskDetailEntity.setDone(false);
            List<TaskDetailEntity> taskDetailEntities = taskDetailDao.selectByEntity(taskDetailEntity);
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

    private String getCookie(String userName, String pwd, String ip,Integer port) {
        RestTemplate restTemplate = TemplateUtil.initSSLTemplateWithProxy(ip,port);
        if (!ObjectUtils.isEmpty(ip)&&!ObjectUtils.isEmpty(port)) {
            restTemplate = TemplateUtil.initSSLTemplateWithProxy(ip, port);
        }
        HttpHeaders headers = new HttpHeaders();
        //获取Csrf
        headers.set("Accept-Encoding", "gzip, deflate, br");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Origin", "https://jnt.mfu.com.cn");
        headers.set("Referer", "https://jnt.mfu.com.cn/page/tg/login");
        headers.set("Sec-Fetch-Mode", "cors");
        headers.set("Sec-Fetch-Site", "same-origin");
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
        headers.set("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        headers.set("sec-ch-ua-platform", "macOS");
        HttpEntity getCsrfEntity = new HttpEntity<>(headers);
        JSONObject getCsrfJson = TemplateUtil.getResponse(restTemplate, getCsrfUrl, HttpMethod.GET, getCsrfEntity);
        if (ObjectUtils.isEmpty(getCsrfJson)) {
            log.info("重试获取CSRF");
            return null;
        }
        String csrf_req = getCsrfJson.getString("csrf_req");
        String csrf_ts = getCsrfJson.getString("csrf_ts");
        String csrf = DigestUtils.md5Hex(csrf_req + csrf_ts);
        String bodyFormat = MessageFormat.format("loginid={0}&passwd={1}&csrf_req={2}&csrf_ts={3}&csrf={4}", userName, pwd, csrf_req, csrf_ts, csrf);
        headers.set("Content-Length", String.valueOf(customURLEncode(bodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity loginEntity = new HttpEntity<>(bodyFormat, headers);
        ResponseEntity<String> doLogin = restTemplate.exchange(loginUrl, HttpMethod.POST, loginEntity, String.class);
        HttpHeaders loginHeaders = doLogin.getHeaders();
        List<String> cookies = loginHeaders.get("set-cookie");
        List<String> date = loginHeaders.get("Date");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        LocalDateTime dateTime = LocalDateTime.parse(date.get(0), formatter);
        LocalDateTime localDateTime = dateTime.plusHours(8);
        log.info("获取到cookie:{}", cookies.get(0));
        if(cookies.get(0).contains("UTOKEN")){
            return getCookie(userName,pwd,ip,port);
        }
        return String.join(";",cookies);
    }

    private void check(String captchaType, RestTemplate restTemplate, HttpHeaders headers) {
        //如果需要验证，请求下验证码
        //获取验证码图片
        headers.remove("Referer");
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject param = new JSONObject();
        param.put("captchaType", captchaType);
        param.put("clientUid", getQueryCaptchaPoint());
        param.put("ts", System.currentTimeMillis());
        HttpEntity getCaptchaEntity = new HttpEntity<>(param, headers);
        JSONObject getCaptchaRes = TemplateUtil.getResponse(restTemplate, getCaptchaUrl, HttpMethod.POST, getCaptchaEntity);
        if (ObjectUtils.isEmpty(getCaptchaRes)) {
            log.info("请求验证码错误重试中");
            check(captchaType, restTemplate, headers);
        }
        //如果是文字点选需要调第三方
        if (StrUtil.equals(getCaptchaRes.getString("code"), "A00006")) {
            String originalImageBase64 = getCaptchaRes.getJSONObject("repData").getString("originalImageBase64");
            String secretKey = getCaptchaRes.getJSONObject("repData").getString("secretKey");
            String token = getCaptchaRes.getJSONObject("repData").getString("token");
            String pointJson = null;
            if (StrUtil.equals("clickWord", captchaType)) {
                HttpHeaders getPointHeaders = new HttpHeaders();
                getPointHeaders.setContentType(MediaType.APPLICATION_JSON);
                getPointHeaders.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
                JSONObject getPointBody = new JSONObject();
                getPointBody.put("image", originalImageBase64);
                List wordList = getCaptchaRes.getJSONObject("repData").getJSONArray("wordList").toJavaList(String.class);
                getPointBody.put("extra", String.join(",", wordList));
                getPointBody.put("token", "2J3UHYaDJTbELG55unhlt9JkNLKoLpcY9gsEOvbZ2Uc");
                getPointBody.put("type", "30100");
                HttpEntity entity = new HttpEntity<>(getPointBody, getPointHeaders);
                JSONObject getPointRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getPointUrl, HttpMethod.POST, entity);
                if (ObjectUtils.isEmpty(getPointRes) || getPointRes.getIntValue("code") != 10000) {
                    log.info("获取图片坐标失败：{}", getPointRes);
                    return;
                }
                JSONObject data = getPointRes.getJSONObject("data");
                String pointArrStr = data.getString("data");
                String[] substring = pointArrStr.split("\\|");
                List<JSONObject> posList = new ArrayList<>();
                for (String s : substring) {
                    String[] split = s.split(",");
                    JSONObject item = new JSONObject();
                    item.put("x", Math.round((Double.valueOf(split[0]) * 310) / 330));
                    item.put("y", Math.round((Double.valueOf(split[1]) * 155) / 155));
                    posList.add(item);
                }
                log.info("文字点选验证码坐标:{}", posList);
                pointJson = EncDecUtil.doAES(JSON.toJSONString(posList), secretKey);
            } else {
                //请求滑块验证码
                String uuid = UUID.randomUUID().toString();
                String backImageName = "."+ File.separator + uuid + "_back.png";
                String sliderImageName = "."+File.separator + uuid + "_slider.png";
                ImageUtils.imagCreate(originalImageBase64, backImageName, 155, 310);
                String jigsawImageBase64 = getCaptchaRes.getJSONObject("repData").getString("jigsawImageBase64");
                ImageUtils.imagCreate(jigsawImageBase64, sliderImageName, 155, 50);
                Double point = ImageUtils.getPoint(backImageName, sliderImageName, uuid);
                JSONObject coordinate = new JSONObject();
                coordinate.put("x", point);
                coordinate.put("y", 5);
                log.info("滑块验证码坐标:{}", coordinate);
                pointJson = EncDecUtil.doAES(JSON.toJSONString(coordinate), secretKey);
                try {
                    Files.delete(Paths.get(backImageName));
                    Files.delete(Paths.get(sliderImageName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            headers.set("Referer", "https://jnt.mfu.com.cn/page/tg");
            headers.setContentType(MediaType.APPLICATION_JSON);
            JSONObject checkBody = new JSONObject();
            checkBody.put("captchaType", captchaType);
            checkBody.put("pointJson", pointJson);
            checkBody.put("token", token);
            HttpEntity checkEntity = new HttpEntity<>(checkBody, headers);
            JSONObject checkRes = TemplateUtil.getResponse(restTemplate, checkUrl, HttpMethod.POST, checkEntity);
            log.info("验证码校验结果:{}", checkRes);
            if (ObjectUtils.isEmpty(checkRes) || !StrUtil.equals("A00006", checkRes.getString("code"))) {
                check(captchaType, restTemplate, headers);
            }
        } else {
            log.info("请求文字点击验证码失败");
            return;
        }
    }

    private static String customURLEncode(String s, String enc) {
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

    private static String getQueryCaptchaPoint() {
        /**
         * js部分
         * for (var s = [], t = '0123456789abcdef', i = 0; i < 36; i++)
         *                                 s[i] = t.substr(Math.floor(16 * Math.random()), 1)
         *                                 ;(s[14] = '4'),
         *                                 (s[19] = t.substr((3 & s[19]) | 8, 1)),
         *                                 (s[8] = s[13] = s[18] = s[23] = '-')
         */
        char[] s = new char[36];
        String sourceStr = "0123456789abcdef";
        for (int i = 0; i < 36; i++) {
            s[i] = sourceStr.charAt((int) Math.floor(16 * Math.random()));
        }
        s[14] = '4';
        if (Character.isDigit(s[19])) {
            int numericValue = Character.getNumericValue(s[19]);
            s[19] = sourceStr.charAt((3 & numericValue) | 8);
        } else {
            s[19] = sourceStr.charAt(8);
        }
        s[8] = s[13] = s[18] = s[23] = '-';
        String charStr = new String(s);
        return "point-" + charStr;
    }

    public static String getId() {
        String r = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        char[] item = new char[32];
        for (int i = 0; i < 32; i++) {
            int val = (int) (Math.random() * 62);
            item[i] = r.charAt(0 | val);
        }
        item[0] = 'u';
        return String.valueOf(item);
    }

    private static JSONArray buildParam(Map<String, String> idNameM) {
        JSONArray param = new JSONArray();
        for (Map.Entry<String, String> idNameEntry : idNameM.entrySet()) {
            JSONObject item = new JSONObject();
            item.put("invalid", false);
            item.put("errmsg_realname", "");
            item.put("errmsg_doctype", "");
            item.put("errmsg_idnum", "");
            item.put("jkbColor", -2);
            item.put("id", getId());
            item.put("realname", idNameEntry.getValue());
            item.put("doctype", "IDCARD");
            item.put("idnum", idNameEntry.getKey());
            param.add(item);
        }
        return param;
    }
}
