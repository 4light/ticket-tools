package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import test.ticket.tickettools.dao.AccountInfoDao;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.UserEntity;
import test.ticket.tickettools.service.AccountService;
import test.ticket.tickettools.service.UserService;
import test.ticket.tickettools.utils.TemplateUtil;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    UserService userServiceImpl;

    @Resource
    AccountInfoDao accountInfoDao;
    private String getPlaceMuUserInfoUrl = "https://lotswap.dpm.org.cn/lotsapi/leaguer/api/userLeaguer/manage/leaguerInfo?cipherText=0&merchantId=2655&merchantInfoId=2655";
    private String getChnMuUserInfoUrl = "https://uu.chnmuseum.cn/prod-api/getUserInfoToIndividual2Mini?p=wxmini";
    private static List<String> accessTokenList = new ArrayList<>();

    @Override
    public ServiceResponse queryAccount(AccountInfoRequest accountInfoRequest) {
        UserEntity userEntity = userServiceImpl.selectByUserName(accountInfoRequest.getCreator());
        AccountInfoEntity accountInfoEntity = new AccountInfoEntity();
        if (!StrUtil.equals("admin", userEntity.getRole())) {
            accountInfoEntity.setBelongUser(userEntity.getId());
            accountInfoEntity.setCreator(userEntity.getUserName());
        }
        accountInfoEntity.setAccount(accountInfoRequest.getAccount());
        accountInfoEntity.setChannel(accountInfoRequest.getChannel());
        accountInfoEntity.setUserName(accountInfoRequest.getUserName());
        List<AccountInfoEntity> select = accountInfoDao.selectList(accountInfoEntity);
        if (ObjectUtils.isEmpty(accountInfoRequest.getPage())) {
            return ServiceResponse.createBySuccess(select);
        }
        return ServiceResponse.createBySuccess(PageableResponse.listCovPageInfo(accountInfoRequest.getPage(), select));
    }

    @Override
    public ServiceResponse addAccount(AccountInfoRequest accountInfoRequest) {
        AccountInfoEntity userInfo = new AccountInfoEntity();
        userInfo.setChannel(accountInfoRequest.getChannel());
        userInfo.setUserName(accountInfoRequest.getUserName());
        userInfo.setAccount(accountInfoRequest.getAccount());
        userInfo.setCreator(accountInfoRequest.getCreator());
        userInfo.setPwd(accountInfoRequest.getPwd());
        userInfo.setStatus(false);
        Integer insert = accountInfoDao.insert(userInfo);
        if (insert == null) {
            return ServiceResponse.createByErrorMessage("插入数据失败");
        }
        return ServiceResponse.createBySuccess();
    }

    @Override
    public ServiceResponse addProxyAccount(ProxyAccountInfoRequest proxyAccountInfoRequest) {
        //获取用户信息
        HttpHeaders headers = new HttpHeaders();
        String proxyHeadersStr = proxyAccountInfoRequest.getHeaders();
        JSONObject proxyHeadersJson = JSON.parseObject(proxyHeadersStr);
        String access = proxyHeadersJson.getString("access-token");
        if (accessTokenList.contains(access)) {
            log.info("存在相同的accessToken直接返回");
            return ServiceResponse.createBySuccess();
        }
        accessTokenList.add(proxyHeadersJson.getString("access-token"));
        proxyHeadersJson.entrySet().forEach(o -> {
            if (StrUtil.equals(o.getKey(), "Accept-Encoding")) {
                headers.set("Accept-Encoding", "gzip, deflate");
            } else {
                headers.set(o.getKey(), o.getValue().toString());
            }
        });
        HttpEntity entity = new HttpEntity(headers);
        String url = proxyAccountInfoRequest.getUrl();
        AccountInfoEntity accountInfoEntity = new AccountInfoEntity();
        JSONObject data = new JSONObject();
        //故宫的
        if (url.contains("lotswap")) {
            JSONObject response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getPlaceMuUserInfoUrl, HttpMethod.GET, entity);
            if (response == null || response.getIntValue("status") != 200) {
                log.info("请求用户信息异常：{}", response);
                accessTokenList.remove(access);
                return ServiceResponse.createByErrorMessage("获取用户信息异常");
            }
            log.info("获取到的用户信息:{}", response);
            data = response.getJSONObject("data");
            proxyHeadersJson.put("mpOpenId", data.getString("openId"));
            accountInfoEntity.setChannel(ChannelEnum.LOTS.getCode());
            String account = data.getString("mobile") == null ? data.getString("email") : data.getString("mobile");
            accountInfoEntity.setAccount(account);
        }
        //国博的
        if (url.contains("chnmuseum")) {
            headers.set("Host", "uu.chnmuseum.cn");
            HttpEntity chnMuEntity = new HttpEntity(headers);
            JSONObject response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getChnMuUserInfoUrl, HttpMethod.GET, chnMuEntity);
            if (response == null || response.getIntValue("code") != 200) {
                accessTokenList.remove(access);
                log.info("请求用户信息异常：{}", response);
                return ServiceResponse.createByErrorMessage("获取用户信息异常");
            }
            log.info("获取到的用户信息:{}", response);
            data = response.getJSONObject("user");
            accountInfoEntity.setChannel(ChannelEnum.CHNMU.getCode());
            String account = data.getString("phoneNumber") == null ? data.getString("email") : data.getString("phoneNumber");
            accountInfoEntity.setAccount(account);
        }
        List<AccountInfoEntity> res = accountInfoDao.selectList(accountInfoEntity);
        if (res.size() > 0) {
            //更新
            JSONObject finalData = data;
            res.forEach(o -> {
                o.setChannelUserId(finalData.getString("userId"));
                o.setHeaders(proxyHeadersStr);
                o.setUpdateDate(new Date());
                accountInfoDao.insertOrUpdate(o);
            });
            return ServiceResponse.createBySuccess();
        } else {
            accountInfoEntity.setHeaders(proxyHeadersStr);
            //先用来存储openId,如果openId没有存国博的
            accountInfoEntity.setExt(data.getString("openId"));
            Integer insert = accountInfoDao.insert(accountInfoEntity);
            if (insert != 0) {
                return ServiceResponse.createBySuccess();
            }
            return ServiceResponse.createByErrorMessage("保存失败");
        }
    }

    @Override
    public ServiceResponse delAccount(Long id) {
        Integer del = accountInfoDao.del(id);
        if (del == null) {
            return ServiceResponse.createByErrorMessage("删除数据失败");
        }
        return ServiceResponse.createBySuccess();
    }

    @Override
    public ServiceResponse updateAccount(AccountInfoEntity accountInfoEntity) {
        if (ObjectUtils.isEmpty(accountInfoEntity.getId())) {
            accountInfoEntity.setStatus(false);
            accountInfoEntity.setCreateDate(new Date());
            accountInfoEntity.setCreator(accountInfoEntity.getCreator());
        } else {
            accountInfoEntity.setUpdateDate(new Date());
            accountInfoEntity.setOperator(accountInfoEntity.getCreator());
        }
        Integer integer = accountInfoDao.insertOrUpdate(accountInfoEntity);
        if (integer == null) {
            return ServiceResponse.createByErrorMessage("更新数据失败");
        }
        return ServiceResponse.createBySuccess();
    }

    @Override
    public ServiceResponse<List<AccountCheckResponse>> accountCheckAuth(String currentUser) {
        List<AccountCheckResponse> result = new ArrayList<>();
        UserEntity userEntity = userServiceImpl.selectByUserName(currentUser);
        AccountInfoEntity accountInfoEntity = new AccountInfoEntity();
        if (!StrUtil.equals("admin", userEntity.getRole())) {
            accountInfoEntity.setBelongUser(userEntity.getId());
            accountInfoEntity.setCreator(userEntity.getUserName());
        }
        List<AccountInfoEntity> accountInfoEntityList = accountInfoDao.selectList(accountInfoEntity);
        RestTemplate restTemplate = TemplateUtil.initSSLTemplate();

        for (AccountInfoEntity item : accountInfoEntityList) {
            AccountCheckResponse accountCheckResponse = new AccountCheckResponse();
            try {
                Integer channel = item.getChannel();
                String headerStr = item.getHeaders();
                if (ObjectUtils.isEmpty(channel) || ObjectUtils.isEmpty(headerStr)) {
                    accountCheckResponse.setAccountId(item.getId());
                    accountCheckResponse.setCheckRes(false);
                    accountCheckResponse.setMsg("没有设置渠道或数据没有头信息");
                    result.add(accountCheckResponse);
                    continue;
                }
                HttpHeaders headers = new HttpHeaders();
                JSONObject response = null;
                //科技馆
                if (channel == 0) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("authority", "pcticket.cstm.org.cn");
                    headers.set("accept", "application/json");
                    headers.set("authorization", headerStr);
                    headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
                    HttpEntity entity = new HttpEntity<>(headers);
                    //获取场次下余票
                    ResponseEntity getUserInfoRes = restTemplate.exchange(ChannelEnum.CSTM.getBaseUrl() + "/prod-api/getUserInfoToIndividual", HttpMethod.GET, entity, String.class);
                    response = JSON.parseObject(getUserInfoRes.getBody().toString());
                }
                //故宫
                if (channel == 2) {
                    JSONObject headerJson = JSON.parseObject(headerStr);
                    for (Map.Entry<String, Object> headerEntry : headerJson.entrySet()) {
                        headers.set(headerEntry.getKey(), headerEntry.getValue().toString());
                    }
                    HttpEntity entity = new HttpEntity<>(headers);
                    response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getPlaceMuUserInfoUrl, HttpMethod.GET, entity);
                }
                if (channel == 3) {
                    HttpEntity chnMuEntity = new HttpEntity(headers);
                    response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getChnMuUserInfoUrl, HttpMethod.GET, chnMuEntity);
                }
                if (response != null && response.getIntValue("code") == 200) {
                    accountCheckResponse.setAccountId(item.getId());
                    accountCheckResponse.setCheckRes(true);
                    accountCheckResponse.setMsg(channel == 0 ? response.getString("msg") : response.getString("data"));
                } else {
                    accountCheckResponse.setAccountId(item.getId());
                    accountCheckResponse.setCheckRes(false);
                    accountCheckResponse.setMsg(response.getString("data"));
                }
                result.add(accountCheckResponse);
            } catch (Exception e) {
                accountCheckResponse.setAccountId(item.getId());
                accountCheckResponse.setCheckRes(false);
                accountCheckResponse.setMsg("检查出现异常");
                e.printStackTrace();
            }
        }
        return ServiceResponse.createBySuccess(result);
    }
}
