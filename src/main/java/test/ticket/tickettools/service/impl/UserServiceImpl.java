package test.ticket.tickettools.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import test.ticket.tickettools.dao.UserDao;
import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.QueryUserInfoParam;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.UserEntity;
import test.ticket.tickettools.service.UserService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Resource
    UserDao userDao;

    @Override
    public UserEntity selectByUserName(String userName) {
        return userDao.findByUsername(userName);
    }

    @Override
    public ServiceResponse<Boolean> insert(UserEntity userEntity) {
        userEntity.setPwd(passwordEncoder.encode(userEntity.getPwd()));
        userEntity.setCreateDate(new Date());
        userEntity.setYn(false);
        userEntity.setStatus(false);
        Integer insert = userDao.insert(userEntity);
        if(insert!=null&&insert>0){
            return ServiceResponse.createBySuccess(true);
        }
        return ServiceResponse.createByErrorMessage("报错失败");
    }

    @Override
    public ServiceResponse<Boolean> update(UserEntity userEntity) {
        UserEntity byUsername = userDao.findByUsername(userEntity.getUserName());
        if(!ObjectUtils.isEmpty(byUsername)&&!ObjectUtils.nullSafeEquals(byUsername.getPwd(),userEntity.getPwd())){
            userEntity.setPwd(passwordEncoder.encode(userEntity.getPwd()));
        }
        Integer update = userDao.updateById(userEntity);
        if(update!=null&&update>0){
            return ServiceResponse.createBySuccess(true);
        }
        return ServiceResponse.createByErrorMessage("更新失败");
    }

    @Override
    public ServiceResponse<PageableResponse<UserEntity>> select(QueryUserInfoParam queryUserInfoParam) {
        UserEntity userEntity=new UserEntity();
        userEntity.setUserName(queryUserInfoParam.getUserName());
        userEntity.setNickName(queryUserInfoParam.getNickName());
        List<UserEntity> select = userDao.select(userEntity);
        if(ObjectUtils.isEmpty(select)){
            return ServiceResponse.createBySuccess();
        }
        return ServiceResponse.createBySuccess(PageableResponse.listCovPageInfo(queryUserInfoParam.getPage(), select));
    }

    @Override
    public ServiceResponse<List<QueryUserInfoParam>> selectAll(QueryUserInfoParam queryUserInfoParam) {
        UserEntity userEntity=new UserEntity();
        userEntity.setUserName(queryUserInfoParam.getUserName());
        userEntity.setNickName(queryUserInfoParam.getNickName());
        List<UserEntity> select = userDao.select(userEntity);
        List<QueryUserInfoParam> result=new ArrayList<>();
        for (UserEntity entity : select) {
            QueryUserInfoParam  param=new QueryUserInfoParam();
            param.setId(entity.getId());
            param.setUserName(entity.getUserName());
            param.setNickName(entity.getNickName());
            result.add(param);
        }
        return ServiceResponse.createBySuccess(result);
    }
}
