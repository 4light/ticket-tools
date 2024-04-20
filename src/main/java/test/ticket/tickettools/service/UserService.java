package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.QueryUserInfoRequest;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.UserInfoEntity;

public interface UserService {
    ServiceResponse<PageableResponse<UserInfoEntity>> queryUser(QueryUserInfoRequest queryUserInfoRequest);
    ServiceResponse addUser(UserInfoEntity userInfoEntity);
    ServiceResponse delUser(Long id);
    ServiceResponse updateUser(UserInfoEntity userInfoEntity);
}
