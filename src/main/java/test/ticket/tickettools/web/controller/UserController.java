package test.ticket.tickettools.web.controller;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.UserInfoEntity;
import test.ticket.tickettools.service.UserService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/ticket")
public class UserController {

    @Resource
    UserService userServiceImpl;



    @PostMapping(value = "/user/list")
    public ServiceResponse<PageableResponse<UserInfoEntity>> getUser(@RequestBody UserInfoRequest userInfoRequest) {
        return userServiceImpl.queryUser(userInfoRequest);
    }


    @PostMapping(value = "/user/add")
    public ServiceResponse addUser(@RequestBody UserInfoRequest userInfoRequest) {
        return userServiceImpl.addUser(userInfoRequest);
    }
}
