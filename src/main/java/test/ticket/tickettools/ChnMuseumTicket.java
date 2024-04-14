package test.ticket.tickettools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Configuration
@EnableScheduling
public class ChnMuseumTicket {
    //查询用户信息
    private static String queryUserInfoUrl = "https://lotswap.dpm.org.cn/lotsapi/leaguer/api/userLeaguer/manage/leaguerInfo?cipherText=0&merchantId=2655&merchantInfoId=2655";
    //查询余票
    private static String queryImperialPalaceTicketsUrl = "https://lotswap.dpm.org.cn/lotsapi/merchant/api/fsyy/calendar?parkId=11324&year=%s&month=%s&merchantId=2655&merchantInfoId=2655";
    //获取门票种类
    private static String getTicketGridUrl = "https://lotswap.dpm.org.cn/lotsapi/merchant/api/merchantParkTicketGridNew?date=%s&merchantParkInfoId=11324&currPage=1&pageSize=200&merchantInfoId=2655&playDate=%s&businessType=park";
    //获取余票信息
    private static String getReserveListUrl = "https://lotswap.dpm.org.cn/lotsapi/order/api/batchTimeReserveList";
    //校验成员信息
    private static String checkUserUrl = "https://lotswap.dpm.org.cn/dubboApi/trade-core/tradeCreateService/ticketVerificationCheck";
    //提交订单
    private static String createUrl = "https://lotswap.dpm.org.cn/dubboApi/trade-core/tradeCreateService/create?sign=%s&timestamp=%s";

    private static String accessToken = "eyJhbGciOiJIUzUxMiJ9.eyJ1IjoiNjM3NjA0MzA0ODM2MjkyNjA4IiwidCI6IjAiLCJleHAiOjE3MTMwMTYzMTR9.INIikFKeJmZ_sqKcl6Vx-QaV_07atDwL8tBeRoLeKJsnDjFfLp_sEY2Qbku7gIjNF2sFxyz85b_CXqm1GaR6mQ";

    private static String useDate = "2024-04-20";
    private static String credentialNo = "13082819891227801X";
    private static String nickName = "张阳";
    private static String mpOpenId = "oOya25BlL4u13ahkDCe7hv72lz3I";

    private static String mpDeviceToken = "v2:wqkBQJniqpm4s6AT6CtYx+RD95eT8I4MYnKqMrJWUIW2bk5qxrExewC7k99NAkxjnaOK4PBvupP5MIGnEAnQmOHnsAJ093fAvE86P7vhXSWuI3y7699DL6S2N94ZYC2HCRefY/WosjDrEXJB7eGg4xGumjCrs7Ko+XjDeCxXukO+30A7QtI4OVA5XdmpS3g+jmGj7FJCkeIvQTYsL7/U5jpGXK19IbU=";
    private static Map<String, JSONObject> typeTicketMap = new HashMap();
    private static Map<String, JSONObject> modelCodeTicketInfoMap = new HashMap();

    private static Map<String, String> iDNameMap = new HashMap() {{
//        put("220281199211070019", "刘东辉");
//        put("220281197007200083", "刘坤");
        put("340824198805196610", "葛腾");
        put("342824196709277018", "葛爱国");
        put("342824196409257023", "丁玉南");
        put("340824199105016628", "葛菁菁");
        put("34060320160421401X", "徐俊皓");
    }};

    @Resource
    private TaskExecutorConfig taskExecutorConfig;


