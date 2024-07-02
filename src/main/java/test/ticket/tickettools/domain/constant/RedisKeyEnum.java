package test.ticket.tickettools.domain.constant;

import java.util.HashMap;
import java.util.Map;

public enum RedisKeyEnum {
    TICKETINGDAY("TICKETINGDAY","放票日数据"),
    NORMAL("NORMAL","日常任务数据"),
    TASK("TASK","任务"),
    USEDATE("USEDATE","余票日期"),
    TASKDETAIL("TASKDETAIL","任务详情");

    String code;
    String desc;

    RedisKeyEnum(String code, String desc) {
        this.code = code;
        this.desc=desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private static final Map<String, RedisKeyEnum> MAP = new HashMap<>();

    static {
        for (RedisKeyEnum t : RedisKeyEnum.values()) {
            MAP.put(t.getCode(), t);
        }
    }

    public static RedisKeyEnum idOf(Integer code) {
        return MAP.get(code);
    }

    public static Map<String, String> toMap() {
        Map<String, String> responseMap = new HashMap<>();
        for (RedisKeyEnum t : RedisKeyEnum.values()) {
            responseMap.put(t.getCode(), t.getDesc());
        }
        return responseMap;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(code).append(",").append(desc).toString();
    }
}
