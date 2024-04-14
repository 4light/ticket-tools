package test.ticket.tickettools;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.script.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static test.ticket.tickettools.ImageUtils.getPoint;

@Slf4j
@Configuration
@EnableScheduling
public class TicketSnatchingSchedule {

    //获取场次url
    private static String getScheduleUrl = "https://pcticket.cstm.org.cn/prod-api/pool/getScheduleByHallId?hallId=1&openPerson=1&queryDate=%s&saleMode=1&single=true";
    //获取场次下余票url
    private static String getPriceByScheduleIdUrl = "https://pcticket.cstm.org.cn/prod-api/pool/getPriceByScheduleId?hallId=1&openPerson=1&queryDate=%s&saleMode=1&scheduleId=";
    //添加人员url
    private static String addUrl = "https://pcticket.cstm.org.cn/prod-api/system/individualContact/add";
    //获取验证码图片
    private static String getCheckImagUrl = "https://pcticket.cstm.org.cn/prod-api/pool/getBlock";
    //提交订单
    private static String shoppingCartUrl = "https://pcticket.cstm.org.cn/prod-api/config/orderRule/shoppingCart";
    private static String getCurrentUserUrl="https://pcticket.cstm.org.cn/prod-api/getUserInfoToIndividual";
    private static String useDate = "2023-08-03 00:00:00";
    private String authorization = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6IjE4Y2JhZDZkLTk1YWEtNGQ4Zi1hMzgxLWQ3YWExM2VkNzBhNSJ9.8gDO15EhJHJ0-tpODHdcc-nzkDZq0ajbmO8zvvt9W9D59Nr3iY2yB8y_YCzKtyeO5jjU2TlvVJ96TXcpyhGGXQ";


    @Resource
    private TaskExecutorConfig taskExecutorConfig;

    private static Map<String, String> nameIDMap = new HashMap() {{
        //put("赵庆山", "320925200911043915");
        //put("王一鸣", "371312200812066930");
        //put("李思彤", "231024200710084722");
        //put("李珩源", "371302200912154011");
        //put("李国政", "371302201307154077");
    }};

    public static ScriptEngine engine;//脚本引擎

    static {
        ScriptEngineManager manager = new ScriptEngineManager();//脚本引擎管理
        engine = manager.getEngineByName("nashorn");//获取nashorn脚本引擎
        engine.getContext().getWriter();//获取正文并且写入
        getScheduleUrl=String.format(getScheduleUrl, DateUtil.format(DateUtil.parse(useDate),"yyyy/MM/dd"));
        getPriceByScheduleIdUrl=String.format(getPriceByScheduleIdUrl,DateUtil.format(DateUtil.parse(useDate),"yyyy/MM/dd"));
    }

    //@Scheduled(cron = "0/1 * * * * ?")
    public void run() {
        for (Map.Entry<String, String> entry : nameIDMap.entrySet()) {
            Map<String,String> newMap=new HashMap(){{
                put(entry.getKey(),entry.getValue());
            }};
            CompletableFuture.runAsync(() -> doSnatching(newMap), taskExecutorConfig.getAsyncExecutor());
        }
    }

