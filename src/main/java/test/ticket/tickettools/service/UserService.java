package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.UserInfoRequest;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.UserInfoEntity;

public interface UserService {
    ServiceResponse queryUser(UserInfoRequest userInfoRequest);
    ServiceResponse addUser(UserInfoRequest userInfoRequest);
    ServiceResponse delUser(Long id);
    ServiceResponse updateUser(UserInfoEntity userInfoEntity);
}
