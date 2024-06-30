package test.ticket.tickettools.dao;

import org.springframework.stereotype.Repository;
import test.ticket.tickettools.domain.entity.AccountInfoEntity;

import java.util.List;

@Repository
public interface AccountInfoDao {
    Integer insertOrUpdate(AccountInfoEntity accountInfoEntity);
    Integer insert(AccountInfoEntity accountInfoEntity);
    List<AccountInfoEntity> selectList(AccountInfoEntity accountInfoEntity);
    AccountInfoEntity selectById(Long id);
    Integer del(Long id);
    Integer updateByChannelAccount(AccountInfoEntity accountInfoEntity);
}
