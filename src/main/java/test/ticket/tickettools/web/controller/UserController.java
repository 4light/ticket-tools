package test.ticket.tickettools.web.controller;


import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.domain.bo.PageableResponse;
import test.ticket.tickettools.domain.bo.QueryUserInfoParam;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.UserEntity;
import test.ticket.tickettools.service.RedisService;
import test.ticket.tickettools.service.UserService;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/ticket")
public class UserController extends BaseController{

    @Resource
    UserService userServiceImpl;
    @Resource
    RedisService redisService;

    @PostMapping(value = "/user/list")
    public ServiceResponse<PageableResponse<UserEntity>> getUser(@RequestBody QueryUserInfoParam queryUserInfoParam) {
        return userServiceImpl.select(queryUserInfoParam);
    }
    @PostMapping(value = "/user/list/all")
    public ServiceResponse<List<QueryUserInfoParam>> getAllUser(@RequestBody QueryUserInfoParam queryUserInfoParam) {
        queryUserInfoParam.setCurrentUser(currentUser);
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
    @PostMapping("/save")
    public void saveList(@RequestParam String key, @RequestBody List<String> list) {
        redisService.saveList(key, list);
    }

    @GetMapping("/get")
    public List<String> getList(@RequestParam String key) {
        return redisService.getList(key);
    }
    @GetMapping("/remove")
    public void getList(@RequestParam String key, @RequestParam String item) {
        redisService.removeFromList(key,item);
    }
}
