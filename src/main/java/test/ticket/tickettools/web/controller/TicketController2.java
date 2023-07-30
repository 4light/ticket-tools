package test.ticket.tickettools.web.controller;

import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;
import test.ticket.tickettools.service.TicketService;

import javax.annotation.Resource;
import java.util.Date;

@RestController
public class TicketController2 {

    @Resource
    TicketService ticketServiceImpl;

    @GetMapping(value = "/")
    public ServiceResponse getPhoneCaptcha(@RequestParam String addr,
                                           @RequestParam String content,
                                           @RequestParam Date date) {
        PhoneInfoEntity param=new PhoneInfoEntity();
        param.setPhoneNum(addr);
        param.setContent(content);
        param.setCreateDate(new Date());
        param.setUpdateDate(date);
        return ticketServiceImpl.addPhoneInfo(param);
    }

}
