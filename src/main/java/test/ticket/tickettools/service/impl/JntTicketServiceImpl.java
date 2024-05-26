package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.dao.TaskDetailDao;
import test.ticket.tickettools.dao.AccountInfoDao;
import test.ticket.tickettools.domain.bo.DoSnatchInfo;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.service.DoSnatchTicketService;
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
import java.util.*;


@Slf4j
@Service
public class JntTicketServiceImpl implements DoSnatchTicketService {

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

    private static Map<Long,Object> runTaskCache=new HashMap<>();
    private static Map<Long,Object> initTaskCache=new HashMap<>();



    @Resource
    TaskDao taskDao;

    @Resource
    TaskDetailDao taskDetailDao;

    @Resource
    AccountInfoDao accountInfoDao;

    @Override
    public void doSnatchingTicket(DoSnatchInfo doSnatchInfo) {
        Long taskId = doSnatchInfo.getTaskId();
        if(runTaskCache.containsKey(taskId)){
            return;
        }else{
            runTaskCache.put(taskId,true);
        }
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
        RestTemplate restTemplate=ObjectUtils.isEmpty(doSnatchInfo.getIp())?TemplateUtil.initSSLTemplate():TemplateUtil.initSSLTemplateWithProxy(doSnatchInfo.getIp(), doSnatchInfo.getPort());
        String queryFormat = MessageFormat.format("fromtype={0}&siteid={1}", "GROUP", "7e97d18d179c4791bab189f8de87ee9d");
        headers.set("Content-Length", String.valueOf(customURLEncode(queryFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
        HttpEntity queryEntity = new HttpEntity<>(queryFormat, headers);
        JSONObject queryRes = TemplateUtil.getResponse(restTemplate, bookingQueryUrl, HttpMethod.POST, queryEntity);
        if (ObjectUtils.isEmpty(queryRes)) {
            log.info("账号{}查询余票数据失败:{}",doSnatchInfo.getAccount(),queryRes);
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
        //查询余票信息
        log.info("场馆信息:{}",sessionMap);
        String referer = "https://jnt.mfu.com.cn/page/tg/editorder/%s?date=%s&begintime=%s&endtime=%s&booking_including_self=0&maxnums=60&minnums=10";
        headers.set("M-Lang","zh");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        TaskEntity taskEntity=new TaskEntity();
        taskEntity.setId(doSnatchInfo.getTaskId());
        taskEntity.setDone(true);
        for (Map.Entry<String, JSONObject> sessionEntity : sessionMap.entrySet()) {
            try {
                Thread.sleep(RandomUtil.randomInt(2000,4000));
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
                        TaskEntity updateTaskEntity=new TaskEntity();
                        updateTaskEntity.setId(doSnatchInfo.getTaskId());
                        updateTaskEntity.setUserInfoId(doSnatchInfo.getUserInfoId());
                        runTaskCache.remove(doSnatchInfo.getTaskId());
                        return;
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
                    TaskEntity updateTaskEntity=new TaskEntity();
                    updateTaskEntity.setId(doSnatchInfo.getTaskId());
                    updateTaskEntity.setUserInfoId(doSnatchInfo.getUserInfoId());
                    break;
                }
                //提交订单
                String submitBodyFormat = MessageFormat.format("usertype=tg&eventssessionid={0}&bookingdata={1}", key, customURLEncode(buildParam(doSnatchInfo.getIdNameMap()).toJSONString(), "utf-8"));
                headers.set("Content-Length", String.valueOf(submitBodyFormat.getBytes(StandardCharsets.UTF_8).length));
                headers.set("Accept-Language","zh-CN,zh-Hans;q=0.9");
                HttpEntity submitEntity = new HttpEntity<>(submitBodyFormat, headers);
                Thread.sleep(RandomUtil.randomInt(1000,3000));
                JSONObject submitRes = TemplateUtil.getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                log.info("账号{}提单请求结果:{}", doSnatchInfo.getAccount(),submitRes);
                if (StrUtil.equals(submitRes.getString("code"), "A00006")) {
                    taskDao.updateTask(taskEntity);
                    taskDetailDao.updateByTaskId(doSnatchInfo.getTaskId());
                    SendMessageUtil.send(ChannelEnum.MFU.getDesc(),formatDate,value.getString("summary"),doSnatchInfo.getAccount(),String.join(",",doSnatchInfo.getIdNameMap().values()));
                    break;
                }
                if (StrUtil.equals(submitRes.getString("code"), "A00013")) {
                    check(submitRes.getString("captcha_type"), restTemplate, headers);
                    submitRes = TemplateUtil.getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                    log.info("账号{}提单请求结果:{}", doSnatchInfo.getAccount(),submitRes);
                    if (StrUtil.equals(submitRes.getString("code"), "A00006")) {
                        taskDao.updateTask(taskEntity);
                        taskDetailDao.updateByTaskId(doSnatchInfo.getTaskId());
                        SendMessageUtil.send(ChannelEnum.MFU.getDesc(),formatDate,value.getString("summary"),doSnatchInfo.getAccount(),String.join(",",doSnatchInfo.getIdNameMap().values()));
                        break;
                    }
                    if(StrUtil.equals("A00004", submitRes.getString("code"))){
                        String cookie = getCookie(doSnatchInfo.getAccount(), doSnatchInfo.getPwd(), doSnatchInfo.getIp(), doSnatchInfo.getPort());
                        if(ObjectUtils.isEmpty(cookie)){
                            runTaskCache.remove(doSnatchInfo.getTaskId());
                            return;
                        }
                        headers.set("cookie",cookie);
                        submitEntity = new HttpEntity<>(submitBodyFormat, headers);
                        submitRes = TemplateUtil.getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                        log.info("账号{}提单请求结果:{}", doSnatchInfo.getAccount(),submitRes);
                        if (StrUtil.equals(submitRes.getString("code"), "A00006")) {
                            taskDao.updateTask(taskEntity);
                            taskDetailDao.updateByTaskId(doSnatchInfo.getTaskId());
                            SendMessageUtil.send(ChannelEnum.MFU.getDesc(),formatDate,value.getString("summary"),doSnatchInfo.getAccount(),String.join(",",doSnatchInfo.getIdNameMap().values()));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                runTaskCache.remove(doSnatchInfo.getTaskId());
                log.info("账号{}doSnatchingJnt出错了:{}",doSnatchInfo.getAccount(),e);
            }
        }
        runTaskCache.remove(doSnatchInfo.getTaskId());
    }

    @Override
    public void initData(TaskEntity unDoneTask) {
        Long taskId = unDoneTask.getId();
        if(initTaskCache.containsKey(taskId)){
            return;
        }else{
            initTaskCache.put(taskId,true);
        }
        try {
            if (!ObjectUtils.isEmpty(unDoneTask.getUpdateDate())&& !ObjectUtils.isEmpty(unDoneTask.getAuth())) {
                initTaskCache.remove(taskId);
                return;
            }
            AccountInfoEntity accountInfoEntity;
            if (ObjectUtils.isEmpty(unDoneTask.getUserInfoId())) {
                AccountInfoEntity userInfo = new AccountInfoEntity();
                userInfo.setChannel(ChannelEnum.MFU.getCode());
                userInfo.setStatus(false);
                List<AccountInfoEntity> select = accountInfoDao.select(userInfo);
                accountInfoEntity = select.get((int) (select.size() * Math.random()));
            } else {
                accountInfoEntity = accountInfoDao.selectById(unDoneTask.getUserInfoId());
            }
            unDoneTask.setUserInfoId(accountInfoEntity.getId());
            unDoneTask.setAccount(accountInfoEntity.getAccount());
            unDoneTask.setPwd(accountInfoEntity.getPwd());
            //获取cookie
            String cookie = getCookie(accountInfoEntity.getAccount(), accountInfoEntity.getPwd(), unDoneTask.getIp(), unDoneTask.getPort());
            if (ObjectUtils.isEmpty(cookie)) {
                initTaskCache.remove(taskId);
                return;
            }
            HttpHeaders headers=getHeaders();
            headers.set("cookie",cookie);
            HttpEntity logininfoEntity = new HttpEntity<>(headers);
            JSONObject response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplateWithProxyAuth(unDoneTask.getIp(), unDoneTask.getPort()), "https://jnt.mfu.com.cn/ajax?ugi=tg/account&action=logininfo&bundleid=com.maiget.tickets&moduleid=6f77be86038c47269f1e00f7ddee9af4", HttpMethod.POST, logininfoEntity);
            if(!ObjectUtils.isEmpty(response)&&StrUtil.equals(response.getString("code"),"A00006")){
                log.info("账号:{}获取cookie成功,cookie:{}",unDoneTask.getAccount(),cookie);
                unDoneTask.setAuth(cookie);
                unDoneTask.setUpdateDate(new Date());
                taskDao.updateTask(unDoneTask);
                JSONObject tourGuide = response.getJSONObject("tourGuide");
                String realName = tourGuide.getString("realname");
                accountInfoEntity.setUserName(realName);
                accountInfoDao.insertOrUpdate(accountInfoEntity);
            }else{
                log.info("账号:{}获取cookie失败,请求个人信息报错:{}",unDoneTask.getAccount(),response);
            }
        }catch (Exception e){
            initTaskCache.remove(taskId);
            log.info("毛纪初始化数据异常:{}",e);
        }
        initTaskCache.remove(taskId);
    }

    @Override
    public List<TaskEntity> getAllUndoneTask() {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setChannel(ChannelEnum.MFU.getCode());
        List<TaskEntity> unDoneTasks = taskDao.getUnDoneTasks(taskEntity);
        return unDoneTasks;
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
        try {
            RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
            if (!ObjectUtils.isEmpty(ip) && !ObjectUtils.isEmpty(port)) {
                restTemplate = TemplateUtil.initSSLTemplateWithProxyAuth(ip, port);
            }
            HttpHeaders headers=getHeaders();
            HttpEntity getCsrfEntity = new HttpEntity<>(headers);
            Thread.sleep(1500);
            ResponseEntity<String> getCsrf = restTemplate.exchange(getCsrfUrl, HttpMethod.GET, getCsrfEntity, String.class);
            HttpHeaders loginHeaders = getCsrf.getHeaders();
            String body = getCsrf.getBody();
            if (ObjectUtils.isEmpty(body)) {
                log.info("账号：{},获取CSRF失败", userName, body);
                return null;
            }
            JSONObject getCsrfJson=JSON.parseObject(body);
            if(!StrUtil.equals("A00006",getCsrfJson.getString("code"))){
                return null;
            }
            String csrf_req = getCsrfJson.getString("csrf_req");
            String csrf_ts = getCsrfJson.getString("csrf_ts");
            String csrf = DigestUtils.md5Hex(csrf_req + csrf_ts);
            String bodyFormat = MessageFormat.format("loginid={0}&passwd={1}&csrf_req={2}&csrf_ts={3}&csrf={4}", userName, pwd, csrf_req, csrf_ts, csrf);
            headers.set("Content-Length", String.valueOf(customURLEncode(bodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity loginEntity = new HttpEntity<>(bodyFormat, headers);
            JSONObject doLoginRes = TemplateUtil.getResponse(restTemplate, loginUrl, HttpMethod.POST, loginEntity);
            if(StrUtil.equals("A00006",doLoginRes.getString("code"))){
                List<String> cookies = loginHeaders.get("set-cookie");
                return String.join(";",cookies);
            }
        }catch (Exception e){
            e.printStackTrace();
            log.info("获取cookie异常:{}",e);
        }
        return null;
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
    private HttpHeaders getHeaders(){
        HttpHeaders headers = new HttpHeaders();
        //获取Csrf
        headers.set("Host", "jnt.mfu.com.cn");
        headers.set("M-Lang", "zh");
        headers.set("Accept-Encoding", "gzip, deflate, br");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Origin", "https://jnt.mfu.com.cn");
        headers.set("Referer", "https://jnt.mfu.com.cn/page/tg/login");
        headers.set("Sec-Fetch-Mode", "cors");
        headers.set("Sec-Fetch-Site", "same-origin");
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        headers.set("Sec-Ch-Ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        headers.set("Sec-Ch-Ua-Platform", "macOS");
        headers.set("Sec-Fetch-Dest", "empty");
        return headers;
    }
}
