package test.ticket.tickettools.service;

import test.ticket.tickettools.domain.bo.AccountCheckResponse;
import test.ticket.tickettools.domain.bo.ProxyAccountInfoRequest;
import test.ticket.tickettools.domain.bo.AccountInfoRequest;
import test.ticket.tickettools.domain.bo.ServiceResponse;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;

import java.util.List;

public interface AccountService {
    ServiceResponse queryAccount(AccountInfoRequest accountInfoRequest);
    ServiceResponse addAccount(AccountInfoRequest accountInfoRequest);
    ServiceResponse addProxyAccount(ProxyAccountInfoRequest proxyAccountInfoRequest);
    ServiceResponse delAccount(Long id);
    ServiceResponse updateAccount(AccountInfoEntity accountInfoEntity);
    ServiceResponse<List<AccountCheckResponse>> accountCheckAuth(String currentUser);
}
