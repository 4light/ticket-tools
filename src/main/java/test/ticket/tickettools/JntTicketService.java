package test.ticket.tickettools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static test.ticket.tickettools.ImageUtils.getPoint;


@Slf4j
@Configuration
@EnableScheduling
public class JntTicketService {
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
    private static String checkUrl="https://jnt.mfu.com.cn/mmodule/mpwork.captcha/ajax/captcha/check";
    private static String useDate = "2024-04-20";

    private static String user = "410425199810226068";
    private static String pwd = "123456ywy!";

    private static String cookie="i18n_redirected=zh; Hm_lvt_2a985e9d9884d17b5ed7589beac18720=1712464519,1713056446; JSESSIONID=E8051F8B42700E8842E63591C13FE7EA; UWAFCAC=4uofgr1aVfw4tSWHmXS4JDnCmxslWtavpFzctKe8xAmzGKxI0dX2G3TNAWPaxbNIXcVVPggGke5kBKHWSW4iKloIglk6Bf7PIsjGe4g6z48=; Hm_lpvt_2a985e9d9884d17b5ed7589beac18720=1713072750";


    private static Map<String, String> iDNameMap = new HashMap() {{
//        put("220281199211070019", "刘东辉");
//        put("220281197007200083", "刘坤");
        put("340824198805196610", "葛腾");
        put("342824196709277018", "葛爱国");
        put("342824196409257023", "丁玉南");
        put("340824199105016628", "葛菁菁");
        put("34060320160421401X", "徐俊皓");
    }};

    private static Map<String, JSONObject> sessionMap = new HashMap();

    @Resource
    private TaskExecutorConfig taskExecutorConfig;


