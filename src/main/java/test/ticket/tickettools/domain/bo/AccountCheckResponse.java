package test.ticket.tickettools.domain.bo;

import java.io.Serializable;

public class AccountCheckResponse implements Serializable {
    private static final long serialVersionUID = -9154657910246993634L;
    private Long accountId;
    private Boolean checkRes;
    private String msg;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Boolean getCheckRes() {
        return checkRes;
    }

    public void setCheckRes(Boolean checkRes) {
        this.checkRes = checkRes;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
