package test.ticket.tickettools;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
@MapperScan("test.ticket.tickettools.dao")
public class TicketToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketToolsApplication.class, args);
    }

}
