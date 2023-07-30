package test.ticket.tickettools.domain.bo;

import lombok.Data;

@Data
public class PageParam {
    private int pageNum = 1;
    private int pageSize = 30;
}
