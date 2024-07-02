package test.ticket.tickettools.web.controller;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.*;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.service.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/ticket")
public class TicketController  extends BaseController{

    @Resource
    TicketService ticketServiceImpl;
    @Resource
    DoSnatchTicketService palaceMuseumTicketServiceImpl;
    @Resource
    SyncDataService syncDataService;

    @PostMapping(value = "/user")
    public ServiceResponse<PageableResponse<TaskInfoListResponse>> getUser(@RequestBody QueryTaskInfo queryTaskInfo) {

        return ticketServiceImpl.getCurrentUser(queryTaskInfo);
    }

    @PostMapping(value = "/task/list")
    public ServiceResponse<PageableResponse<TaskInfoListResponse>> getTaskList(@RequestBody QueryTaskInfo queryTaskInfo) {
        queryTaskInfo.setCreator(currentUser);
        return ticketServiceImpl.queryTask(queryTaskInfo);
    }

    @PostMapping(value = "/task/list2")
    public ServiceResponse<PageableResponse<TaskInfoListResponse>> getTaskList2(@RequestBody QueryTaskInfo queryTaskInfo) {
        return ticketServiceImpl.queryTask(queryTaskInfo);
    }


    @PostMapping(value = "/add/taskInfo")
    public ServiceResponse addTask(@RequestBody TaskInfo taskInfo) {
        taskInfo.setCreator(currentUser);
        return ticketServiceImpl.addTaskInfo(taskInfo);
    }

    @PostMapping(value = "/init/task")
    public ServiceResponse initTask(@RequestBody InitTaskParam initTaskParam) {
        return ticketServiceImpl.initTask(initTaskParam);
    }

    @GetMapping(value = "/get/detail")
    public ServiceResponse getTask(@RequestParam Long taskId,@RequestParam Boolean yn) {
        return ticketServiceImpl.getTask(taskId,yn);
    }

    @GetMapping(value = "/delete")
    public ServiceResponse delete(@RequestParam Long taskId,@RequestParam Boolean yn) {
        return ticketServiceImpl.delete(taskId,yn);
    }

    @GetMapping(value = "/operator/detail")
    public ServiceResponse operatorDetail(@RequestParam Long id,@RequestParam Boolean yn) {
        TaskDetailEntity taskDetailEntity=new TaskDetailEntity();
        taskDetailEntity.setId(id);
        taskDetailEntity.setYn(yn);
        Boolean res = ticketServiceImpl.updateTaskDetail(taskDetailEntity);
        if(res){
            syncDataService.syncNormalData();
            syncDataService.syncTickingDayData();
            return ServiceResponse.createBySuccessMessgge("更新成功");
        }
        return ServiceResponse.createByErrorMessage("更新失败");
    }


    @GetMapping(value = "/phone/captcha")
    public ServiceResponse savePhoneCaptcha(HttpServletRequest request) {
        AccountInfoEntity param=new AccountInfoEntity();
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
                        param.setAccount(phone.startsWith("+86")?phone.substring(3):phone);
                    }
                }
                if(entity.contains("content")){
                    String[] contentArr = entity.split("=");
                    if(contentArr.length>=2){
                        param.setExt(contentArr[1]);
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
        param.setUpdateDate(new Date());
        return ticketServiceImpl.addPhoneInfo(param);
    }

    @GetMapping(value = "/phone/msg")
    public ServiceResponse getPhoneMsg(@RequestParam String phoneNum){
        return ticketServiceImpl.getPhoneMsg(phoneNum);
    }

    @PostMapping(value = "/pay")
    public ServiceResponse pay(@RequestBody PlaceOrderInfo placeOrderInfo) {
        return ticketServiceImpl.pay(placeOrderInfo);
    }

    @GetMapping(value = "/sync")
    public void sync(@RequestParam String tag){
        if(ObjectUtil.equals(tag,"ticketingDay")){
            syncDataService.syncTickingDayData();
        }
        if(ObjectUtil.equals(tag,"normal")){
            syncDataService.syncNormalData();
        }
    }
}
