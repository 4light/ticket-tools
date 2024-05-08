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
import test.ticket.tickettools.dao.UserInfoDao;
import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.ProxyUserInfoRequest;
import test.ticket.tickettools.domain.bo.UserInfoRequest;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.constant.ChannelEnum;
import test.ticket.tickettools.domain.entity.UserInfoEntity;
import test.ticket.tickettools.service.UserService;
import test.ticket.tickettools.utils.TemplateUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    UserInfoDao userInfoDao;
    private String getChnMuUserInfoUrl="https://lotswap.dpm.org.cn/lotsapi/leaguer/api/userLeaguer/manage/leaguerInfo?cipherText=0&merchantId=2655&merchantInfoId=2655";
    private static List<String> accessTokenList=new ArrayList<>();
    @Override
    public ServiceResponse queryUser(UserInfoRequest userInfoRequest) {
        UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setUserName(userInfoRequest.getUserName());
        userInfoEntity.setAccount(userInfoRequest.getAccount());
        List<UserInfoEntity> select = userInfoDao.select(userInfoEntity);
        if(ObjectUtils.isEmpty(userInfoRequest.getPage())){
            return ServiceResponse.createBySuccess(select);
        }
        return ServiceResponse.createBySuccess(PageableResponse.listCovPageInfo(userInfoRequest.getPage(),select));
    }

    @Override
    public ServiceResponse addUser(UserInfoRequest userInfoRequest) {
        UserInfoEntity userInfo=new UserInfoEntity();
        userInfo.setChannel(userInfoRequest.getChannel());
        userInfo.setUserName(userInfoRequest.getUserName());
        userInfo.setAccount(userInfoRequest.getAccount());
        userInfo.setPwd(userInfoRequest.getPwd());
        Integer insert = userInfoDao.insert(userInfo);
        if(insert==null){
            return ServiceResponse.createByErrorMessage("插入数据失败");
        }
        return ServiceResponse.createBySuccess();
    }

    @Override
    public ServiceResponse addProxyUser(ProxyUserInfoRequest proxyUserInfoRequest) {
        //故宫获取用户信息
        HttpHeaders headers=new HttpHeaders();
        String proxyHeadersStr = proxyUserInfoRequest.getHeaders();
        JSONObject proxyHeadersJson = JSON.parseObject(proxyHeadersStr);
        if(accessTokenList.contains(proxyHeadersJson.getString("access-token"))){
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
        JSONObject response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), getChnMuUserInfoUrl, HttpMethod.GET, entity);
        if(response==null||response.getIntValue("status")!=200){
            log.info("请求用户信息异常：{}",response);
            return ServiceResponse.createByErrorMessage("获取用户信息异常");
        }
        log.info("获取到的用户信息:{}",response);
        JSONObject data = response.getJSONObject("data");
        UserInfoEntity userInfoEntity=new UserInfoEntity();
        userInfoEntity.setChannel(ChannelEnum.LOTS.getCode());
        userInfoEntity.setChannelUserId(data.getString("userId"));
        String account = data.getString("mobile") == null ? data.getString("email") : data.getString("mobile");
        userInfoEntity.setAccount(account);
        List<UserInfoEntity> res = userInfoDao.select(userInfoEntity);
        if(res.size()>0){
            //更新
            res.forEach(o->{
                o.setHeaders(proxyHeadersStr);
                o.setUpdateDate(new Date());
                userInfoDao.insertOrUpdate(o);
            });
            return ServiceResponse.createBySuccess();
        }else {
            userInfoEntity.setHeaders(proxyHeadersStr);
            //先用来存储openId
            userInfoEntity.setExt(data.getString("openId"));
            Integer insert = userInfoDao.insert(userInfoEntity);
            if (insert != 0) {
                return ServiceResponse.createBySuccess();
            }
            return ServiceResponse.createByErrorMessage("保存失败");
        }
    }

    @Override
    public ServiceResponse delUser(Long id) {
        Integer del = userInfoDao.del(id);
        if(del==null){
            return ServiceResponse.createByErrorMessage("删除数据失败");
        }
        return ServiceResponse.createBySuccess();
    }

    @Override
    public ServiceResponse updateUser(UserInfoEntity userInfoEntity) {
        userInfoEntity.setUpdateDate(new Date());
        Integer integer = userInfoDao.insertOrUpdate(userInfoEntity);
        if(integer==null){
            return ServiceResponse.createByErrorMessage("更新数据失败");
        }
        return ServiceResponse.createBySuccess();
    }
}
