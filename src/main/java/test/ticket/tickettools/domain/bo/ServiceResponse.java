package test.ticket.tickettools.domain.bo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import test.ticket.tickettools.domain.constant.ResponseCodeEnum;

import java.io.Serializable;

@JsonSerialize
public class ServiceResponse<T> implements Serializable {
    private Integer status;
    private String msg;
    private T data;

    private ServiceResponse(Integer status) {
        this.status = status;
    }

    private ServiceResponse(Integer status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    private ServiceResponse(Integer status, T data) {
        this.status = status;
        this.data = data;
    }

    private ServiceResponse(Integer status, String msg, T data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.status == ResponseCodeEnum.SUCCESS.getCode();
    }

    public Integer getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public static <T> ServiceResponse<T> createBySuccess() {
        return new ServiceResponse<T>(ResponseCodeEnum.SUCCESS.getCode(),ResponseCodeEnum.SUCCESS.getDesc());
    }

    public static <T> ServiceResponse<T> createBySuccessMessgge(String msg) {
        return new ServiceResponse<T>(ResponseCodeEnum.SUCCESS.getCode(), msg);
    }

    public static <T> ServiceResponse<T> createBySuccess(T data) {
        return new ServiceResponse<T>(ResponseCodeEnum.SUCCESS.getCode(),ResponseCodeEnum.SUCCESS.getDesc(), data);
    }

    public static <T> ServiceResponse<T> createBySuccess(T data, String msg) {
        return new ServiceResponse<T>(ResponseCodeEnum.SUCCESS.getCode(), msg, data);
    }

    public static <T> ServiceResponse<T> createByError() {
        return new ServiceResponse<T>(ResponseCodeEnum.ERROR.getCode(), ResponseCodeEnum.ERROR.getDesc());
    }

    public static <T> ServiceResponse<T> createByErrorMessage(String msg) {
        return new ServiceResponse<T>(ResponseCodeEnum.ERROR.getCode(), msg);
    }

    public static <T> ServiceResponse<T> createByErrorCodeMessage(Integer errorCode, String msg) {
        return new ServiceResponse<T>(errorCode, msg);
    }
}

