package test.ticket.tickettools.utils;


import java.time.*;
import java.util.*;

public class DateUtils {

    /**
     *
     * @param localDateTime
     * @return
     */
    public static Date localDateToDate(LocalDateTime localDateTime){
        //获取系统默认时区
        ZoneId zoneId = ZoneId.systemDefault();
        //时区的日期和时间
        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);
        return Date.from(zonedDateTime.toInstant());
    }
    public static Date localDateToDate(LocalDate date){
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = date.atStartOfDay().atZone(zone).toInstant();
        return Date.from(instant);
    }
}
