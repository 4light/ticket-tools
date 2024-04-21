package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.dao.UserInfoDao;
import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.UserInfoRequest;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.UserInfoEntity;
import test.ticket.tickettools.service.UserService;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    UserInfoDao userInfoDao;


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
        Integer insert = userInfoDao.insert(userInfo);
        if(insert==null){
            return ServiceResponse.createByErrorMessage("插入数据失败");
        }
        return ServiceResponse.createBySuccess();
    }

    @Override
    public ServiceResponse delUser(Long id) {
        return null;
    }

    @Override
    public ServiceResponse updateUser(UserInfoEntity userInfoEntity) {
        return null;
    }
}
