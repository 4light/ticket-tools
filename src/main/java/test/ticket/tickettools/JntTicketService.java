package test.ticket.tickettools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

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
    private static String checkUrl = "https://jnt.mfu.com.cn/mmodule/mpwork.captcha/ajax/captcha/check";
    //校验姓名url
    private static String checkNameUrl="https://jnt.mfu.com.cn/ajax?ugi=account&action=checkSensitiveWord&bundleid=com.maiget.tickets&moduleid=6f77be86038c47269f1e00f7ddee9af4";
    //校验身份证号
    private static String checkIdUrl="https://jnt.mfu.com.cn/ajax?ugi=bookingorder&action=checkBookingUserV2&bundleid=com.maiget.tickets&moduleid=6f77be86038c47269f1e00f7ddee9af4";
    private static String useDate = "2024-04-23";


    private static Map<String, Map<String, String>> cookieUserMap = new HashMap<>();

    //private static String cookie="i18n_redirected=zh; Hm_lvt_2a985e9d9884d17b5ed7589beac18720=1712464519,1713056446,1713143974; JSESSIONID=4ECEA5629ACF60CE36A715E84C133CDA; Hm_lpvt_2a985e9d9884d17b5ed7589beac18720=1713241701";

    private static Map<String, String> idNameMap = new HashMap() {{
        put("370827198710060125", "王琨");
        put("370921199101150928", "王雪");
        put("370827200003271329", "张雯");
        put("370827200103053724", "甄舒新");
        put("370827199902240539", "吴承霖");
        put("370827200205061346", "李欣雨");
        put("362321199507283568", "黄紫琴");
        put("370827199510060520", "姜文文");
        put("370827198311213016", "李海龙");
        put("371722200111211722", "王若冰");
        put("370827198810011320", "随备备");
        put("450481199712042044", "莫焕琳");
        put("370827201512181326", "崔廷玉");
        put("370827198006112825", "张婧");
        put("370827200102272000", "田园乐");
    }};
    private static Map<String, String> idNameMap2 = new HashMap() {{
        put("340602200306282435", "张欣阳");
        put("341226198301280326", "魏晓清");
        put("342422198203166104", "吴芬");
        put("342128197208017201", "万丽");
        put("341226197003110121", "魏家平");
        put("341226196911231936", "白良贵");
        put("342422196905041227", "王世格");
        put("342128196903182124", "范平芝");
        put("340405196810121218", "刘维理");
        put("342128196710254427", "程云");
        put("341226196607140044", "魏家荣");
        put("341226196401101971", "凌从林");
        put("340121196309080401", "琚爱珍");
        put("341226196211080268", "魏家芳");
        put("342128196201100382", "吴德兰");
        put("342128195912016238", "朱洪林");
        put("342128195909290041", "周梦芳");
        put("341226195902082612", "汪米书");
        put("341226195806260327", "范维皊");
        put("342128195707190333", "魏建");
        put("34122619560501156X", "李芹");
        put("341226195305050145", "徐顺然");
        put("342128195311270070", "王佩新");
        put("341226195203212625", "徐珍然");
        put("341226194407180229", "童兰英");
    }};
    private static Map<String, String> idNameMap3 = new HashMap() {{
        put("41232219620325452X", "邵素霞");
        put("412322196503154520", "杨秀");
        put("412322197010144548", "张艳霞");
        put("41232419531120451X", "赵尚学");
        put("412324195503154529", "冉令真");
        put("412324195710244519", "茅一友");
        put("411423195802038016", "梁家生");
        put("412324195806094527", "王素梅");
        put("412324196405204515", "陈国新");
        put("412322195506122730", "宋会良");
        put("41232219550726276X", "刘秀兰");
        put("412322195210122731", "朱宜海");
        put("412322197612302734", "朱慈书");
        put("412301196310143014", "徐振保");
        put("412322197212252731", "朱会金");
        put("412322195203172722", "赵培娟");
        put("412322194908152751", "朱慈贾");
        put("41232219651016484X", "徐忠秀");
        put("41232219651015481X", "谢立友");
        put("412322195508013925", "张云真");
        put("412322195408063925", "张秀云");
        put("412322196205043961", "冯运丽");
        put("412322196812273929", "曹林敏");
        put("412322196404143922", "潘凤琴");
        put("412322196608064521", "窦银阁");
        put("412322196405223916", "宗勤科");
        put("411402201510240163", "宗倩倩");
        put("411402201304090096", "宗树亮");
        put("41140220100325021X", "宗树洞");
        put("412322195510123920", "张爱云");
        put("412322196105173929", "陈永玲");
        put("412322193409263946", "赵桂芝");
        put("41232219560519393X", "魏振江");
        put("41232219480925391X", "宗玉科");
        put("412322195012263920", "刘爱真");
        put("412322194111043910", "宋云林");
        put("412322195608153941", "门三秀");
        put("41232219560714391X", "李传增");
        put("41232219600914452X", "楚德兰");
        put("412327195402168445", "闫秀真");
        put("412327195003155030", "陈召举");
        put("411424198811094562", "白翠红");
        put("412327196402264562", "梁月芳");
        put("412327195809254521", "邓新玲");
        put("412327195610134522", "李秀芝");
        put("412326197302062435", "冯为谦");
        put("412327198110199227", "宋洪花");
        put("41232719541102921X", "宋清亮");
        put("412327195608029264", "邹玉真");
        put("412327195606118415", "张春宇");
    }};

    private static Map<String, String> userPwdMap = new HashMap() {{
        //put("17610400550", "LGZlgz123!");
        //put("13521436109", "z1234567@");
        put("13522369632", "Lht123456789@");
    }};

    private static Map<String, JSONObject> sessionMap = new HashMap();

    @Scheduled(cron = "0 29 12 * * ?")
    public void initLogin() {
        List<Map<String, String>> idNameMapList = new ArrayList();
        idNameMapList.add(idNameMap);
        //idNameMapList.add(idNameMap2);
        //idNameMapList.add(idNameMap3);
        for (Map.Entry<String, String> userPwdEntry : userPwdMap.entrySet()) {
            int i = 0;
            RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
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
            HttpEntity getCsrfEntity = new HttpEntity<>(headers);
            JSONObject getCsrfJson = getResponse(restTemplate, getCsrfUrl, HttpMethod.GET, getCsrfEntity);
            if (ObjectUtils.isEmpty(getCsrfJson)) {
                log.info("获取CSRF失败");
                return;
            }
            String csrf_req = getCsrfJson.getString("csrf_req");
            String csrf_ts = getCsrfJson.getString("csrf_ts");
            String csrf = DigestUtils.md5Hex(csrf_req + csrf_ts);
            String bodyFormat = MessageFormat.format("loginid={0}&passwd={1}&csrf_req={2}&csrf_ts={3}&csrf={4}", userPwdEntry.getKey(), userPwdEntry.getValue(), csrf_req, csrf_ts, csrf);
            headers.set("Content-Length", String.valueOf(customURLEncode(bodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity loginEntity = new HttpEntity<>(bodyFormat, headers);
            ResponseEntity<String> doLogin = restTemplate.exchange(loginUrl, HttpMethod.POST, loginEntity, String.class);
            HttpHeaders loginHeaders = doLogin.getHeaders();
            List<String> cookies = loginHeaders.get("set-cookie");
            log.info("获取到cookie:{}", cookies.get(0));
            cookieUserMap.put(cookies.get(0), idNameMapList.get(i));
            i++;
        }
    }

    @Scheduled(cron = "2 30 12 * * ?")
    public void doSnatchingJnt() {
        try {
            for (Map.Entry<String, Map<String, String>> cookieUser : cookieUserMap.entrySet()) {
                RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
                JSONObject proxy = ProxyUtil.getProxy();
                if (!ObjectUtils.isEmpty(proxy)) {
                    restTemplate=TemplateUtil.initSSLTemplateWithProxy(proxy.getString("ip"), proxy.getIntValue("port"));
                }
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
                //查询余票
                headers.set("Referer", "https://jnt.mfu.com.cn/page/tg");
                //headers.set("cookie", cookie.get(0));
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.set("cookie", cookieUser.getKey());
                String queryFormat = MessageFormat.format("fromtype={0}&siteid={1}", "GROUP", "7e97d18d179c4791bab189f8de87ee9d");
                headers.set("Content-Length", String.valueOf(customURLEncode(queryFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
                HttpEntity queryEntity = new HttpEntity<>(queryFormat, headers);
                JSONObject queryRes = getResponse(restTemplate, bookingQueryUrl, HttpMethod.POST, queryEntity);
                if (ObjectUtils.isEmpty(queryRes)) {
                    log.info("查询余票数据失败");
                    return;
                }
                if (StrUtil.equals(queryRes.getString("code"), "A00013")) {
                    check(queryRes.getString("captcha_type"), restTemplate, headers);
                    queryRes = getResponse(restTemplate, bookingQueryUrl, HttpMethod.POST, queryEntity);
                }
                List<String> sessionList = new ArrayList();
                if (StrUtil.equals(queryRes.getString("code"), "A00006")) {
                    JSONObject useDateTickInfo = queryRes.getJSONObject(useDate);
                    JSONArray sessions = useDateTickInfo.getJSONArray("sessions");
                    for (int i = 0; i < sessions.size(); i++) {
                        String eventsSessionId = sessions.getJSONObject(i).getString("eventssessionid");
                        sessionList.add(0, eventsSessionId);
                        sessionMap.put(eventsSessionId, sessions.getJSONObject(i));
                    }
                }
                String referer = "https://jnt.mfu.com.cn/page/tg/editorder/%s?date=%s&begintime=%s&endtime=%s&booking_including_self=0&maxnums=60&minnums=10";
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                outLoop:
                while (true) {
                    for (String session : sessionList) {
                        String format = String.format(referer, session, useDate, sessionMap.get(session).getString("begintime"), sessionMap.get(session).getString("endtime"));
                        headers.set("Referer", format);
                        //校验姓名和身份证号
                        Map<String, String> currentIdNameMap = cookieUser.getValue();
                        for (String name : currentIdNameMap.values()) {
                            String reqParam="str="+name;
                            headers.set("Content-Length", String.valueOf(customURLEncode(reqParam, "utf-8").getBytes(StandardCharsets.UTF_8).length));
                            HttpEntity checkNameEntity = new HttpEntity<>(reqParam, headers);
                            JSONObject response = TemplateUtil.getResponse(restTemplate, checkNameUrl, HttpMethod.POST, checkNameEntity);
                            if(!StrUtil.equals("A00006",response.getString("code"))){
                                log.info("姓名-{}校验不通过:{}",name,response);
                                break outLoop;
                            }
                        }
                        String idNums = String.join(",", currentIdNameMap.values());
                        String idParamForm=MessageFormat.format("eventssessionid={0}&usertype=tg&idnums={1}",session,idNums);
                        headers.set("Content-Length", String.valueOf(customURLEncode(idParamForm, "utf-8").getBytes(StandardCharsets.UTF_8).length));
                        HttpEntity checkIdEntity = new HttpEntity<>(idParamForm, headers);
                        JSONObject checkIdRes = getResponse(restTemplate, checkIdUrl, HttpMethod.POST, checkIdEntity);
                        if(!StrUtil.equals("A00006",checkIdRes.getString("code"))){
                            log.info("身份证校验不通过:{}",checkIdRes);
                            break outLoop;
                        }
                        //提交订单
                        String submitBodyFormat = MessageFormat.format("usertype=tg&eventssessionid={0}&bookingdata={1}", session, buildParam(cookieUser.getValue()));
                        headers.set("Content-Length", String.valueOf(customURLEncode(submitBodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
                        HttpEntity submitEntity = new HttpEntity<>(submitBodyFormat, headers);
                        JSONObject submitRes = getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                        log.info("请求结果:{}", submitRes);
                        if (StrUtil.equals(submitRes.getString("code"), "A00006")) {
                            break outLoop;
                        }
                        if (StrUtil.equals(submitRes.getString("code"), "A00013")) {
                            check(submitRes.getString("captcha_type"), restTemplate, headers);
                            submitRes = getResponse(restTemplate, submitUrl, HttpMethod.POST, submitEntity);
                            log.info("请求结果:{}", submitRes);
                            if (StrUtil.equals(submitRes.getString("code"), "A00006")) {
                                break outLoop;
                            }
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
                JSONObject getPointRes = getResponse(restTemplate, getPointUrl, HttpMethod.POST, entity);
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
                System.out.println(JSON.toJSONString(posList));
                pointJson = doClickSecret(posList, secretKey);
            } else {
                //请求滑块验证码
                String uuid = UUID.randomUUID().toString();
                String backImageName = "./" + uuid + "_back.png";
                String sliderImageName = "./" + uuid + "_slider.png";
                ImageUtils.imagCreate(originalImageBase64, backImageName, 155, 310);
                String jigsawImageBase64 = getCaptchaRes.getJSONObject("repData").getString("jigsawImageBase64");
                ImageUtils.imagCreate(jigsawImageBase64, sliderImageName, 155, 50);
                Double point = getPoint(backImageName, sliderImageName, uuid);
                pointJson = doSecretKey(point, secretKey);
            }
            headers.set("Referer", "https://jnt.mfu.com.cn/page/tg");
            headers.setContentType(MediaType.APPLICATION_JSON);
            JSONObject checkBody = new JSONObject();
            checkBody.put("captchaType", captchaType);
            checkBody.put("pointJson", pointJson);
            checkBody.put("token", token);
            HttpEntity checkEntity = new HttpEntity<>(checkBody, headers);
            JSONObject checkRes = getResponse(restTemplate, checkUrl, HttpMethod.POST, checkEntity);
            log.info("验证码校验结果:{}",checkRes);
            if(!StrUtil.equals("A00006",checkRes.getString("code"))){
                check(captchaType,restTemplate,headers);
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


    private JSONArray buildParam(Map<String, String> idNameM) {
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
        System.out.println(getId());
    }
}