    //@Scheduled(cron = "0/1 * * * * ?")
    public void doSnatching(Map<String,String> iDMap) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {
                {
                    setConnectTimeout(20000);
                    setReadTimeout(20000);
                }
            });
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authority", "pcticket.cstm.org.cn");
            headers.set("accept", "application/json");
            headers.set("authorization", authorization);
            headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            HttpEntity entity = new HttpEntity<>(headers);
            /*ResponseEntity getUserRes = restTemplate.exchange(getCurrentUserUrl, HttpMethod.GET, entity, String.class);
            String userInfoStr = getUserRes.getBody().toString();
            JSONObject userInfoJson = JSON.parseObject(userInfoStr);
            JSONObject userInfo = userInfoJson == null ? null : userInfoJson.getJSONObject("user");
            if (userInfo == null) {
                log.info("获取用户信息失败：{}", userInfoJson);
            }*/
            //long userId = userInfo.getLongValue("userId");
            long userId = 2077998;
            //String phone = userInfo.getString("phoneNumber");
            String phone = "18310327323";
            ResponseEntity response = restTemplate.exchange(getScheduleUrl, HttpMethod.GET, entity, String.class);
            Object body = response.getBody();
            //log.info("获取到的场次信息为:{}",body);
            JSONObject responseJson = JSON.parseObject(body.toString());
            Integer hallScheduleId = responseJson == null ? null : responseJson.getJSONArray("data").isEmpty() ? null : responseJson.getJSONArray("data").getJSONObject(0).getInteger("hallScheduleId");
            if (ObjectUtils.isEmpty(hallScheduleId)) {
                log.info("获取到的场次失败");
                return;
            }
            //获取场次下余票
            ResponseEntity getPriceByScheduleRes = restTemplate.exchange(getPriceByScheduleIdUrl + hallScheduleId, HttpMethod.GET, entity, String.class);
            JSONObject getPriceByScheduleJson = JSON.parseObject(getPriceByScheduleRes.getBody().toString());
            //log.info("获取到的场次下余票为:{}",getPriceByScheduleJson);
            //获取成人票和儿童票
            JSONArray getPriceByScheduleData = getPriceByScheduleJson == null ? null : getPriceByScheduleJson.getJSONArray("data");
            if (ObjectUtils.isEmpty(getPriceByScheduleData)) {
                log.info("获取到的场次失败");
                return;
            }
            boolean flag = true;
            int priceId = 0;
            int childrenPriceId = 0;
            int discountPriceId = 0;
            int olderPriceId = 0;
            Map<String, Integer> priceNameCountMap = new HashMap<>();
            for (String value : iDMap.values()) {
                int ageForIdcard = getAgeForIdcard(value);
                if (ageForIdcard > 0 && ageForIdcard <= 8) {
                    if (priceNameCountMap.containsKey("childrenTicket")) {
                        priceNameCountMap.put("childrenTicket", priceNameCountMap.get("childrenTicket") + 1);
                    } else {
                        priceNameCountMap.put("childrenTicket", 1);
                    }
                    continue;
                }
                if (ageForIdcard > 8 && ageForIdcard <= 18) {
                    if (priceNameCountMap.containsKey("discountTicket")) {
                        priceNameCountMap.put("discountTicket", priceNameCountMap.get("discountTicket") + 1);
                    } else {
                        priceNameCountMap.put("discountTicket", 1);
                    }
                    continue;
                }
                if (ageForIdcard >= 60 && ageForIdcard <= 199) {
                    if (priceNameCountMap.containsKey("olderTicket")) {
                        priceNameCountMap.put("olderTicket", priceNameCountMap.get("olderTicket") + 1);
                    } else {
                        priceNameCountMap.put("olderTicket", 1);
                    }
                    continue;
                }
                if (ageForIdcard >= 0 && ageForIdcard <= 100) {
                    if (priceNameCountMap.containsKey("normalTicket")) {
                        priceNameCountMap.put("normalTicket", priceNameCountMap.get("normalTicket") + 1);
                    } else {
                        priceNameCountMap.put("normalTicket", 1);
                    }
                }
            }
            int ticketPool= 0;
            int childrenTicketPool= 0;
            int discountTicketPool= 0;
            int olderTicketPool= 0;
            for (int i = 0; i < getPriceByScheduleData.size(); i++) {
                JSONObject obj=getPriceByScheduleData.getJSONObject(i);
                if ("普通票".equals(obj.getString("priceName")) || "儿童免费票".equals(obj.getString("priceName")) ||"优惠票".equals(obj.getString("priceName"))|| "老人免费票".equals(obj.getString("priceName"))){
                    if ("普通票".equals(obj.getString("priceName"))) {
                        ticketPool=obj.getIntValue("ticketPool");
                        priceId=obj.getInteger("priceId");
                    }
                }
                if ("儿童免费票".equals(obj.getString("priceName"))) {
                    childrenTicketPool=obj.getIntValue("ticketPool");
                    childrenPriceId=obj.getInteger("priceId");
                }
                if ("优惠票".equals(obj.getString("priceName"))) {
                    discountTicketPool=obj.getIntValue("ticketPool");
                    discountPriceId=obj.getInteger("priceId");
                }
                if ("老人免费票".equals(obj.getString("priceName"))) {
                    olderTicketPool=obj.getIntValue("ticketPool");
                    olderPriceId=obj.getInteger("priceId");
                }
            };
            for (Map.Entry<String, Integer> priceNameCountEntry : priceNameCountMap.entrySet()) {
                if("normalTicket".equals(priceNameCountEntry.getKey())){
                    flag = flag && ticketPool >= priceNameCountEntry.getValue();
                    if(flag) {
                        ticketPool=ticketPool - priceNameCountEntry.getValue();
                    }
                }
                if("childrenTicket".equals(priceNameCountEntry.getKey())){
                    flag = flag && childrenTicketPool >= priceNameCountEntry.getValue();
                    //如果余票不足看普票数量
                    if(!flag){
                        if((ticketPool-priceNameCountEntry.getValue())>=0){
                            ticketPool=ticketPool - priceNameCountEntry.getValue();
                            flag=true;
                        }
                    }
                }
                if("discountTicket".equals(priceNameCountEntry.getKey())){
                    flag = flag && discountTicketPool >= priceNameCountEntry.getValue();
                    //如果余票不足看普票数量
                    if(!flag){
                        if((ticketPool-priceNameCountEntry.getValue())>=0){
                            ticketPool=ticketPool - priceNameCountEntry.getValue();
                            flag=true;
                        }
                    }
                }
                if("olderTicket".equals(priceNameCountEntry.getKey())){
                    flag = flag && olderTicketPool >= priceNameCountEntry.getValue();
                    //如果余票不足看普票数量
                    if(!flag){
                        if((ticketPool-priceNameCountEntry.getValue())>=0){
                            ticketPool=ticketPool - priceNameCountEntry.getValue();
                            flag=true;
                        }
                    }
                }
            }
            //log.info("余票信息：{},{},{}",ticketPool,childrenTicketPool,olderTicketPool);
            //儿童票、老人票不足分配普票
            /*for (int i = 0; i < getPriceByScheduleData.size(); i++) {
                JSONObject item = getPriceByScheduleData.getJSONObject(i);
                if ("普通票".equals(item.getString("priceName")) || "儿童免费票".equals(item.getString("priceName")) || "老人免费票".equals(item.getString("priceName"))) {
                    //log.info("{}余票：{}", item.getString("priceName"), item.getIntValue("ticketPool"));
                    if ("普通票".equals(item.getString("priceName"))&&priceNameCountMap.get("normalTicket") != null) {
                        flag = flag && ticketPool >= priceNameCountMap.get("normalTicket");
                        if(flag) {
                            ticketPool=ticketPool - priceNameCountMap.get("normalTicket");
                        }

                    }
                    if ("儿童免费票".equals(item.getString("priceName"))&&priceNameCountMap.get("childrenTicket") != null) {
                        flag = flag && childrenTicketPool >= priceNameCountMap.get("childrenTicket");
                        //如果余票不足看普票数量
                        if(!flag){
                            if((ticketPool-priceNameCountMap.get("childrenTicket"))>priceNameCountMap.get("childrenTicket")){
                                ticketPool=ticketPool - priceNameCountMap.get("childrenTicket");
                                flag=true;
                            }
                        }
                    }
                    if ("老人免费票".equals(item.getString("priceName"))&&priceNameCountMap.get("olderTicket") != null) {
                        flag = flag && olderTicketPool >= priceNameCountMap.get("olderTicket");
                        //如果余票不足看普票数量
                        if(!flag){
                            if((ticketPool-priceNameCountMap.get("olderTicket"))>priceNameCountMap.get("olderTicket")){
                                ticketPool=ticketPool- priceNameCountMap.get("olderTicket");
                                flag=true;
                            }
                        }
                    }
                }
            }*/
            //余票充足
            if (flag) {
                //几个人添加几次
                for (Map.Entry<String, String> entry : iDMap.entrySet()) {
                    HttpEntity addEntity = new HttpEntity<>(buildAddParam(entry.getValue(), entry.getKey(), userId), headers);
                    restTemplate.exchange(addUrl, HttpMethod.POST, addEntity, String.class);
                }
                ResponseEntity<JSONObject> getCheckImagRes = restTemplate.exchange(getCheckImagUrl, HttpMethod.GET, entity, JSONObject.class);
                JSONObject getCheckImageJson = getCheckImagRes.getBody();
                if (!StringUtils.isEmpty(getCheckImageJson)) {
                    JSONObject data = getCheckImageJson.getJSONObject("data");
                    String jigsawImageBase64 = data == null ? null : data.getString("jigsawImageBase64");
                    String originalImageBase64 = data == null ? null : data.getString("originalImageBase64");
                    String secretKey = data == null ? null : data.getString("secretKey");
                    String token = data == null ? null : data.getString("token");
                    String imageUuid = UUID.randomUUID().toString();
                    String sliderImageName = "." + File.separator + imageUuid + "_" + "slider.png";
                    String backImageName = "." + File.separator + imageUuid + "_" + "back.png";
                    imagCreate(jigsawImageBase64, sliderImageName, 47);
                    imagCreate(originalImageBase64, backImageName, 310);
                    //图片验证码处理
                    Double x = getPoint(sliderImageName, backImageName, imageUuid);
                    //log.info("uuid的值为：{}", imageUuid);
                    //log.info("x的值为：{}", x);
                    String point = doSecretKey(x, secretKey);
                    HttpEntity shoppingCartUrlEntity = new HttpEntity<>(buildParam(token, priceNameCountMap.get("childrenTicket"), point, hallScheduleId, useDate, priceId, childrenPriceId, discountPriceId,olderPriceId, phone,iDMap), headers);
                    ResponseEntity<String> exchange = restTemplate.exchange(shoppingCartUrl, HttpMethod.POST, shoppingCartUrlEntity, String.class);
                    if(exchange.getBody().contains("\"code\":200")){
                        System.out.println(exchange.getBody());
                    }
                    try {
                        Files.delete(Paths.get(sliderImageName));
                        Files.delete(Paths.get(backImageName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * js加密
     * @param x
     * @param secretKey
     * @return
     */
    public String doSecretKey(Double x, String secretKey) {
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

    public static void main(String[] args) {
        imagCreate("iVBORw0KGgoAAAANSUhEUgAAAC8AAACbCAYAAADyfMLPAAASQElEQVR4Xu2YZ3RV55WGs2bGJbbBpgpkehdFQgih3nvvvaJeryTUe0W9dyGhAkJIIHqVwLQxsbEBOzMu47jEcezggkM8yf9n9pWDE3DiTLTWrOHHedY661ydc8+5797fbp9+9jMFBQUFBQUFBQUFBQUFBYUnG/4Oj3/viUMt8quvPuLBt59z//5nPPj9Pf70h2+ebCMeCvvjd/f503ffyPG1iP6a7779Lb//6mO++u07fP7xm0+eAWox3z24xx9F7H8/+JIH4vH7asFfvM83n7/Dl5/e5ssPr/P5u2d498bQk2WAWog6NL578Dvuf/kh3375Mfc+e5tvvvgPvvr1a/zq7jmmjnZw62wTv7rWzOtn6p8M8TPCv1N7/Hd8+9UHfPHpXf7w9Ud8/du3+PU75/nwzmnuXp/g8vFepkfLeO1YCVfGy58M76sFPLj/Ed/ce088/j7f3nufzz64JqKPc/N8Pzcv9PHWtVE+vH2UMwfKmehK5eLBAm4dzX0yxN/79A3u/+4dvvjkFvc+foPb18eZOtLI65dGuHaqlY9uH+Y/r3Zx5XAh50aKuTqaxRuTWU+G+JuXJ0T0bT577xo3p0e4eqKLo/uruf3KQV4/18WrZ9s5N5jDjfFcrhzIYqDCh7fOPSHif/PBG3zw1mWunRvk1qUhLhxp4tJku3i9meNDxRzqzmaoMZmR2jDqc13pqPBmoMr7/178zC/8A25dneDV6cOcGW/h8mQbo71F9DXncKA9h86aOI7tS2NfXQwlqe7UZHvQv9eDkiSrx1/zN3lcz/8a9cMxo560TxXxK2kw3977DR+/f4ur5w/Q2VzGnZtnuXNjksNDLRzaX0dNeTz7OwqpKoqitT6TlqpkSjKDKEx1o782nL054vGGMPbt9WKgPpb8FDEmx4Oh5gj+63o9146X8/bVdj56c4zrxypnb4T6gYRxX9Img5h8fYBPP32XD965wytTk5RXFrCvt5mpU6NMHe/h5OEWuttKyMmKJC3ZD1WyP7WVKWLEblorI+mq3k1XhS+N+Z4c6ogjK8GC9qoouqujOdqfwbWJPO6eL+QXp2tmjjfO1XPjZAN3LvUyNVY2O/Fpx0Opu5jNe5/c5d133mLy2Cgd7TVUVJUyONDK1QtHycyIJXNPOHm5saRnRLNHFUJlWRJ5mYGU5IWSsNuGxHArKjOdqczyoCDRgaJ0D8qzvRhqihVxeVyZKOTfJ8t5ZbyCo91SSg+VcGeqg0vjlfzylc7Zia++sIdXfnmCTz55n7HJMUr2VlGxt5zO1joamypori8jXRVDdEwA8UkhpIjHw4PtyM/0IybcltAQJypL4oiQaxlxjlRk2pMQYTkjuqnIl/piWdVeFYNNMQw1RDN1KJ+htlRODufNdOPrp5u4M90xO/Hjt/q5+95dLl2+SEVdDaW1deSVFFFWmk1RSSY5ecn4h3jj4+dEeKQf/v6W+PpYEOxnQkyUM0GBNsRHu1CU6U1Oihtp8fZimC/NpRFkywr01kTQXhZEW4WsVo4P4/tymTpcyZH+LM4M5/OLs23S2IpmJ37iziA3bt+gc6CX7OIi0nIzSc/NIjUrnYCIQFy8bHH1tMLO0YzdMX44Ou0kONQBD09zfLzNiI1zlbMhsZFmVIqn22oi6W1MorMqnP2tcexr2E1MoBEVub4c7s+mvSaR4VYVF8dKOdypkvuJksxpsxM/fusA569fo0Q8npSZQZwqibCYMCJiI3Byc8TCzhIrBwvsnM2xcTTEwmoLXr7GBARaERFpR5Z4efduBxITHMhNcyQ93oyG0kBiwiwpynCmpTyQ5GgnOhtVUlIjpbymUVMYxkBjKtUFUYx05zPWM4sxQv3AyZvTdA6PkFZQiE9YIAG7vxfu6u2Io4cDPiFeBEb6Y+tkLMK34+BmiIuHMfYOO3FwMiDI3wI/PzOCguyIDLMhPdlJjHAnLs4LT2cdGkvC6K5NpLNOJdUngZ7aJLqbVTRVJNJRq+LqqXqODZXMTvzEhSmSc/OJTEomOWcPoQm78Qr0wMbZFGvxtrO3DS7etviF2OLta4JPoDXOPpY4uovwUDu8vE0JDrYiwMeIuFgRHetAWqrnn6uQE6pYF8kFD0pzIhjoKKKrQUKlNV2EpzLcns30kSqmJ4pnJ76pbwRVQTGxaSmERAXjHeSLu58rDh424mUR6mGOlaMezp4GWNluw9puB/Yuxuwy3YS7CHd2N5ccsMPWZiuhcvbzMyIkxIyk3baU5QfTJY0qTsImR+VFQbo/pbnBlOYFS3OLYvpotSRvKX2NUTPi/xaPa/4B9c2yxh6iVcm4+Phh42Iv3rbB3HYXZjb6WDsaYW6njbHFepw8d2LvZsAOgw1Y2OiI5y1wcN2Fq489rh6muHvswsfHDEuLLVJS3QmR1chMdWVPsqxGlBNhQZakxrvTUZfCwa5sMSCco4N54v102qplvJCh7lBnOtdPVLG/KZEj+zJ/2gj1xby9jQRFReLq64NHkP8jD9i5GmFktZUdu1aib7QGI7N16O1ah4GpFk6ukgM22rh5W2Fjpycrsk0q0VYpnWYESzJ7uuni4a5LcoIzoYHmhEhOJEoeNInQzrokosNsaSqThO3YQ2dDKseGC2ivDJdETqC20JeJviz6G5Lo/3MlUvMj8TlVNbgH+v3oCz9cEIwsN0gObGSn8SoxYhUmVtswMdcRY7RwcNklhz4usipBgfp4umtLCG3CVozx8jLG20Mfd2d96QWulBVFSDJ7UV8unTpRBrdsf7qlrNaXxjLek8WBDhWVuSHUFQfM9IfmykTOjddy9XjDD1oeEf+ji4/x8L6h5UoMTFZgaL6aHUZrsXHSZZfReon9rRgZrpnxvInpOoyMV2NmshJX521YWWrNGBEaYkGAVCVVihdhgRYzY0VpYbgYEU1/awYnR7IZ68umR5K5viyCnsYYbpyqoa9ljxiUSV9THEd6039S599F/ZC53SaMRLiF/QY2bdUQ0bISBrIShuvQN1jDVp1l6Om9jLEItzBdgc7mhRgYb5bObIKHeN9PxKsbXHKqDy5O28nN8icrzZfcdHfqK8Jp3Rstno6hujBI8iBVqlClJHsSXRJiham+9NWnPPTjP2eA+gFbp03i9ZXoGWqipb0EQ7MNGJqux0SS2dhsEzt2rkRHRwN9/ZfR19PAYOfLMytha7sFQ4PVuEppDYtwIDjIVq5vxdPTmPKSWFQSPi0S86mJrmSmB9LdlMD+5hSpQPFSXgOoKJBQk+tNe5MY686ZnXh1yJjabcDMfr2EzTJMrNehLUbo7Fgm3hdP6y5h3ZoX2L5tnhigibHpKnboqQ3YMFOV7MXrSakepKp8CYv0khWxlw5tT1KiIyopoVGxHjJuB8pc5E1Wqgx+2X5ikJscnhRJbnTVxTEifWFW4s1tN2Al3rd03CgJvBZzh01obdPA1HoD2roabNNditbmeWzRmsN2vYWYmS/D3HojJkbrsLZW9wdt6ci7iBDvh4Y7YmW9Gb9gE3z9d0lImZKi8iZZ5UZKvAux0tjiou2lxNoRE+dBSqIXxVnesmv7vnw+ru8nUT+gTlRn780S85KUVlIujdew02wNBmbL0d6+EF29BZhZLkVXdwFbtefNnM2s1d9bxy6DFZhImPn4W2PvvF2GPQMCJIm9AkwIkbE6QpqYb4AZoRH28rcjccle0vAkyf1MZfDzFO8HkBjjRkfN93H/iLB/ZI36vpWjtowFupjabsLBU1cGNlkFpy0YmK9g+46FbN8+Dx0xQmf7AklgDQknDYxM1mJqsQZnt23Y2G+RjmyEv4hy95Km5mMqBljiL30hTsaHWKlEMdLEohL8SM4MlTHcmoBgC8LDzQkPMpZm58q+tr+KefWH/L3VD/X/5cZfiVYTm5lITEYcfjKrOMkMYyll0tJBCwsxxERWQUd7Adt05kv4zJeknSeeXspmrRcliRdj77geK5v1ePmbExHnJuLNZWr1ICLeh+AwR0IiXWTw8yQkWnIhxouopEACw2TMDnIgKMKJwGBbaXoWFGQ+9p8I9R/ZpYX4RoYSGPtod1Wzp6qWgoaWmUNVUk5CTgbB8aG4ygbEwUsPIwspl4Yr0dWXcNmxmI2bXmTLtvkzn3XF81vl8w45m1ptlE2M1UxDC5Y4jk4JJlYVTbwqipTcdPKqyimorhJHVqDKS5V9hJ9MtgGyIqH4SbeOT3AlXVZGzaPiC9MIjY8kLjuKsJRIsmrqqRkYp+nASdoPn6X78AkaBkap7t5PaUs7FZ0tJOYn4xniiq2bvghbL+PCGjZozWfjRvG+zgLx/BI5NNDWW4ap5TqsJFn9xJPBkd4yeqdQXFtCVUs9Lft6GDp6gvGzrzA8eZ7m/gMU1zWIMWUkZCQRmxyOs6shYTLRPiL8ofis/HhiUhNIFItTSwqpH5xg4OQNDp57lUPnf8HAsSlahsZpGz5I6+AAdb1dFNZXkVGWh2eALbaO0knttKRcLpfYXyTjw2LxtsS9hMxmbU0srA3YHa3eI/izpySLxt5WBscPM3RkksmzF+WY5tjZq1y8/hbTN3/JmSu3GDk+TVV7D2l5fxnOHhH+UHxJRQZRydFkVVeKxw8wcu4aE5duceTSm4xfeI3h4xfonzhG39gR6nv7qOnqoqK5iZLGRuLSYiSet2JqLolp9n3ibtk6h81bXmS77iLZbfnS3tZHRLiP7NLiaOnrmRE+duIk48dPM3nmIqcvXmHqymtcvfn2D0L/msc1/4D6ZlltjiRjMo2DwwydvsTY1KuMXbzJ6NkrHDw1ReeBcdlpjdHaP0xdZwfljXU093RS39FGSX257AHcMVWXTb3FUteXo7dzjjSoFZJwNlRISAZ52xER40ttay0DYwfoHdzP0KERhkaHGB47zOTp81y8cuOnhf4t1A8UVeWTX9/E4KkrHLn8unj91RnRvaNjssQd1HW3UdPeQW17K+37e6jraKKxrYG61kpq20oprU4nNcODoDB93Dw3SYxrYma1TMbkNdjZbyM63pOymnxaexrpG+ymd383+4a66T/Qz8DoMIMHDjIxeXx24rPLC2gemWBi+jXGp8Tj5y7RNbSfvc2VFFVkkVOUQnZZGtnFyeSWJFJZn0NjewV1LYXUteVT25JLuexRC8rCiIwyIzB8Jx4B2vj66ZOa5kP53jQaxMiWzr2099TNnJtaK6hvKhUnVIkxkrRixKzEZ5SV03rwmHhcvD59i8Ejp+Xl1eTLnjPYUx9b880YGKxHT3c1WluWSRfdjLeXLulpjtTI8NQg+9HqxgyKZMjKKfRHle1KWq4/mXkRFBZHsbc2neauMhFdTENdMgV5QezJDJLmI/U7QJpQgB1F+YmzE1/W1s349Bscu/b2jOf3jRygNF9FtLsxjtpL2bZqHquXvYTmkgUs0VzCvDlPs3jRMjQXL0RfZzFZGbbUy/xdXBgsg1UQheVxFJTGkCUjb2FuIDX16dQ3pJC/xwkXqzWsXzEHzUXPsmzJXFZozmPz2kXYGm2cnfjiln0Mn7nGsatvMnHxGp39fZQWpKMKdcB1hyTgek2WL13A4oXP8fKSF3h56UusXreJJRrzmfvsv7Bx+QvkJJvL5OdITpob2bm7yc6KJCfdjz3xdpTk+pCRYIH22jnMffpfmf/CU+IILZYt386KlVpoasqIvWbh7MQ3jZ6YqTKHLlzm4NkL9B0coq6mnHzpfr5msvnesoYVC+eycO7TrJSRd9WGOSxf83OWvvxzNOY/y9xn/g1z7fmkBm4hNdyQ/NxoCgv2kBbtQlqoCcl+8g4Znec9/zQaLz3L6pUvsXazLktF9FKNuaxbv5JVqzRnJ7528CD7Tp5l9MJFhk6covfgMK3tdeLJFIKdTXDasQKjDUtZ8uILLFn4DEuXPs2iRRI6859h3nPq83NorZxDmMz6Sb6ywSjOoba2lrQoTxJ9DPEwlHzRWo3m/OdZNO8pNm5bwOrNc1i6XIyR9yxfPk9WVGN24tW0Hh5l6MwJRk6fZGBiTEpaD5WleSSFuuAuyeokGw5z7fWsXDCP+c8/I0v/LAuee4YFL8iPazzPMhHmabKRxEBLKspLaGhsIW23L5EO29mxejlay5fI6v1cvP8UGgufRVPjGVk1ccKLT81c27Z+7T8vXs1DA/6/eVyXgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCwpPK/wDrgXZzKWkQzAAAAABJRU5ErkJggg==","./slider.png",50);
        Double point = getPoint("/Users/devin.zhang/Downloads/back.png", "./slider.png", UUID.randomUUID().toString());
        System.out.println(point);
    }

    /**
     * 构建参数
     * @param captchaToken
     * @param childTicketNum
     * @param point
     * @param hallScheduleId
     * @param useDate
     * @param priceId
     * @param childrenPriceId
     * @param olderTicketPriceId
     * @param phone
     * @return
     */
    public Object buildParam(String captchaToken, Integer childTicketNum,String point,Integer hallScheduleId, String useDate,Integer priceId, Integer childrenPriceId,Integer discountPriceId,Integer olderTicketPriceId,String phone,Map<String,String> iDMap) {
        JSONObject param = new JSONObject();
        param.put("captchaToken",captchaToken);
        param.put("childTicketNum",childTicketNum);
        param.put("date", useDate);
        param.put("phone",phone);
        param.put("platform",1);
        param.put("pointJson",point);
        param.put("poolFlag",1);
        param.put("realNameFlag",1);
        param.put("saleMode",1);
        param.put("ticketNum",iDMap.size());
        param.put("useTicketType",1);
        List ticketInfoList=new ArrayList();
        for (Map.Entry<String, String> entry : iDMap.entrySet()) {
            int ageForIdCard=getAgeForIdcard(entry.getValue());
            JSONObject ticketInfo = new JSONObject();
            ticketInfo.put("certificate",1);
            ticketInfo.put("certificateInfo",entry.getValue());
            ticketInfo.put("cinemaFlag",0);
            ticketInfo.put("hallId",1);
            ticketInfo.put("hallScheduleId",hallScheduleId);
            if(ageForIdCard>0&&ageForIdCard<=8){
                ticketInfo.put("isChildFreeTicket",1);
            }else{
                ticketInfo.put("isChildFreeTicket",0);
            }
            ticketInfo.put("platform",1);
            ticketInfo.put("realNameFlag",1);
            ticketInfo.put("saleMode",1);
            ticketInfo.put("status",0);
            if(ageForIdCard>=0&&ageForIdCard<=100){
                ticketInfo.put("ticketPriceId",priceId);
            }
            if(ageForIdCard>0&&ageForIdCard<=8){
                ticketInfo.put("ticketPriceId",childrenPriceId);
            }
            if(ageForIdCard>8&&ageForIdCard<=18){
                ticketInfo.put("ticketPriceId",discountPriceId);
            }
            if(ageForIdCard>=60&&ageForIdCard<=199){
                ticketInfo.put("ticketPriceId",olderTicketPriceId);
            }
            ticketInfo.put("useDate",useDate);
            ticketInfo.put("userName",entry.getKey());
            ticketInfoList.add(ticketInfo);
        }
        param.put("ticketInfoList",ticketInfoList);
        return param;
    }

    /**
     * base64转图片
     * @param base64String
     * @param imagePath
     * @param width
     */
    public static void imagCreate(String base64String, String imagePath, Integer width) {
        try {
            // 将 Base64 字符串解码为字节数组
            byte[] imageBytes = Base64.getDecoder().decode(base64String);
            // 创建 ByteArrayInputStream 以读取字节数组
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            // 使用 ImageIO 读取 ByteArrayInputStream 中的图像数据
            BufferedImage originalImage = ImageIO.read(bis);
            // 指定所需的宽度和高度
            int desiredWidth = width;
            int desiredHeight = 155;
            // 创建调整后尺寸的图像
            BufferedImage adjustedImage = new BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = adjustedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, desiredWidth, desiredHeight, null);
            g.dispose();
            // 将图像保存到文件中
            File outputImage = new File(imagePath);
            ImageIO.write(adjustedImage, "png", outputImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 通过身份证获取年龄
     *
     * @param idcard
     * @return
     */
    public int getAgeForIdcard(String idcard) {
        try {
            int age = 0;
            if (StringUtils.isEmpty(idcard)) {
                return age;
            }

            String birth = "";
            if (idcard.length() == 18) {
                birth = idcard.substring(6, 14);
            } else if (idcard.length() == 15) {
                birth = "19" + idcard.substring(6, 12);
            }

            int year = Integer.valueOf(birth.substring(0, 4));
            int month = Integer.valueOf(birth.substring(4, 6));
            int day = Integer.valueOf(birth.substring(6));
            Calendar cal = Calendar.getInstance();
            age = cal.get(Calendar.YEAR) - year;
            //周岁计算
            if (cal.get(Calendar.MONTH) < (month - 1) || (cal.get(Calendar.MONTH) == (month - 1) && cal.get(Calendar.DATE) < day)) {
                age--;
            }
            return age;
        } catch (Exception e) {
            e.getMessage();
        }
        return -1;
    }


    /**
     * 构建添加人员入参
     * @param certificateNumber
     * @param name
     * @param userId
     * @return
     */
    private JSONObject buildAddParam(String certificateNumber, String name, Long userId) {
        JSONObject param = new JSONObject();
        param.put("certificateNumber", certificateNumber);
        param.put("certificateType", 1);
        param.put("isShowError", "N");
        param.put("name", name);
        param.put("userId", userId);
        return param;
    }
}
