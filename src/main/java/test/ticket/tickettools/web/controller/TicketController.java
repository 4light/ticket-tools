package test.ticket.tickettools.web.controller;

import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.domain.bo.QueryTaskInfo;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.domain.entity.TaskEntity;
import test.ticket.tickettools.service.TicketService;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Resource
    TicketService ticketServiceImpl;

    @PostMapping(value = "/task/list")
    public ServiceResponse<List<TaskEntity>> getTaskList(@RequestBody QueryTaskInfo queryTaskInfo) {
        return ticketServiceImpl.queryTask(queryTaskInfo);
    }

    @PostMapping(value = "/add/task")
    public ServiceResponse addTask(@RequestBody TaskEntity taskEntity) {
        return ticketServiceImpl.addTask(taskEntity);
    }

    @PostMapping(value = "/add/task/detail")
    public ServiceResponse addTaskDetail(@RequestBody TaskDetailEntity taskDetailEntity) {
        return ticketServiceImpl.addTaskDetail(taskDetailEntity);
    }

    @GetMapping(value = "/get/task/detail")
    public ServiceResponse getTaskDetail(@RequestParam Long taskId) {
        return ticketServiceImpl.getTask(taskId);
    }

    @GetMapping(value = "/phone/captcha")
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
