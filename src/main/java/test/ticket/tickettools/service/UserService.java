package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.QueryUserInfoParam;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.UserEntity;

import java.util.List;

public interface UserService {
    UserEntity selectByUserName(String userName);
    ServiceResponse<Boolean> insert(UserEntity userEntity);
    ServiceResponse<Boolean> update(UserEntity userEntity);
    ServiceResponse<PageableResponse<UserEntity>> select(QueryUserInfoParam queryUserInfoParam);
    ServiceResponse<List<QueryUserInfoParam>> selectAll(QueryUserInfoParam queryUserInfoParam);
}
