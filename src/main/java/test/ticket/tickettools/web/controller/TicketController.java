package test.ticket.tickettools.web.controller;
import cn.hutool.core.date.DateUtil;
import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;
import test.ticket.tickettools.service.TicketService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Resource
    TicketService ticketServiceImpl;

    @PostMapping(value = "/task/list")
    public ServiceResponse<PageableResponse<TaskInfoListResponse>> getTaskList(@RequestBody QueryTaskInfo queryTaskInfo) {
        return ticketServiceImpl.queryTask(queryTaskInfo);
    }

    @PostMapping(value = "/add/taskInfo")
    public ServiceResponse addTask(@RequestBody TaskInfoRequest taskInfoRequest) {
        return ticketServiceImpl.addTaskInfo(taskInfoRequest);
    }

    @PostMapping(value = "/update/taskInfo")
    public ServiceResponse updateTask(@RequestBody UpdateTaskDetailRequest updateTaskDetailRequest) {
        return ticketServiceImpl.updateTaskDetail(updateTaskDetailRequest);
    }

    @GetMapping(value = "/phone/captcha")
    public ServiceResponse getPhoneCaptcha(HttpServletRequest request) {
        PhoneInfoEntity param=new PhoneInfoEntity();
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
            String reqStr = stringEntry.getKey();

        }
        /*param.setPhoneNum(addr);
        param.setContent(content);
        param.setCreateDate(new Date());
        param.setUpdateDate(DateUtil.parse(date,"yyyy-MM-dd HH:mm:ss"));*/
        return ticketServiceImpl.addPhoneInfo(param);
    }

    @GetMapping(value = "/test")
    public ServiceResponse test() {
        return ServiceResponse.createBySuccess(ticketServiceImpl.getTaskForRun());
    }

}
