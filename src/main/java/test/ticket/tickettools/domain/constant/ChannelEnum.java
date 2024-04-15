package test.ticket.tickettools.domain.constant;

import java.util.HashMap;
import java.util.Map;

public enum ChannelEnum {
    CSTM(0,"科技馆","https://pcticket.cstm.org.cn"),
    MFU(1,"毛纪","https://jnt.mfu.com.cn"),
    LOTS(2,"故宫","https://lotswap.dpm.org.cn");

    Integer code;
    String desc;
    String baseUrl;

    ChannelEnum(Integer code, String desc,String baseUrl) {
        this.code = code;
        this.desc = desc;
        this.baseUrl=baseUrl;
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private static final Map<Integer, ChannelEnum> MAP = new HashMap<>();

    static {
        for (ChannelEnum t : ChannelEnum.values()) {
            MAP.put(t.getCode(), t);
        }
    }

    public static ChannelEnum idOf(Integer code) {
        return MAP.get(code);
    }

    public static Map<Integer, String> toMap() {
        Map<Integer, String> responseMap = new HashMap<>();
        for (ChannelEnum t : ChannelEnum.values()) {
            responseMap.put(t.getCode(), t.getDesc());
        }
        return responseMap;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(code).append(",").append(desc).toString();
    }
}
