package test.ticket.tickettools.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.dao.AccountInfoDao;
import test.ticket.tickettools.domain.bo.LogInCSTMParam;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.utils.DateUtils;
import test.ticket.tickettools.utils.TemplateUtil;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LoginService {
    private static String getCaptchaImageUrl = "https://pcticket.cstm.org.cn/prod-api/ingore/captchaImage?verifyType=1";

    private static String sendMessageUrl = "https://pcticket.cstm.org.cn/prod-api/ingore/sendMessage";

    private static String getCodeUrl = "http://api.jfbym.com/api/YmServer/customApi";

    private static String loginUrl = "https://pcticket.cstm.org.cn/prod-api/login";

    private static String getCurrentUserUrl = "https://pcticket.cstm.org.cn/prod-api/getUserInfoToIndividual";


    @Resource
    AccountInfoDao accountInfoDao;

    public String longinCSTM(String loginPhone) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {
            {
                setConnectTimeout(20000);
                setReadTimeout(20000);
            }
        });
        HttpEntity entity = new HttpEntity<>(getBaseHeader());
        ResponseEntity<String> captchaImageRes = restTemplate.exchange(getCaptchaImageUrl, HttpMethod.GET, entity, String.class);
        String captchaImageBody = captchaImageRes.getBody();
        JSONObject captchaImageJson = JSON.parseObject(captchaImageBody);
        if (!ObjectUtils.isEmpty(captchaImageJson) && captchaImageJson.getIntValue("code") == 200) {
            String img = captchaImageJson.getString("img");
            log.info("img:{}", img);
            String uuid = captchaImageJson.getString("uuid");
            /*String captchaUuid = UUID.randomUUID().toString();
            String path = "." + File.separator + captchaUuid + "captcha.png";
            ImageUtils.imagCreate(img, path, 24, 64);
            String captchaCode = ImageUtils.getCaptchaCode(path);
            log.info("获取到的验证码为:{}",captchaCode);
            if(!captchaCode.matches("^[A-Za-z0-9]{4}$")){
                return null;
            }
            try {
                Files.delete(Paths.get(path));
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            HttpHeaders getCodeHeaders = new HttpHeaders();
            getCodeHeaders.setContentType(MediaType.APPLICATION_JSON);
            getCodeHeaders.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            JSONObject getPointBody = new JSONObject();
            getPointBody.put("image", img);
            getPointBody.put("token", "2J3UHYaDJTbELG55unhlt9JkNLKoLpcY9gsEOvbZ2Uc");
            getPointBody.put("type", "10103");
            HttpEntity getCodeEntity = new HttpEntity(getPointBody, getCodeHeaders);
            JSONObject getPointRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getCodeUrl, HttpMethod.POST, getCodeEntity);
            if (ObjectUtils.isEmpty(getPointRes) || getPointRes.getIntValue("code") != 10000) {
                log.info("获取图片验证码失败：{}", getPointRes);
                return null;
            }
            log.info("获取图片验证码：{}", getPointRes);
            JSONObject getPointData = getPointRes.getJSONObject("data");
            String captchaCode = getPointData.getString("data");
            JSONObject param = new JSONObject();
            param.put("smsType", 1);
            param.put("username", loginPhone);
            param.put("uuid", uuid);
            param.put("verifyCode", captchaCode);
            param.put("verifyCodeType", 1);
            HttpHeaders headers = getBaseHeader();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity getMsgCodeEntity = new HttpEntity(param, headers);
            ResponseEntity<String> getMsgCodeRes = restTemplate.exchange(sendMessageUrl, HttpMethod.POST, getMsgCodeEntity, String.class);
            log.info("发送验证码结果:{}", getMsgCodeRes.getBody());
            if (getMsgCodeRes.getBody().contains("550")) {
                log.error("验证码错误");
                return null;
            }
            //等待接收的验证码
            AccountInfoEntity accountInfoEntity = new AccountInfoEntity();
            accountInfoEntity.setAccount(loginPhone);
            accountInfoEntity.setChannel(ChannelEnum.CSTM.getCode());
            long startTimestamp = System.currentTimeMillis();
            LocalDateTime now = LocalDateTime.now();
            Pattern pattern = Pattern.compile("验证码(\\d{6})");
            String verificationCode = "";
            //等待获取短信验证码
            while (true) {
                try {
                    List<AccountInfoEntity> result = accountInfoDao.selectList(accountInfoEntity);
                    if (ObjectUtils.isEmpty(result)||ObjectUtils.isEmpty(result.get(0).getUpdateDate())) {
                        continue;
                    }
                    if (result.get(0).getUpdateDate().after(DateUtils.localDateToDate(now))) {
                        String content = result.get(0).getExt();
                        Matcher matcher = pattern.matcher(content);
                        if (matcher.find()) {
                            verificationCode = matcher.group(1);
                            log.info("提取到的验证码是:{}", verificationCode);
                            break;
                        } else {
                            log.info("未找到验证码");
                        }
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (System.currentTimeMillis() - startTimestamp > 120 * 1000) {
                    break;
                }
            }
            JSONObject loginParam = new JSONObject();
            loginParam.put("loginClient", "1");
            loginParam.put("loginType", "2");
            loginParam.put("password", verificationCode);
            loginParam.put("userType", 1);
            loginParam.put("username", loginPhone);
            HttpEntity loginEntity = new HttpEntity(loginParam, getBaseHeader());
            ResponseEntity<JSONObject> getLoginRes = restTemplate.exchange(loginUrl, HttpMethod.POST, loginEntity, JSONObject.class);
            JSONObject loginRes = getLoginRes.getBody();
            if (loginRes != null) {
                return loginRes.getString("token") == null ? "" : "Bearer " + loginRes.getString("token");
            }
        }
        return null;
    }

    public ServiceResponse<LogInCSTMParam> getCaptchaImage(){
        LogInCSTMParam logInCSTMParam=new LogInCSTMParam();
        HttpEntity entity = new HttpEntity<>(getBaseHeader());
        JSONObject captchaImageRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(),getCaptchaImageUrl, HttpMethod.GET, entity);
        if (!ObjectUtils.isEmpty(captchaImageRes) && captchaImageRes.getIntValue("code") == 200) {
            String img = captchaImageRes.getString("img");
            String uuid = captchaImageRes.getString("uuid");
            logInCSTMParam.setCaptchaImageBase64(img);
            logInCSTMParam.setUuid(uuid);
            return ServiceResponse.createBySuccess(logInCSTMParam);
        }
        return ServiceResponse.createByErrorMessage("获取验证码失败!");
    }

    public ServiceResponse sendMessageCode(LogInCSTMParam logInCSTMParam){
        JSONObject param = new JSONObject();
        param.put("smsType", 1);
        param.put("username", logInCSTMParam.getPhone());
        param.put("uuid", logInCSTMParam.getUuid());
        param.put("verifyCode", logInCSTMParam.getCaptchaImage());
        param.put("verifyCodeType", 1);
        HttpHeaders headers = getBaseHeader();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity getMsgCodeEntity = new HttpEntity(param, headers);
        JSONObject getMsgCodeRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(),sendMessageUrl, HttpMethod.POST, getMsgCodeEntity);
        log.info("发送验证码结果:{}", getMsgCodeRes);
        if(!ObjectUtils.isEmpty(getMsgCodeRes)&&getMsgCodeRes.getIntValue("code")==200){
            return ServiceResponse.createBySuccessMessgge("发送短信成功");
        }
        return ServiceResponse.createByErrorMessage("发送验证码失败:"+JSON.toJSONString(getMsgCodeRes));
    }

    public ServiceResponse login(LogInCSTMParam logInCSTMParam){
        JSONObject loginParam = new JSONObject();
        loginParam.put("loginClient", "1");
        loginParam.put("loginType", "2");
        loginParam.put("password", logInCSTMParam.getVerificationCode());
        loginParam.put("userType", 1);
        loginParam.put("username", logInCSTMParam.getPhone());
        HttpEntity loginEntity = new HttpEntity(loginParam, getBaseHeader());
        JSONObject getLoginRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(),loginUrl, HttpMethod.POST, loginEntity);
        if (!ObjectUtils.isEmpty(getLoginRes)&&getLoginRes.getIntValue("code")==200) {
            String auth="Bearer " + getLoginRes.getString("token");
            Long userId = getUserId(auth);
            if(ObjectUtils.isEmpty(userId)){
                return ServiceResponse.createByErrorMessage("获取用户ID失败");
            }
            AccountInfoEntity accountInfoEntity=new AccountInfoEntity();
            accountInfoEntity.setChannel(ChannelEnum.CSTM.getCode());
            accountInfoEntity.setAccount(logInCSTMParam.getPhone());
            accountInfoEntity.setUpdateDate(new Date());
            accountInfoEntity.setChannelUserId(String.valueOf(userId));
            accountInfoEntity.setHeaders(auth);
            Integer integer = accountInfoDao.updateByChannelAccount(accountInfoEntity);
            if(integer>0){
                return ServiceResponse.createBySuccessMessgge("登录态更新成功");
            }
            return ServiceResponse.createByErrorMessage("登录态保存失败");
        }
        return ServiceResponse.createByErrorMessage("获取登录态失败:"+JSON.toJSONString(getLoginRes));
    }


    private Long getUserId(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authority", "pcticket.cstm.org.cn");
        headers.set("accept", "application/json");
        headers.set("authorization", auth);
        headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        HttpEntity entity = new HttpEntity<>(headers);
        JSONObject getUserRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(),getCurrentUserUrl, HttpMethod.GET, entity);
        JSONObject userInfo = getUserRes == null ? null : getUserRes.getJSONObject("user");
        if (userInfo == null) {
            log.info("获取用户信息失败：{}", getUserRes);
            return null;
        }
        long userId = userInfo.getLongValue("userId");
        return userId;
    }



    private HttpHeaders getBaseHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authority", "pcticket.cstm.org.cn");
        headers.set("accept", "application/json");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        return headers;
    }
}