    //@Scheduled(cron = "0/1 * * * * ?")
    public void doSnatchingChnMuseum() {
        //记录有票的具体日期
        JSONArray parkFsyyDetailDTOs = new JSONArray();
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
            LocalDate now = LocalDate.now();
            String headerStr = FileUtil.readString("/Users/devin.zhang/Desktop/record", Charset.defaultCharset());
            JSONObject headerJson = JSON.parseObject(headerStr);
            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
//            headers.set("accept", "*/*");
//            headers.set("xweb_xhr", "1");
//            headers.set("app", "app_qqmap_tickets");
//            headers.set("Sec-Fetch-Site", "cross-site");
//            headers.set("Sec-Fetch-Mode", "cors");
//            headers.set("Sec-Fetch-Dest", "empty");
//            headers.set("Accept-Language", "zh-CN,zh;q=0.9");
//            headers.set("access-token", accessToken);
//            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36 MicroMessenger/6.8.0(0x16080000) NetType/WIFI MiniProgramEnv/Mac MacWechat/WMPF MacWechat/3.8.7(0x13080712) XWEB/1191");
            for (Map.Entry<String, Object> headerEntry : headerJson.entrySet()) {
                headers.set(headerEntry.getKey(), headerEntry.getValue().toString());
            }

            HttpEntity getUserEntity = new HttpEntity<>(headers);
            JSONObject getUserJson = getResponse(restTemplate, queryUserInfoUrl, HttpMethod.GET, getUserEntity);
            if (ObjectUtils.isEmpty(getUserJson)) {
                return;
            }
            headers.set("Host", "lotswap.dpm.org.cn");
            headers.set("mpOpenId", headerJson.getString("mpOpenId"));
            headers.set("mpDeviceToken", mpDeviceToken);
            HttpEntity entity = new HttpEntity<>(headers);
            //查询当月余票
            String month = now.getMonthValue() > 10 ? String.valueOf(now.getMonthValue()) : "0" + now.getMonthValue();
            queryImperialPalaceTicketsUrl = String.format(queryImperialPalaceTicketsUrl, now.getYear(), month);
            JSONObject responseJson = getResponse(restTemplate, queryImperialPalaceTicketsUrl, HttpMethod.GET, entity);
            JSONArray data = responseJson.getJSONArray("data");
            if (ObjectUtils.isEmpty(data)) {
                //log.info("获取到的场次失败");
                return;
            }
            boolean haveTicket = false;
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);
                if (StrUtil.equals("T", item.getString("saleStatus")) && item.getIntValue("stockNum") == 1) {
                    if (StrUtil.equals(useDate, item.getString("occDate"))) {
                        JSONArray parkFsyyDetailDTOS = item.getJSONArray("parkFsyyDetailDTOS");
                        if (!ObjectUtils.isEmpty(parkFsyyDetailDTOS)) {
                            for (int j = 0; j < parkFsyyDetailDTOS.size(); j++) {
                                JSONObject parkFsyyDetailJson = parkFsyyDetailDTOS.getJSONObject(j);
                                if (parkFsyyDetailJson.getIntValue("stockNum") == 1 && parkFsyyDetailJson.getIntValue("totalNum") == 1) {
                                    haveTicket = true;
                                    parkFsyyDetailDTOs.add(parkFsyyDetailJson);
                                }
                            }
                            break;
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
            getTicketGridUrl = String.format(getTicketGridUrl, useDate, useDate);
            JSONObject ticketGridJson = getResponse(restTemplate, getTicketGridUrl, HttpMethod.GET, getTicketEntity);
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
            for (int i = 0; i < ticketList.size(); i++) {
                JSONObject ticketInfo = ticketList.getJSONObject(i);
                String nickName = ticketInfo.getString("nickName");
                String modelCode = ticketInfo.getString("modelCode");
                JSONObject tickCodeInfo = new JSONObject();
                tickCodeInfo.put("modelCode", modelCode);
                tickCodeInfo.put("externalCode", ticketInfo.getString("externalCode"));
                tickCodeInfo.put("startTime", useDate);
                tickCodeInfo.put("endTime", useDate);
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
                //ticketInfo.put("parkFsyyDetailDTO", parkFsyyDetailDTO);
                modelCodeTicketInfoMap.put(modelCode, ticketInfo);
                ticketReserveList.add(tickCodeInfo);
            }
            headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
            String bodyFormat = MessageFormat.format("queryParam={0}&merchantId=2655&merchantInfoId=2655", ticketReserveList);
            //需要设置content_type application/x-www-form-urlencoded
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            URLEncoder.encode(bodyFormat, "utf-8");
            headers.set("Content-Length", String.valueOf(customURLEncode(bodyFormat, "utf-8").getBytes(StandardCharsets.UTF_8).length));
            HttpEntity getReserveListEntity = new HttpEntity<>(bodyFormat, headers);
            JSONObject reserveListJson = getResponse(restTemplate, getReserveListUrl, HttpMethod.POST, getReserveListEntity);
            if (ObjectUtils.isEmpty(reserveListJson)) {
                return;
            }
            JSONArray reserveList = reserveListJson.getJSONArray("data");
            if (ObjectUtils.isEmpty(reserveList)) {
                log.info("批量获取余票数据失败batchTimeReserveList", reserveListJson);
                return;
            }
            //创建订单
            while (true) {
                //校验用户信息
                headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
                headers.setContentType(MediaType.APPLICATION_JSON);
                JSONObject checkUserBody = buildCheckUserParam();
                log.info("校验身份信息入参：{}", JSON.toJSONString(checkUserBody));
                HttpEntity checkUserEntity = new HttpEntity<>(checkUserBody, headers);
                JSONObject checkUserBodyJson = getResponse(restTemplate, checkUserUrl, HttpMethod.POST, checkUserEntity);
                JSONObject checkUserData = checkUserBodyJson.getJSONObject("data");
                if (ObjectUtils.isEmpty(checkUserData.getJSONArray("rejectCertAuthList"))) {
                    log.info("身份验证失败:{}", checkUserBodyJson);
                }
                for (int j = 0; j < parkFsyyDetailDTOs.size(); j++) {
                    JSONObject parkFsyyDetailDTO = parkFsyyDetailDTOs.getJSONObject(j);
                    modelCodeTicketInfoMap.put("parkFsyyDetailDTO", parkFsyyDetailDTO);
                    headers.set("ts", String.valueOf(System.currentTimeMillis() / 1000));
                    String ts = String.valueOf(System.currentTimeMillis());
                    ts = ts.substring(0, 11);
                    String signStr = "VDsdxfwljhy#@!94857access-token=" + accessToken + ts + "AAXY";
                    String sign = DigestUtils.md5Hex(signStr);
                    JSONObject jsonObject = buildCreateParam(getUserJson, buildCheckUserParam());
                    log.info("创建订单入参：{}", jsonObject);
                    HttpEntity addTicketQueryEntity = new HttpEntity<>(buildCreateParam(getUserJson, buildCheckUserParam()), headers);
                    createUrl = String.format(createUrl, sign, ts);
                    JSONObject createRes = getResponse(restTemplate, createUrl, HttpMethod.POST, addTicketQueryEntity);
                    log.info("请求结果{}", createRes);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject buildCheckUserParam() {
        JSONObject param = new JSONObject();
        JSONObject normal = new JSONObject();
        JSONObject old = new JSONObject();
        JSONObject free = new JSONObject();
        //JSONObject student=new JSONObject();
        for (Map.Entry<String, String> nameIDMapEntry : iDNameMap.entrySet()) {
            String idCard = nameIDMapEntry.getKey();
            String name = nameIDMapEntry.getValue();
            Integer age = GetAgeForIdCardUtil.getAge(idCard);
            if (age >= 0 && age < 18) {
                free = buildItem(free, idCard, name, "free");
                continue;
            }
            if (age >= 60) {
                old = buildItem(old, idCard, name, "old");
                continue;
            }
            //学生后续优化
            normal = buildItem(normal, idCard, name, "normal");
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

    private JSONObject buildItem(JSONObject item, String idCard, String name, String type) {
        if (item.get("certAuthDTOS") == null) {
            item.put("certAuthDTOS", Arrays.asList(new HashMap() {{
                put("cardType", 0);
                put("certNo", idCard);
                put("name", name);
            }}));
        } else {
            List certAuthDTOS = item.getJSONArray("certAuthDTOS").toJavaList(Object.class);
            JSONObject certAuthDTO = new JSONObject();
            certAuthDTO.put("cardType", 0);
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

    private JSONObject buildCreateParam(JSONObject userInfo, JSONObject checkParam) {
        JSONObject param = new JSONObject();
        param.put("buyer", new HashMap<String, Object>() {{
            put("id", userInfo.getJSONObject("data").getString("userId"));
            put("openId", userInfo.getJSONObject("data").getString("openId"));
            put("mobile", userInfo.getJSONObject("data").getString("mobile"));
            put("credentialNo", credentialNo);
            put("credentialType", "0");
            put("nickName", nickName);
        }});
        param.put("couponCode", "");
        param.put("startDate", useDate);
        param.put("endDate", useDate);
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
            orderInfo.put("itemId", ticketInfoJson.getIntValue("itemId"));
            orderInfo.put("wayType", "6");
            orderInfo.put("fsName", parkFsyyDetailDTO.getString("fsTimeName"));
            JSONArray certAuthDTOS = ticketVerificationDTO.getJSONArray("certAuthDTOS");
            List certAuthList = new ArrayList();
            for (int j = 0; j < certAuthDTOS.size(); j++) {
                JSONObject certAuthDTO = certAuthDTOS.getJSONObject(j);
                certAuthList.add(new HashMap<String, Object>() {{
                    put("realName", certAuthDTO.getString("name"));
                    put("certType", 0);
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

    private JSONObject getResponse(RestTemplate restTemplate, String url, HttpMethod httpMethod, HttpEntity httpEntity) {
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

    public static void main(String[] args) throws UnsupportedEncodingException {
        String s = FileUtil.readString("/Users/devin.zhang/Desktop/record", Charset.defaultCharset());
        System.out.println(s);
    }
}
