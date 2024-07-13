package test.ticket.tickettools.web.controller;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.commons.io.IOUtils;
import test.ticket.tickettools.dao.TaskDetailDao;
import test.ticket.tickettools.domain.bo.*;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;
import test.ticket.tickettools.domain.entity.TaskDetailEntity;
import test.ticket.tickettools.service.*;
import test.ticket.tickettools.utils.ProxyUtil;
import test.ticket.tickettools.utils.ScreenshotUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/ticket")
public class TicketController  extends BaseController{

    @Resource
    TicketService ticketServiceImpl;
    @Resource
    TaskDetailDao taskDetailDao;
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

    @GetMapping(value = "/get/path")
    public ServiceResponse getScreenShortPath(@RequestParam Long taskId){
        File folder=new File("/root/screenShort/");
        File[] files = folder.listFiles();
        List<JSONObject> path=new ArrayList<>();
        for (File file : files) {
            JSONObject item=new JSONObject();
            if(file.getName().startsWith("task"+taskId)){
                item.set("name",file.getName());
                path.add(item);
            }
        }
        if(!ObjectUtil.isEmpty(path)){
            return ServiceResponse.createBySuccess(path);
        }
        return ServiceResponse.createByErrorMessage("当前任务还没有订单截图");
    }
    @GetMapping("/scs/download/{filename:.+}")
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable String filename){
        if(filename.startsWith(".")){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        try {
            Path file = Paths.get("/root/screenShort/"+filename);;
            org.springframework.core.io.Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(file))  // 设置内容类型
                        .contentLength(Files.size(file))  // 设置内容长度
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @GetMapping(value = "/test1")
    public ServiceResponse test1(){
        return ServiceResponse.createBySuccess(ProxyUtil.getXieQuProxy(1));
    }

    @GetMapping(value = "/test2")
    public ServiceResponse test2(@RequestParam String orderId,@RequestParam String auth,@RequestParam String name){
        ScreenshotUtil.takeScreenshot(orderId,auth,name);
        return ServiceResponse.createBySuccess();
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
