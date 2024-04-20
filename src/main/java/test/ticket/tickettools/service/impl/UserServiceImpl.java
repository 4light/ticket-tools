package test.ticket.tickettools.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import test.ticket.tickettools.dao.UserInfoDao;
import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.QueryUserInfoRequest;
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
    public ServiceResponse<PageableResponse<UserInfoEntity>> queryUser(QueryUserInfoRequest queryUserInfoRequest) {
        List<UserInfoEntity> select = userInfoDao.select(new UserInfoEntity());
        select=select.stream().filter(o->
            StrUtil.equals(o.getUserName(),queryUserInfoRequest.getUserName())
        ).filter(o->StrUtil.equals(o.getPhoneNum(),queryUserInfoRequest.getPhoneNum())).collect(Collectors.toList());
        return ServiceResponse.createBySuccess(PageableResponse.listCovPageInfo(queryUserInfoRequest.getPage(),select));
    }

    @Override
    public ServiceResponse addUser(UserInfoEntity userInfoEntity) {
        return null;
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
