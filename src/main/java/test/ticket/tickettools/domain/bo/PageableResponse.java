package test.ticket.tickettools.domain.bo;

//import com.github.pagehelper.Page;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageableResponse<T> {

    private long total; //总条数
    private Integer totalPage; //总页数
    private int pageNo;
    private int pageSize;
    private List<T> list;


    public static <T> PageableResponse<T> listCovPageInfo(PageParam page, List<T> list) {

        int total = list.size();
        int maxPageTotal = Math.max(total / page.getPageSize() + (total % page.getPageSize() > 0 ? 1 : 0), 1);
        int pageNo = page.getPageNum() > maxPageTotal ? maxPageTotal : page.getPageNum();
        int startIndex = (pageNo - 1) * page.getPageSize();
        int endIndex = Math.min(startIndex + page.getPageSize(), total);

        return PageableResponse.<T>builder()
                .totalPage(maxPageTotal)
                .total(total)
                .pageNo(pageNo)
                .pageSize(page.getPageSize())
                .list(list.subList(startIndex, endIndex))
                .build();
    }
}
