package test.ticket.tickettools.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.ObjectUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VirtualPhoneUtil {
    //获取号码
    private static String token = "12979f4ebab7cfff2eb0f62b76d68ff47604e99eabcb6f67dea8b3086c90b8ded10015176f6ac8131c921eed962625876572a81c2e15f0b4a7b497091f5fb39cc4b014bc4cf8ec85b53f4e5042c53ddd";
    private static String getPhoneUrl = "https://api.haozhuma.com/sms/?api=getPhone&token=%s&sid=%s";
    //指定号码
    private static String designatedNumber="https://api.haozhuma.com/sms/?api=getPhone&token=%s&sid=%s&phone=%s";
    //获取验证码
    private static  String getVerCodeUrl="https://api.haozhuma.com/sms/?api=getMessage&token=%s&sid=%s&phone=%s";
    //科学技术馆
    private static Integer cstmCode = 67048;

    public static String getPhoneNo() {
        for (int i = 0; i < 5; i++) {
            String formatUrl = String.format(getPhoneUrl, token, cstmCode);
            HttpResponse execute = HttpUtil.createGet(formatUrl)
                    .timeout(60000)
                    .execute();
            String body = execute.body();
            System.out.println("获取到手机号结果："+body);
            if (!ObjectUtils.isEmpty(body)) {
                JSONObject response = JSON.parseObject(body);
                if (StrUtil.equals(response.getString("code"), "0")){
                    return response.getString("phone");
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static String getMiHouTaoPhoneNo() {
        for (int i = 0; i < 5; i++) {
            HttpResponse execute = HttpUtil.createGet("http://ergyiphd.hgsaeuyrgd.club:6747/takebysard?The=Api&acc=919306156@qq.com&apimiyao=9a121360b7b23a4c9d310e11f734bb67&apiid=67938601&yys=2")
                    .timeout(60000)
                    .execute();
            String body = execute.body();
            System.out.println("获取到手机号结果："+body);
            if (!ObjectUtils.isEmpty(body)) {
                JSONObject response = JSON.parseObject(body);
                if (response.getIntValue("code")==200&&response.getBoolean("stat")){
                    return response.getString("phone");
                }
            }
            try {
                Thread.sleep(2100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getVerificationCode(String phoneNum){
        String formatUrl = String.format(getVerCodeUrl, token, cstmCode,phoneNum);
        HttpResponse execute = HttpUtil.createGet(formatUrl)
                .timeout(60000)
                .execute();
        String body = execute.body();
        System.out.println("获取到手机验证码结果："+body);
        if (!ObjectUtils.isEmpty(body)) {
            JSONObject response = JSON.parseObject(body);
            if (StrUtil.equals(response.getString("code"), "0")){
                return response.getString("yzm");
            }
        }
        return null;
    }

    public static String getMiHouTaoVerificationCode(String phoneNum,String msg){
        String url="http://ergyiphd.hgsaeuyrgd.club:6747/ssbar?The=Api&acc=919306156@qq.com&apimiyao=9a121360b7b23a4c9d310e11f734bb67&phone=%s&message=%s";
        String formatUrl = String.format(url, phoneNum,msg);
        HttpResponse execute = HttpUtil.createGet(formatUrl)
                .timeout(60000)
                .execute();
        String body = execute.body();
        System.out.println("获取到手机验证码结果："+body);
        if (!ObjectUtils.isEmpty(body)) {
            JSONObject response = JSON.parseObject(body);
            if (response.getIntValue("code")==200&&response.getBoolean("stat")){
                String data = response.getString("data");
                String regex = "\\d{6}";
                // 创建Pattern对象
                Pattern pattern = Pattern.compile(regex);

                // 创建Matcher对象
                Matcher matcher = pattern.matcher(data);

                // 查找并提取验证码
                if (matcher.find()) {
                    String code = matcher.group();
                    return code;
                }
                return null;
            }
        }
        return null;
    }
    //占用手机号
    public static Boolean getDesignatedNumber(String phoneNum){
        String formatUrl = String.format(designatedNumber, token, cstmCode,phoneNum);
        HttpResponse execute = HttpUtil.createGet(formatUrl)
                .timeout(60000)
                .execute();
        String body = execute.body();
        System.out.println("占用手机号结果："+body);
        if (!ObjectUtils.isEmpty(body)) {
            JSONObject response = JSON.parseObject(body);
            if (StrUtil.equals(response.getString("code"), "0")){
                return true;
            }
        }
        return false;
    }



    public static void main(String[] args) {
        /*String phoneNo = getPhoneNo();
        System.out.println(phoneNo);
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String verificationCode = getVerificationCode(phoneNo);
            if(!ObjectUtils.isEmpty(verificationCode)){
                System.out.println();
                break;
            }

        }*/
        System.out.println(getPhoneNo());
    }
}
