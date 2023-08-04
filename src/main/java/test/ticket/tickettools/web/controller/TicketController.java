package test.ticket.tickettools.web.controller;
import cn.hutool.core.date.DateUtil;
import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.dao.TaskDao;
import test.ticket.tickettools.dao.TaskDetailDao;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.PhoneInfoEntity;
import test.ticket.tickettools.service.LoginService;
import test.ticket.tickettools.service.TicketService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Resource
    TicketService ticketServiceImpl;
    @Resource
    TaskDetailDao taskDetailDao;

    @Resource
    LoginService loginService;

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
    public ServiceResponse savePhoneCaptcha(HttpServletRequest request) {
        PhoneInfoEntity param=new PhoneInfoEntity();
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
            //from=+8618310327323&addr=10684336557485380022&content=【科大讯飞】尊敬的用户，您正在进行手机号码登录操作。验证码为：848200。如非本人操作，还请忽略。五分钟内有效&date=2023-08-01 14:19:16 -> {String[1]@12216} [""]
            String reqStr = stringEntry.getKey();
            String[] split = reqStr.split("&");
            for (String entity : split) {
                if(entity.contains("from")){
                    String[] fromArr = entity.split("=");
                    if(fromArr.length>=2){
                        String phone=fromArr[1];
                        param.setPhoneNum(phone.startsWith("+86")?phone.substring(3):phone);
                    }
                }
                if(entity.contains("content")){
                    String[] contentArr = entity.split("=");
                    if(contentArr.length>=2){
                        param.setContent(contentArr[1]);
                    }
                }
                if(entity.contains("date")){
                    String[] dateArr = entity.split("=");
                    if(dateArr.length>=2){
                        param.setUpdateDate(DateUtil.parse(dateArr[1],"yyyy-MM-dd HH:mm:ss"));
                    }
                }
            }
        }
        param.setCreateDate(new Date());
        return ticketServiceImpl.addPhoneInfo(param);
    }

    @GetMapping(value = "/phone/msg")
    public ServiceResponse getPhoneMsg(@RequestParam String phoneNum){
        return ticketServiceImpl.getPhoneMsg(phoneNum);
    }

    @GetMapping(value = "/test")
    public ServiceResponse test() {
        loginService.longinCSTM("17610773273");
        return ServiceResponse.createBySuccess();
    }

}
