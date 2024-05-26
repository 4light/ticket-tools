package test.ticket.tickettools.web.controller;


import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.service.LoginService;
import test.ticket.tickettools.service.AccountService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/ticket")
public class AccountController {

    @Resource
    AccountService accountServiceImpl;
    @Resource
    LoginService loginService;




    @PostMapping(value = "/user/list")
    public ServiceResponse<PageableResponse<AccountInfoEntity>> getUser(@RequestBody AccountInfoRequest accountInfoRequest) {
        return accountServiceImpl.queryAccount(accountInfoRequest);
    }


    @PostMapping(value = "/user/add")
    public ServiceResponse addUser(@RequestBody AccountInfoRequest accountInfoRequest) {
        return accountServiceImpl.addAccount(accountInfoRequest);
    }
    @PostMapping(value = "/user/update")
    public ServiceResponse updateUser(@RequestBody AccountInfoEntity accountInfoEntity) {
        return accountServiceImpl.updateAccount(accountInfoEntity);
    }
    @GetMapping(value = "/user/del")
    public ServiceResponse delUser(@RequestParam Long id) {
        return accountServiceImpl.delAccount(id);
    }

    @PostMapping(value = "/proxy/user/add")
    public ServiceResponse addProxyUser(@RequestBody ProxyAccountInfoRequest proxyAccountInfoRequest) {
        return accountServiceImpl.addProxyAccount(proxyAccountInfoRequest);
    }

    @GetMapping(value = "/test/jnt")
    public ServiceResponse test() {
        //JntTicketServiceImpl.doSnatchingJnt();
        loginService.longinCSTM("18310327323");
        return ServiceResponse.createBySuccess();
    }

}
