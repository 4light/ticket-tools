package test.ticket.tickettools.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.domain.bo.ProxyInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyUtil {

    //获取白名单列表
    private static String getWriteListUrl = "https://wapi.http.linkudp.com/index/index/white_list?neek=2430132&appkey=e7f7cd7830d1aff6f58ef5864c1e4e88";

    public static ProxyInfo getProxy() {
        try {
            //循环10次
            for (int i = 0; i < 10; i++) {
                Thread.sleep(2000);
                RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
                HttpHeaders proxyHeaders = new HttpHeaders();
                proxyHeaders.setContentType(MediaType.APPLICATION_JSON);
                JSONObject response = TemplateUtil.getResponse(restTemplate, "http://webapi.http.zhimacangku.com/getip?neek=d1fa042275328b9a&num=1&type=2&time=2&pro=0&city=0&yys=0&port=11&pack=0&ts=1&ys=0&cs=1&lb=1&sb=&pb=45&mr=3&regions=110000,130000,120000,210000,370000,140000,410000&username=chukou01&spec=1", HttpMethod.GET, new HttpEntity<>(proxyHeaders));
                if (response.getIntValue("code") != 0) {
                    System.out.println("获取代理异常:{}" + JSON.toJSONString(response));
                    //添加白名单
                    if (response.getIntValue("code") == 113) {
                        String msg = response.getString("msg");
                        String ipAddress = null;
                        Pattern pattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
                        Matcher matcher = pattern.matcher(msg);
                        if (matcher.find()) {
                            ipAddress = matcher.group();
                        }
                        //添加ip
                        if (!ObjectUtils.isEmpty(ipAddress)) {
                            //查询ip是否是五个
                            HttpHeaders headers = new HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_JSON);
                            JSONObject getWriteListRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getWriteListUrl, HttpMethod.GET, new HttpEntity(headers));
                            if (!ObjectUtils.isEmpty(getWriteListRes) && getWriteListRes.getIntValue("code") == 0) {
                                JSONObject data = getWriteListRes.getJSONObject("data");
                                JSONArray lists = data.getJSONArray("lists");
                                //小于5直接添加，大于5删除一个在添加
                                if (lists.size() > 4) {
                                    JSONObject last = lists.getJSONObject(0);
                                    String delWriteUrl = "https://wapi.http.linkudp.com/index/index/del_white?neek=2430132&appkey=e7f7cd7830d1aff6f58ef5864c1e4e88&white=" + last.getString("mark_ip");
                                    JSONObject delWriteUrlRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), delWriteUrl, HttpMethod.GET, new HttpEntity(headers));
                                    if (ObjectUtils.isEmpty(delWriteUrlRes) || delWriteUrlRes.getIntValue("code") != 0) {
                                        System.out.println("删除ip失败：" + JSON.toJSONString(delWriteUrlRes));
                                        continue;
                                    }
                                }
                                String addWriteUrl = "https://wapi.http.linkudp.com/index/index/save_white?neek=2430132&appkey=e7f7cd7830d1aff6f58ef5864c1e4e88&white=" + ipAddress;
                                JSONObject addWriteUrlRes = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), addWriteUrl, HttpMethod.GET, new HttpEntity(headers));
                                if (ObjectUtils.isEmpty(addWriteUrlRes) || addWriteUrlRes.getIntValue("code") != 0) {
                                    System.out.println("添加ip失败：" + JSON.toJSONString(addWriteUrlRes));
                                    continue;
                                }
                            }
                        }
                    }
                    continue;
                } else {
                    JSONArray data = response.getJSONArray("data");
                    if (ObjectUtils.isEmpty(data)) {
                        System.out.println("代理数据异常:{}" + JSON.toJSONString(data));
                        continue;
                    }
                    return new ProxyInfo(data.getJSONObject(0).getString("ip"),data.getJSONObject(0).getIntValue("port"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<ProxyInfo> getProxyList(Integer num){
        RestTemplate restTemplate = TemplateUtil.initSSLTemplate();
        HttpHeaders proxyHeaders = new HttpHeaders();
        proxyHeaders.setContentType(MediaType.APPLICATION_JSON);
        JSONObject response = TemplateUtil.getResponse(restTemplate, "https://share.proxy.qg.net/get?key=VGZDIJ1W&num="+num+"&area=110100,120100,130100,130200,130300,130400,130500,130600,130700,130800,130900,131000,131100&isp=0&format=json&distinct=true", HttpMethod.GET, new HttpEntity<>(proxyHeaders));
        List<ProxyInfo> list=new ArrayList<>();
        if (StrUtil.equals("SUCCESS",response.getString("code")) ) {
            JSONArray data = response.getJSONArray("data");
            for (int i = data.size() - 1; i >= 0; i--) {
                JSONObject item = data.getJSONObject(i);
                String proxy_ip = item.getString("proxy_ip");
                String server = item.getString("server");
                int port= Integer.parseInt(server.split(":")[1]);
                list.add(new ProxyInfo(proxy_ip,port));
            }
        }
        return list;
    }
}
