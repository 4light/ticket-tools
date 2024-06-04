package test.ticket.tickettools.web.controller;


import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.QueryUserInfoParam;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.UserEntity;
import test.ticket.tickettools.service.UserService;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/ticket")
public class UserController extends BaseController{

    @Resource
    UserService userServiceImpl;

    @PostMapping(value = "/user/list")
    public ServiceResponse<PageableResponse<UserEntity>> getUser(@RequestBody QueryUserInfoParam queryUserInfoParam) {
        return userServiceImpl.select(queryUserInfoParam);
    }
    @PostMapping(value = "/user/list/all")
    public ServiceResponse<List<QueryUserInfoParam>> getAllUser(@RequestBody QueryUserInfoParam queryUserInfoParam) {
        return userServiceImpl.selectAll(queryUserInfoParam);
    }
    @PostMapping(value = "/user/add")
    public ServiceResponse<Boolean> addUser(@RequestBody UserEntity userEntity) {
        return userServiceImpl.insert(userEntity);
    }
    @PostMapping(value = "/user/update")
    public ServiceResponse<Boolean> updateUser(@RequestBody UserEntity userEntity) {
        return userServiceImpl.update(userEntity);
    }
}
