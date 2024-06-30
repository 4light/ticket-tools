package test.ticket.tickettools.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.taobao.api.ApiException;
import org.apache.commons.codec.binary.Base64;
import test.ticket.tickettools.domain.constant.ChannelEnum;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SendMessageUtil {
    private static String secretKey="SECe4602ef7a06ff23af115c8f8ba39fefa18d50290dd394fcdcd69ffc4ba5fee01";
    private static String robotToken="b8c66518ecacb83e0b26f58471d20a1f6a5da9ea4acc93362d259ceb70b2eaab";
    public static void send(String channel,String userDate,String session,String account,String user){
        try {
            Long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secretKey;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)),"UTF-8");
            //sign字段和timestamp字段必须拼接到请求URL上，否则会出现 310000 的错误信息
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/robot/send?sign="+sign+"&timestamp="+timestamp);
            OapiRobotSendRequest req = new OapiRobotSendRequest();
            /**
             * 发送文本消息
             */
            //设置消息类型
            //定义文本内容
            OapiRobotSendRequest.Markdown markdown = new OapiRobotSendRequest.Markdown();
            markdown.setTitle("任务执行结果通知");
            markdown.setText("### " + channel+"任务结果通知 \n"
                    +"#### 任务账号:"+account+"\n"
                    +"#### 日期:"+userDate+"\n"
                    +"#### 场次:"+session+"\n"
                    +"#### 游客姓名:"+user);
            //定义 @ 对象
            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
            at.setIsAtAll(true);
            //设置消息类型
            req.setMsgtype("markdown");
            req.setMarkdown(markdown);
            req.setAt(at);
            OapiRobotSendResponse rsp = client.execute(req, robotToken);
            System.out.println(rsp.getBody());
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
