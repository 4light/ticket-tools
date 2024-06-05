package test.ticket.tickettools.web.controller;


import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.service.LoginService;
import test.ticket.tickettools.service.AccountService;

import javax.annotation.Resource;
import java.util.Calendar;

@RestController
@RequestMapping("/ticket")
public class AccountController extends BaseController{

    @Resource
    AccountService accountServiceImpl;
    @Resource
    LoginService loginService;




    @PostMapping(value = "/account/list")
    public ServiceResponse<PageableResponse<AccountInfoEntity>> getUser(@RequestBody AccountInfoRequest accountInfoRequest) {
        accountInfoRequest.setCreator(currentUser);
        return accountServiceImpl.queryAccount(accountInfoRequest);
    }


    @PostMapping(value = "/account/add")
    public ServiceResponse addUser(@RequestBody AccountInfoRequest accountInfoRequest) {
        accountInfoRequest.setCreator(currentUser);
        return accountServiceImpl.addAccount(accountInfoRequest);
    }
    @PostMapping(value = "/account/update")
    public ServiceResponse updateUser(@RequestBody AccountInfoEntity accountInfoEntity) {
        accountInfoEntity.setCreator(currentUser);
        return accountServiceImpl.updateAccount(accountInfoEntity);
    }
    @GetMapping(value = "/account/del")
    public ServiceResponse delUser(@RequestParam Long id) {
        return accountServiceImpl.delAccount(id);
    }

    @PostMapping(value = "/proxy/user/add")
    public ServiceResponse addProxyUser(@RequestBody ProxyAccountInfoRequest proxyAccountInfoRequest) {
        return accountServiceImpl.addProxyAccount(proxyAccountInfoRequest);
    }

    @GetMapping(value = "/account/getCaptcha")
    public ServiceResponse accountCaptcha() {
        return loginService.getCaptchaImage();
    }

    @PostMapping(value = "/account/getMsgCode")
    public ServiceResponse accountgetMsgCode(@RequestBody LogInCSTMParam logInCSTMParam) {
        return loginService.sendMessageCode(logInCSTMParam);
    }


    @PostMapping(value = "/account/login")
    public ServiceResponse accountLogin(@RequestBody LogInCSTMParam logInCSTMParam) {
        return loginService.login(logInCSTMParam);
    }

    @GetMapping(value = "/test/jnt")
    public ServiceResponse test() {
        //JntTicketServiceImpl.doSnatchingJnt();
        loginService.longinCSTM("18310327323");
        return ServiceResponse.createBySuccess();
    }

}
