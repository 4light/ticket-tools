package test.ticket.tickettools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class TemplateUtil {
    public static RestTemplate initSSLTemplate(){
        SSLContext sslContext = null;
        try {
            sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        ((HttpComponentsClientHttpRequestFactory) requestFactory).setHttpClient(HttpClients.custom()
                .setSSLContext(sslContext)
                .disableCookieManagement()
                .build());
        ((HttpComponentsClientHttpRequestFactory) requestFactory).setConnectTimeout(20000);
        ((HttpComponentsClientHttpRequestFactory) requestFactory).setReadTimeout(20000);
        return new RestTemplate(requestFactory);
    }
    public static JSONObject getResponse(RestTemplate restTemplate, String url, HttpMethod httpMethod, HttpEntity httpEntity) {
        try {
            ResponseEntity<String> checkUserRes = restTemplate.exchange(url, httpMethod, httpEntity, String.class);
            String checkUserResBody = checkUserRes.getBody();
            if (StrUtil.isEmpty(checkUserResBody)) {
                System.out.println("获取数据失败:"+checkUserResBody);
                return null;
            }
            return JSON.parseObject(checkUserResBody);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
