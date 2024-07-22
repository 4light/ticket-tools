package test.ticket.tickettools.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.ObjectUtils;

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
