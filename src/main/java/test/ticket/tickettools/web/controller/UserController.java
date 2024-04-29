package test.ticket.tickettools.web.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.UserInfoEntity;
import test.ticket.tickettools.service.JntTicketService;
import test.ticket.tickettools.service.UserService;
import test.ticket.tickettools.utils.TemplateUtil;

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
    @PostMapping(value = "/user/update")
    public ServiceResponse updateUser(@RequestBody UserInfoEntity userInfoEntity) {
        return userServiceImpl.updateUser(userInfoEntity);
    }
    @GetMapping(value = "/user/del")
    public ServiceResponse delUser(@RequestParam Long id) {
        return userServiceImpl.delUser(id);
    }

    @PostMapping(value = "/proxy/user/add")
    public ServiceResponse addProxyUser(@RequestBody ProxyUserInfoRequest proxyUserInfoRequest) {
        return userServiceImpl.addProxyUser(proxyUserInfoRequest);
    }

    @GetMapping(value = "/test/jnt")
    public ServiceResponse test() {
        //JntTicketServiceImpl.doSnatchingJnt();
        return ServiceResponse.createBySuccess();
    }

    @GetMapping(value = "/test/init")
    public ServiceResponse init() {
        //JntTicketServiceImpl.initCookie();
        JSONObject response = TemplateUtil.getResponse(TemplateUtil.initSSLTemplate(), "https://lotswap.dpm.org.cn/lotsapi/merchant/api/fsyy/calendar?parkId=11324&year=2024&month=03&merchantId=2655&merchantInfoId=2655", HttpMethod.GET, new HttpEntity(new HttpHeaders()));
        return ServiceResponse.createBySuccess(response);
    }
}