    //@Scheduled(cron = "0/1 * * * * ?")
    public void doSnatchingJnt() {
        try {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build();

            ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            ((HttpComponentsClientHttpRequestFactory) requestFactory).setHttpClient(HttpClients.custom()
                    .setSSLContext(sslContext)
                    .disableCookieManagement()
                    .build());
            ((HttpComponentsClientHttpRequestFactory) requestFactory).setConnectTimeout(20000);
            ((HttpComponentsClientHttpRequestFactory) requestFactory).setReadTimeout(20000);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            //获取Csrf
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
            /*HttpEntity getCsrfEntity = new HttpEntity<>(headers);
            JSONObject getCsrfJson = getResponse(restTemplate, getCsrfUrl, HttpMethod.GET, getCsrfEntity);
            if (ObjectUtils.isEmpty(getCsrfJson)) {
                log.info("获取CSRF失败");
                return;
            }
            String csrf_req = getCsrfJson.getString("csrf_req");
            String csrf_ts = getCsrfJson.getString("csrf_ts");
            String csrf = DigestUtils.md5Hex(csrf_req + csrf_ts);
            String bodyFormat = MessageFormat.format("loginid={0}&passwd={1}&csrf_req={2}&csrf_ts={3}&csrf={4}", user, pwd, csrf_req, csrf_ts, csrf);
            headers.set("Content-Length", String.valueOf(customURLEncode(bodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity loginEntity = new HttpEntity<>(bodyFormat, headers);
            ResponseEntity<String> doLogin = restTemplate.exchange(loginUrl, HttpMethod.POST, loginEntity, String.class);
            HttpHeaders loginHeaders = doLogin.getHeaders();
            List<String> cookie = loginHeaders.get("set-cookie");*/
            //查询余票
            headers.set("Referer", "https://jnt.mfu.com.cn/page/tg");
            //headers.set("cookie", cookie.get(0));
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("cookie", cookie);
            String queryFormat = MessageFormat.format("fromtype={0}&siteid={1}", "GROUP", "7e97d18d179c4791bab189f8de87ee9d");
            headers.set("Content-Length", String.valueOf(customURLEncode(queryFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
            HttpEntity queryEntity = new HttpEntity<>(queryFormat, headers);
            JSONObject queryRes = getResponse(restTemplate, bookingQueryUrl, HttpMethod.POST, queryEntity);
            if (ObjectUtils.isEmpty(queryRes)) {
                log.info("查询余票数据失败");
                return;
            }
            if (StrUtil.equals(queryRes.getString("code"), "A00013")) {
                check(queryRes.getString("captcha_type"),restTemplate,headers);
                queryRes = getResponse(restTemplate, bookingQueryUrl, HttpMethod.POST, queryEntity);
            }
            List<String> sessionList = new ArrayList();
            if (StrUtil.equals(queryRes.getString("code"), "A00006")) {
                JSONObject useDateTickInfo = queryRes.getJSONObject(useDate);
                JSONArray sessions = useDateTickInfo.getJSONArray("sessions");
                for (int i = 0; i < sessions.size(); i++) {
                    String eventsSessionId = sessions.getJSONObject(i).getString("eventssessionid");
                    sessionList.add(eventsSessionId);
                    sessionMap.put(eventsSessionId, sessions.getJSONObject(i));
                }
            }
            String referer = "https://jnt.mfu.com.cn/page/tg/editorder/%s?date=%s&begintime=%s&endtime=%s&booking_including_self=0&maxnums=60&minnums=10";
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String param = "[{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"uj5AO2B9XuKHRot4cJfRbY8XzTNyVTkP\",\"realname\":\"王永梅\",\"doctype\":\"IDCARD\",\"idnum\":\"210821197404243364\"},{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"uejSeX9a2P2GqXhKzQf19sKdivy01IV9\",\"realname\":\"韩美玉\",\"doctype\":\"IDCARD\",\"idnum\":\"210881199910235960\"},{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"uyuhTT65uBKYUl8vviLFn0D2CLcAGfHI\",\"realname\":\"韩学武\",\"doctype\":\"IDCARD\",\"idnum\":\"21082419770510595X\"},{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"u6D19qa8BgGkgLUdNZrNgZ8kf2AvaLez\",\"realname\":\"陈英梅\",\"doctype\":\"IDCARD\",\"idnum\":\"21088119790306616X\"},{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"uqTfpi1asJpWlTe1FsDdgSVl5u3frn3Y\",\"realname\":\"张磊\",\"doctype\":\"IDCARD\",\"idnum\":\"210811197504240045\"},{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"uFdw3L7MXtXglWcPv5GBH6WPrfguigjT\",\"realname\":\"马罡越\",\"doctype\":\"IDCARD\",\"idnum\":\"210803201512031533\"},{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"ua8aIceUXtGckBSsI0x8s9P8UEWcAeGw\",\"realname\":\"张力\",\"doctype\":\"IDCARD\",\"idnum\":\"210811197103071025\"},{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"umCuTRe4hbcQI7UdUUKwIUDBkuwiADg9\",\"realname\":\"李晓明\",\"doctype\":\"IDCARD\",\"idnum\":\"210881198512041445\"},{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"utrZONkK59rvyNXX3ub0JGxQjoTRx9Q3\",\"realname\":\"李美静\",\"doctype\":\"IDCARD\",\"idnum\":\"210881200802251440\"},{\"invalid\":false,\"errmsg_realname\":\"\",\"errmsg_doctype\":\"\",\"errmsg_idnum\":\"\",\"jkbColor\":-2,\"id\":\"u5CHhm7tIaKqvINo0KM8NQYEQSHuHmmT\",\"realname\":\"林姣\",\"doctype\":\"IDCARD\",\"idnum\":\"210881198804146329\"}]";
            AtomicBoolean go= new AtomicBoolean(true);
            while(go.get()) {
                for (String session : sessionList) {
                        String format = String.format(referer, session, useDate, sessionMap.get(session).getString("begintime"), sessionMap.get(session).getString("endtime"));
                        String submitBodyFormat = MessageFormat.format("usertype=tg&eventssessionid={0}&bookingdata={1}", session, param);
                        headers.set("Referer", format);
                        headers.set("Content-Length", String.valueOf(customURLEncode(submitBodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
                        HttpEntity submitEntity = new HttpEntity<>(submitBodyFormat, headers);
                        JSONObject submitRes = getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                        log.info("请求结果:{}",submitRes);
                        if(StrUtil.equals(submitRes.getString("code"), "A00006")){
                            go.set(false);
                        }
                        if (StrUtil.equals(submitRes.getString("code"), "A00013")) {
                            check(submitRes.getString("captcha_type"),restTemplate,headers);
                            submitRes = getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                            log.info("请求结果:{}",submitRes);
                            if(StrUtil.equals(submitRes.getString("code"), "A00006")){
                                go.set(false);
                            }
                        }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONObject getResponse(RestTemplate restTemplate, String url, HttpMethod httpMethod, HttpEntity httpEntity) {
        try {
            ResponseEntity<String> checkUserRes = restTemplate.exchange(url, httpMethod, httpEntity, String.class);
            String checkUserResBody = checkUserRes.getBody();
            if (StrUtil.isEmpty(checkUserResBody)) {
                log.info("获取数据失败", checkUserResBody);
                return null;
            }
            return JSON.parseObject(checkUserResBody);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void check(String captchaType, RestTemplate restTemplate,HttpHeaders headers){
        //如果需要验证，请求下验证码
            //获取验证码图片
            headers.remove("Referer");
            headers.setContentType(MediaType.APPLICATION_JSON);
            JSONObject param = new JSONObject();
            param.put("captchaType", captchaType);
            param.put("clientUid", getQueryCaptchaPoint());
            param.put("ts", System.currentTimeMillis());
            HttpEntity getCaptchaEntity = new HttpEntity<>(param, headers);
            JSONObject getCaptchaRes = getResponse(restTemplate, getCaptchaUrl, HttpMethod.POST, getCaptchaEntity);
            if (ObjectUtils.isEmpty(getCaptchaRes)) {
                log.info("请求验证码错误");
                return;
            }
            //如果是文字点选需要调第三方
            if (StrUtil.equals(getCaptchaRes.getString("code"), "A00006")) {
                String originalImageBase64 = getCaptchaRes.getJSONObject("repData").getString("originalImageBase64");
                String secretKey = getCaptchaRes.getJSONObject("repData").getString("secretKey");
                String token = getCaptchaRes.getJSONObject("repData").getString("token");
                String pointJson=null;
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
                    HttpEntity entity = new HttpEntity<>(getPointBody,getPointHeaders);
                    JSONObject getPointRes = getResponse(restTemplate, getPointUrl, HttpMethod.POST, entity);
                    if (ObjectUtils.isEmpty(getPointRes) || getPointRes.getIntValue("code") != 10000) {
                        log.info("获取图片坐标失败：{}", getPointRes);
                        return;
                    }
                    JSONObject data = getPointRes.getJSONObject("data");
                    String pointArrStr = data.getString("data");
                    String[] substring = pointArrStr.split("\\|");
                    List<JSONObject> posList=new ArrayList<>();
                    for (String s : substring) {
                        String[] split = s.split(",");
                        JSONObject item=new JSONObject();
                        item.put("x",Math.round((Double.valueOf(split[0])*310)/330));
                        item.put("y",Math.round((Double.valueOf(split[1])*155)/155));
                        posList.add(item);
                    }
                    System.out.println(JSON.toJSONString(posList));
                    pointJson= doClickSecret(posList, secretKey);
                } else {
                    //请求滑块验证码
                    String uuid = UUID.randomUUID().toString();
                    String backImageName = "./" + uuid + "_back.png";
                    String sliderImageName = "./" + uuid + "_slider.png";
                    ImageUtils.imagCreate(originalImageBase64, backImageName, 155, 310);
                    String jigsawImageBase64 = getCaptchaRes.getJSONObject("repData").getString("jigsawImageBase64");
                    ImageUtils.imagCreate(jigsawImageBase64, sliderImageName, 155, 50);
                    Double point = getPoint(backImageName, sliderImageName, uuid);
                    pointJson= doSecretKey(point, secretKey);
                }
                headers.set("Referer","https://jnt.mfu.com.cn/page/tg");
                headers.setContentType(MediaType.APPLICATION_JSON);
                JSONObject checkBody=new JSONObject();
                checkBody.put("captchaType",captchaType);
                checkBody.put("pointJson",pointJson);
                checkBody.put("token",token);
                HttpEntity checkEntity = new HttpEntity<>(checkBody,headers);
                JSONObject checkRes = getResponse(restTemplate, checkUrl, HttpMethod.POST, checkEntity);
                System.out.println(JSON.toJSONString(checkRes));
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


    public static String doSecretKey(Double x, String secretKey) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval(new java.io.InputStreamReader(TicketSnatchingSchedule.class.getResourceAsStream("/META-INF/resources/webjars/crypto-js/3.1.9-1/crypto-js.js")));

            // 读取 JavaScript 文件并执行
            String scriptFile = "./getPoint.js";
            engine.eval(new java.io.FileReader(scriptFile));
            JSONObject param = new JSONObject();
            param.put("x", x);
            param.put("y", 5);
            // 获取 JavaScript 函数的执行结果
            Invocable invocable = (Invocable) engine;
            Object result = invocable.invokeFunction("getPoint", param.toString(), secretKey);
            if (result != null) {
                return result.toString();
            }
        } catch (ScriptException | java.io.FileNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static String doClickSecret(List<JSONObject> checkPosJson, String secretKey) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            engine.eval(new java.io.InputStreamReader(TicketSnatchingSchedule.class.getResourceAsStream("/META-INF/resources/webjars/crypto-js/3.1.9-1/crypto-js.js")));

            // 读取 JavaScript 文件并执行
            String scriptFile = "./getPoint.js";
            engine.eval(new java.io.FileReader(scriptFile));
            // 获取 JavaScript 函数的执行结果
            Invocable invocable = (Invocable) engine;
            Object result = invocable.invokeFunction("getPoint", JSON.toJSONString(checkPosJson), secretKey);
            if (result != null) {
                return result.toString();
            }
        } catch (ScriptException | java.io.FileNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }


    private JSONArray buildParam() {
        for (Map.Entry<String, String> idNameEntry : iDNameMap.entrySet()) {
            JSONObject item = new JSONObject();
            item.put("invalid", false);
            item.put("errmsg_realname", "");
            item.put("errmsg_doctype", "");
            item.put("errmsg_idnum", "");
            item.put("jkbColor", -2);
            //item.put("id", );
            item.put("realname", idNameEntry.getValue());
            item.put("doctype", "IDCARD");
            item.put("idnum", idNameEntry.getKey());
        }
        return null;
    }

    public static void main(String[] args) {
        //doSnatchingJnt();
        /*String posArrStr="45,92|91,40|217,16";
        String[] substring = posArrStr.split("\\|");
        List<JSONObject> posList=new ArrayList<>();
        for (String s : substring) {
            String[] split = s.split(",");
            JSONObject item=new JSONObject();
            item.put("x",Math.round((Double.valueOf(split[0])*310)/330));
            item.put("y",Math.round((Double.valueOf(split[1])*155)/155));
            posList.add(item);
        }
        System.out.println(JSON.toJSONString(posList));
        System.out.println(doClickSecret(posList, "owdNoYDuS651jtGd"));*/
        /*JntTicketService jntTicketService=new JntTicketService();
        jntTicketService.doSnatchingJnt();*/
        String referer = "https://jnt.mfu.com.cn/page/tg/editorder/%s?date=%s&begintime=%s&endtime=%s&booking_including_self=0&maxnums=60&minnums=10";

        String format = String.format(referer, "aaaa", useDate, "2023-03-22", "2024-03-22");
        System.out.println(format);

    }
}
