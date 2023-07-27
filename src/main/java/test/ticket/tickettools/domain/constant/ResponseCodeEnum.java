package test.ticket.tickettools.domain.constant;

import java.util.HashMap;
import java.util.Map;

public enum ResponseCodeEnum {
    SUCCESS(0,"成功"),
    ERROR(-1,"失败");

    Integer code;
    String desc;

    ResponseCodeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private static final Map<Integer, ResponseCodeEnum> MAP = new HashMap<>();

    static {
        for (ResponseCodeEnum t : ResponseCodeEnum.values()) {
            MAP.put(t.getCode(), t);
        }
    }

    public static ResponseCodeEnum idOf(Integer code) {
        return MAP.get(code);
    }

    public static Map<Integer, String> toMap() {
        Map<Integer, String> responseMap = new HashMap<>();
        for (ResponseCodeEnum t : ResponseCodeEnum.values()) {
            responseMap.put(t.getCode(), t.getDesc());
        }
        return responseMap;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(code).append(",").append(desc).toString();
    }
}
