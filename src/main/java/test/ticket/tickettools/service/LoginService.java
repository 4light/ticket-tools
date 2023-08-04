package test.ticket.tickettools.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.dao.PhoneInfoDao;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;
import test.ticket.tickettools.utils.DateUtils;
import test.ticket.tickettools.utils.ImageUtils;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LoginService {
    private static String getCaptchaImageUrl = "https://pcticket.cstm.org.cn/prod-api/ingore/captchaImage?verifyType=1";

    private static String sendMessageUrl = "https://pcticket.cstm.org.cn/prod-api/ingore/sendMessage";

    private static String loginUrl="https://pcticket.cstm.org.cn/prod-api/login";

    @Resource
    PhoneInfoDao phoneInfoDao;

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
            String uuid = captchaImageJson.getString("uuid");
            String captchaUuid = UUID.randomUUID().toString();
            String path = "." + File.separator + captchaUuid + "captcha.png";
            ImageUtils.imagCreate(img, path, 55, 155);
            ImageUtils.toBlackImag(path,captchaUuid);
            String captchaCode = ImageUtils.getCaptchaCode("./" + captchaUuid + "_black.png");
            log.info("获取到的验证码为:{}",captchaCode);
            JSONObject param=new JSONObject();
            param.put("smsType",1);
            param.put("username",loginPhone);
            param.put("uuid",uuid);
            param.put("verifyCode",captchaCode);
            param.put("verifyCodeType",1);
            HttpHeaders headers=getBaseHeader();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity getMsgCodeEntity = new HttpEntity(param,headers);
            ResponseEntity<String> getMsgCodeRes = restTemplate.exchange(sendMessageUrl, HttpMethod.POST, getMsgCodeEntity, String.class);
            log.info("发送验证码结果:{}",getMsgCodeRes.getBody());
            if(getMsgCodeRes.getBody().contains("550")){
                log.error("验证码错误");
                return null;
            }
            //等待接收的验证码
            PhoneInfoEntity phoneInfoEntity=new PhoneInfoEntity();
            phoneInfoEntity.setPhoneNum(loginPhone);
            long startTimestamp = System.currentTimeMillis();
            LocalDateTime now=LocalDateTime.now();
            Pattern pattern = Pattern.compile("验证码(\\d{6})");
            String verificationCode="";
            //等待获取短信验证码
            while(true){
                PhoneInfoEntity result = phoneInfoDao.select(phoneInfoEntity);
                if(ObjectUtils.isEmpty(result)){
                    continue;
                }
                if(result.getCreateDate().before(DateUtils.localDateToDate(now))){
                    String content = result.getContent();
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        verificationCode = matcher.group(1);
                        log.info("提取到的验证码是:{}",verificationCode);
                        break;
                    } else {
                       log.info("未找到验证码");
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(System.currentTimeMillis()-startTimestamp>120*1000){
                    break;
                }
            }
            JSONObject loginParam=new JSONObject();
            loginParam.put("loginClient","1");
            loginParam.put("loginType","2");
            loginParam.put("password",verificationCode);
            loginParam.put("userType",1);
            loginParam.put("username",loginPhone);
            ResponseEntity<String> getLoginRes = restTemplate.exchange(sendMessageUrl, HttpMethod.POST, getMsgCodeEntity, String.class);
            HttpHeaders headers1 = getLoginRes.getHeaders();
            for (Map.Entry<String, List<String>> stringListEntry : headers1.entrySet()) {
                System.out.println(stringListEntry.getKey()+":"+stringListEntry.getValue());
            }
        }
        return null;
    }

    private HttpHeaders getBaseHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authority", "pcticket.cstm.org.cn");
        headers.set("accept", "application/json");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
        return headers;
    }
}
