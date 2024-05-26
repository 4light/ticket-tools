package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.dao.AccountInfoDao;
import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.ProxyAccountInfoRequest;
import test.ticket.tickettools.domain.bo.AccountInfoRequest;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.service.AccountService;
import test.ticket.tickettools.utils.TemplateUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    AccountInfoDao accountInfoDao;
    private String getPlaceMuUserInfoUrl ="https://lotswap.dpm.org.cn/lotsapi/leaguer/api/userLeaguer/manage/leaguerInfo?cipherText=0&merchantId=2655&merchantInfoId=2655";
    private String getChnMuUserInfoUrl ="https://uu.chnmuseum.cn/prod-api/getUserInfoToIndividual2Mini?p=wxmini";
    private static List<String> accessTokenList=new ArrayList<>();
    @Override
    public ServiceResponse queryAccount(AccountInfoRequest accountInfoRequest) {
        AccountInfoEntity accountInfoEntity = new AccountInfoEntity();
        accountInfoEntity.setUserName(accountInfoRequest.getUserName());
        accountInfoEntity.setAccount(accountInfoRequest.getAccount());
        accountInfoEntity.setChannel(accountInfoRequest.getChannel());
        List<AccountInfoEntity> select = accountInfoDao.select(accountInfoEntity);
        if(ObjectUtils.isEmpty(accountInfoRequest.getPage())){
            return ServiceResponse.createBySuccess(select);
        }
        return ServiceResponse.createBySuccess(PageableResponse.listCovPageInfo(accountInfoRequest.getPage(),select));
    }

    @Override
    public ServiceResponse addAccount(AccountInfoRequest accountInfoRequest) {
        AccountInfoEntity userInfo=new AccountInfoEntity();
        userInfo.setChannel(accountInfoRequest.getChannel());
        userInfo.setUserName(accountInfoRequest.getUserName());
        userInfo.setAccount(accountInfoRequest.getAccount());
        userInfo.setPwd(accountInfoRequest.getPwd());
        userInfo.setStatus(false);
        Integer insert = accountInfoDao.insert(userInfo);
        if(insert==null){
            return ServiceResponse.createByErrorMessage("插入数据失败");
        }
        return ServiceResponse.createBySuccess();
    }

    @Override
    public ServiceResponse addProxyAccount(ProxyAccountInfoRequest proxyAccountInfoRequest) {
        //获取用户信息
        HttpHeaders headers=new HttpHeaders();
        String proxyHeadersStr = proxyAccountInfoRequest.getHeaders();
        JSONObject proxyHeadersJson = JSON.parseObject(proxyHeadersStr);
        String access = proxyHeadersJson.getString("access-token");
        if(accessTokenList.contains(access)){
            log.info("存在相同的accessToken直接返回");
            return ServiceResponse.createBySuccess();
        }
        accessTokenList.add(proxyHeadersJson.getString("access-token"));
        proxyHeadersJson.entrySet().forEach(o->{
            if(StrUtil.equals(o.getKey(),"Accept-Encoding")){
                headers.set("Accept-Encoding","gzip, deflate");
            }else{
                headers.set(o.getKey(),o.getValue().toString());
            }
        });
        HttpEntity entity=new HttpEntity(headers);
        String url = proxyAccountInfoRequest.getUrl();
        AccountInfoEntity accountInfoEntity = new AccountInfoEntity();
        JSONObject data=new JSONObject();
        //故宫的
        if(url.contains("lotswap")) {
            JSONObject response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getPlaceMuUserInfoUrl, HttpMethod.GET, entity);
            if (response == null || response.getIntValue("status") != 200) {
                log.info("请求用户信息异常：{}", response);
                accessTokenList.remove(access);
                return ServiceResponse.createByErrorMessage("获取用户信息异常");
            }
            log.info("获取到的用户信息:{}", response);
            data = response.getJSONObject("data");
            accountInfoEntity.setChannel(ChannelEnum.LOTS.getCode());
            String account = data.getString("mobile") == null ? data.getString("email") : data.getString("mobile");
            accountInfoEntity.setAccount(account);
        }
        //国博的
        if(url.contains("chnmuseum")){
            headers.set("Host","uu.chnmuseum.cn");
            HttpEntity chnMuEntity=new HttpEntity(headers);
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
        List<AccountInfoEntity> res = accountInfoDao.select(accountInfoEntity);
        if(res.size()>0){
            //更新
            JSONObject finalData = data;
            res.forEach(o->{
                o.setChannelUserId(finalData.getString("userId"));
                o.setHeaders(proxyHeadersStr);
                o.setUpdateDate(new Date());
                accountInfoDao.insertOrUpdate(o);
            });
            return ServiceResponse.createBySuccess();
        }else {
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
        if(del==null){
            return ServiceResponse.createByErrorMessage("删除数据失败");
        }
        return ServiceResponse.createBySuccess();
    }

    @Override
    public ServiceResponse updateAccount(AccountInfoEntity accountInfoEntity) {
        if(ObjectUtils.isEmpty(accountInfoEntity.getId())){
            accountInfoEntity.setStatus(false);
            accountInfoEntity.setCreateDate(new Date());
        }
        Integer integer = accountInfoDao.insertOrUpdate(accountInfoEntity);
        if(integer==null){
            return ServiceResponse.createByErrorMessage("更新数据失败");
        }
        return ServiceResponse.createBySuccess();
    }
}
