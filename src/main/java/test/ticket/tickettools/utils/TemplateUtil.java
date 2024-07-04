package test.ticket.tickettools.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
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
    public static RestTemplate initSSLTemplateWithProxy(String proxyHost,Integer proxyPort){
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
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(10000)
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .setCircularRedirectsAllowed(true)
                .setExpectContinueEnabled(false)
                .setProxy(new HttpHost(proxyHost, proxyPort))
                .build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        ((HttpComponentsClientHttpRequestFactory) requestFactory).setHttpClient(HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setSSLContext(sslContext)
                .disableCookieManagement()
                .build());
        ((HttpComponentsClientHttpRequestFactory) requestFactory).setConnectTimeout(20000);
        ((HttpComponentsClientHttpRequestFactory) requestFactory).setReadTimeout(20000);
        return new RestTemplate(requestFactory);
    }
    public static RestTemplate initSSLTemplateWithProxyAuth(String proxyHost,Integer proxyPort){
        // 配置代理服务器地址和端口
        HttpHost proxy = new HttpHost(proxyHost, proxyPort);

        // 如果代理需要身份验证
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(proxyHost, proxyPort),
                new UsernamePasswordCredentials("VGZDIJ1W", "84395E70A086"));

        try {
            // 创建SSLContext，允许所有主机名，以便代理服务器可以解析HTTPS流量
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true)
                    .build();

            // 创建SSL连接套接字工厂，允许所有主机名，以便代理服务器可以解析HTTPS流量
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            // 创建HttpClient并设置代理、凭据提供者和SSL连接套接字工厂
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .setProxy(proxy)
                    .setSSLSocketFactory(socketFactory)
                    .build();

            // 创建HttpComponentsClientHttpRequestFactory并设置HttpClient
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

            // 使用工厂创建RestTemplate
            RestTemplate restTemplate = new RestTemplate(factory);
            return restTemplate;
        }catch (Exception e){
            e.printStackTrace();
        }
        return new RestTemplate();
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
